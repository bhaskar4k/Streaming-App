package com.app.upload.service;

import com.app.authentication.common.DbWorker;
import com.app.upload.common.Util;
import com.app.upload.common.VideoUtil;
import com.app.upload.entity.TEncodedVideoInfo;
import com.app.upload.entity.TLogExceptions;
import com.app.upload.entity.TVideoInfo;
import com.app.upload.entity.TVideoMetadata;
import com.app.upload.environment.Environment;
import com.app.upload.model.JwtUserDetails;
import com.app.upload.model.UIEnum;
import com.app.upload.model.Video;
import com.app.upload.rabbitmq.RabbitQueuePublish;
import com.app.upload.repository.TEncodedVideoInfoRepository;
import com.app.upload.repository.TVideoInfoRepository;
import com.app.upload.repository.TVideoMetadataRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.jvnet.hk2.annotations.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.File;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

@Service
@Component
public class UploadService {
    private Environment environment;
    private Util util;
    @Autowired
    private LogExceptionsService logExceptionsService;
    @Autowired
    private TVideoInfoRepository tVideoInfoRepository;
    @Autowired
    private TEncodedVideoInfoRepository tEncodedVideoInfoRepository;
    @Autowired
    private TVideoMetadataRepository tVideoMetadataRepository;
    @Autowired
    private VideoUtil videoUtil;
    private RabbitQueuePublish rabbitQueuePublish;
    private DbWorker dbWorker;

    @PersistenceContext
    private EntityManager entityManager;

    private String sql_string;
    List<Object> params;

    public UploadService(){
        this.environment = new Environment();
        this.util = new Util();
        this.rabbitQueuePublish = new RabbitQueuePublish();
        this.dbWorker=new DbWorker();
    }


    public TVideoInfo saveVideo(MultipartFile file, JwtUserDetails userDetails) {
        if (file.isEmpty()) {
            return null;
        }

        try {
            String VIDEO_GUID = util.getrandomGUID();
            String ORIGINAL_FILE_DIR = environment.getOriginalVideoPath() + util.getUserSpecifiedFolder(userDetails.getT_mst_user_id(),VIDEO_GUID);
            Files.createDirectories(Paths.get(ORIGINAL_FILE_DIR));

            long fileSize = file.getSize();
            String fileExtension = util.getFileExtension(file.getOriginalFilename());

            String originalFilenameWithoutExtension = util.getFileNameWithoutExtension(file.getOriginalFilename());
            String encodedFileName = VIDEO_GUID + "." + fileExtension;

            Path originalFilePath = Paths.get(ORIGINAL_FILE_DIR, encodedFileName);
            Files.write(originalFilePath, file.getBytes());

            String sourceResolution = videoUtil.getVideoResolution(originalFilePath.toString(), userDetails);
            Double duration = videoUtil.getVideoDuration(originalFilePath.toString(),userDetails);
            Long no_of_chunks = (long)Math.ceil(duration / 5L);

            List<String> validResolutions = videoUtil.getValidResolutions(sourceResolution, environment.getResolutions(), userDetails.getT_mst_user_id());

            TVideoInfo tVideoInfo = new TVideoInfo(VIDEO_GUID, originalFilenameWithoutExtension, fileSize, fileExtension, sourceResolution, duration, no_of_chunks, userDetails.getT_mst_user_id());
            TEncodedVideoInfo tEncodedVideoInfo = new TEncodedVideoInfo(String.join(",", validResolutions),
                                                                        UIEnum.ProcessingStatus.TO_BE_PROCESSED.getValue());

            if(saveVideoDetails(tVideoInfo,tEncodedVideoInfo)){
                Video video = new Video(VIDEO_GUID,originalFilePath.toString(),encodedFileName,userDetails.getT_mst_user_id());
                rabbitQueuePublish.publishIntoRabbitMQ(video).getData();
                return tVideoInfo;
            }else{
                deleteVideoDetails(tVideoInfo,tEncodedVideoInfo);
                return null;
            }
        } catch (Exception e) {
            log(userDetails.getT_mst_user_id(),"saveVideo()",e.getMessage());
            return null;
        }
    }

