package com.app.upload.service;

import com.app.upload.common.Util;
import com.app.upload.entity.TLogExceptions;
import com.app.upload.entity.TVideoInfo;
import com.app.upload.environment.Environment;
import com.app.upload.model.JwtUserDetails;
import com.app.upload.model.Video;
import com.app.upload.rabbitmq.RabbitQueuePublish;
import com.app.upload.repository.TVideoInfoRepository;
import org.jvnet.hk2.annotations.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Component
public class UploadService {
    private Environment environment;
    private Util util;
    @Autowired
    private LogExceptionsService logExceptionsService;
    @Autowired
    private TVideoInfoRepository tVideoInfoRepository;
    private RabbitQueuePublish rabbitQueuePublish;

    public UploadService(){
        this.environment = new Environment();
        this.util = new Util();
        this.rabbitQueuePublish = new RabbitQueuePublish();
    }


    public Boolean uploadAndProcessVideo(MultipartFile file, String fileId, JwtUserDetails userDetails) {
        if (file.isEmpty()) {
            return false;
        }

        try {
            String VIDEO_GUID = util.getrandomGUID();
            String ORIGINAL_FILE_DIR = environment.getOriginalVideoPath() + util.getUserSpecifiedFolder(userDetails,VIDEO_GUID);
            Files.createDirectories(Paths.get(ORIGINAL_FILE_DIR));

            long fileSize = file.getSize();
            String fileExtension = util.getFileExtension(file);

            String originalFilenameWithoutExtension = util.getFileNameWithoutExtension(file);
            String originalFilename = file.getOriginalFilename();
            originalFilename = originalFilename.replace(" ","");

            Path originalFilePath = Paths.get(ORIGINAL_FILE_DIR, originalFilename);
            Files.write(originalFilePath, file.getBytes());

            String sourceResolution = getVideoResolution(originalFilePath.toString(), userDetails);

            TVideoInfo tVideoInfo = new TVideoInfo(VIDEO_GUID, originalFilenameWithoutExtension, fileSize, fileExtension, sourceResolution, userDetails.getT_mst_user_id());

            if(saveVideoDetails(tVideoInfo)){
                Video video = new Video(VIDEO_GUID,originalFilePath.toString(),originalFilename,userDetails.getT_mst_user_id());
                return rabbitQueuePublish.publishIntoRabbitMQ(video).getData();
            }else{
                // Rollback the original file save

                return false;
            }
        } catch (Exception e) {
            log(userDetails.getT_mst_user_id(),"uploadAndProcessVideo()",e.getMessage());
            return false;
        }
    }

    private String getVideoResolution(String filePath, JwtUserDetails userDetails){
        try {
            String ffprobePath = environment.getFfprobePath();
            ProcessBuilder processBuilder = new ProcessBuilder(ffprobePath, "-v", "error", "-select_streams", "v:0",
                    "-show_entries", "stream=width,height", "-of", "csv=s=x:p=0", filePath);
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String res = reader.readLine();
                int size=res.length();

                if(res.charAt(size-1)=='x'){
                    res = res.substring(0, size-1);
                }

                return res;
            }
        } catch (Exception e) {
            log(userDetails.getT_mst_user_id(),"getVideoResolution()",e.getMessage());
            return null;
        }
    }

    public boolean saveVideoDetails(TVideoInfo video) {
        try {
            tVideoInfoRepository.save(video);
            return true;
        } catch (Exception e) {
            log(video.getT_mst_user_id(),"saveVideoDetails()",e.getMessage());
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