    @Transactional
    public boolean saveVideoDetails(TVideoInfo video, TEncodedVideoInfo encodedVideoInfo) {
        try {
            tVideoInfoRepository.save(video);
            encodedVideoInfo.setT_video_info_id(video.getId());
            tEncodedVideoInfoRepository.save(encodedVideoInfo);
            return true;
        } catch (Exception e) {
            log(video.getT_mst_user_id(),"saveVideoDetails()",e.getMessage());
            return false;
        }
    }

    public void deleteVideoDetails(TVideoInfo video, TEncodedVideoInfo encodedVideoInfo) {
        try {
            tVideoInfoRepository.deleteById(video.getId());
            tEncodedVideoInfoRepository.deleteById(encodedVideoInfo.getId());
        } catch (Exception e) {
            log(video.getT_mst_user_id(),"deleteVideoDetails()",e.getMessage());
        }
    }

    public boolean saveVideoMetadata(TVideoInfo video_info, String title, String description, int is_public, MultipartFile thumbnail, JwtUserDetails post_validated_request){
        try {
            TVideoMetadata tVideoMetadata = new TVideoMetadata(video_info.getId(), title, description, is_public, UIEnum.YesNo.NO.getValue());

            if (thumbnail != null && !thumbnail.isEmpty()) {
                String THUMBNAIL_FILE_DIR = environment.getOriginalThumbnailPath() + util.getUserSpecifiedFolderForThumbnail(post_validated_request.getT_mst_user_id());

                File thumbnailDir = new File(THUMBNAIL_FILE_DIR);
                if (!thumbnailDir.exists()) thumbnailDir.mkdirs();

                String fileExtension = util.getFileExtension(thumbnail.getOriginalFilename());
                File tempUploadedThumbnailFile = new File(THUMBNAIL_FILE_DIR + File.separator + video_info.getGuid() + "_TEMPCPYFILE." + fileExtension);
                thumbnail.transferTo(tempUploadedThumbnailFile);

                String ConvertedJPGFileOutputPath = THUMBNAIL_FILE_DIR + File.separator + video_info.getGuid() + ".jpg";
                boolean thumbnail_saved = convertImageToJPGFormatAndSave(tempUploadedThumbnailFile.getAbsolutePath(), ConvertedJPGFileOutputPath, post_validated_request);

                if(thumbnail_saved){
                    tempUploadedThumbnailFile.delete();
                    tVideoMetadata = new TVideoMetadata(video_info.getId(), title, description, is_public, UIEnum.YesNo.YES.getValue());
                }
            }

            tVideoMetadataRepository.save(tVideoMetadata);
            return true;
        } catch (Exception e) {
            log(post_validated_request.getT_mst_user_id(),"saveVideoMetadata()",e.getMessage());
            return false;
        }
    }

    public boolean convertImageToJPGFormatAndSave(String inputFilePath, String outputFilePath, JwtUserDetails post_validated_request) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    environment.getFfmpegPath(),
                    "-i", inputFilePath,
                    "-vf", "format=rgb24",
                    "-q:v", "2",
                    "-y",
                    outputFilePath
            );

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 1) {
                log(post_validated_request.getT_mst_user_id(), "convertImageToJPGFormatAndSave()", "FFmpeg failed with exit code " + exitCode + ": " + output.toString());
                return false;
            }

            return true;
        } catch (Exception e) {
            log(post_validated_request.getT_mst_user_id(), "convertImageToJPGFormatAndSave()", e.getMessage());
            return false;
        }
    }


    private void log(Long t_mst_user_id, String function_name, String exception_msg){
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        String full_class_path = stackTraceElements[2].getClassName();
        String class_name = full_class_path.substring(full_class_path.lastIndexOf(".") + 1);

        String full_package_path = full_class_path.substring(0, full_class_path.lastIndexOf("."));
        String package_name = full_package_path.substring(full_package_path.lastIndexOf(".") + 1);

        logExceptionsService.saveLogException(new TLogExceptions(package_name,class_name,function_name,exception_msg,t_mst_user_id));
    }
}