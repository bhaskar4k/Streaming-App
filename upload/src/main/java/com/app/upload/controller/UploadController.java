package com.app.upload.controller;

import com.app.upload.common.CommonReturn;
import com.app.upload.entity.TLogExceptions;
import com.app.upload.entity.TVideoInfo;
import com.app.upload.environment.Environment;
import com.app.upload.model.JwtUserDetails;
import com.app.upload.service.AuthService;
import com.app.upload.service.LogExceptionsService;
import com.app.upload.service.UploadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.DataInput;


@RestController
@RequestMapping("/upload")
public class UploadController {
    @Autowired
    public AuthService authService;
    @Autowired
    private UploadService uploadService;
    @Autowired
    private LogExceptionsService logExceptionsService;
    public Environment environment;

    public UploadController(){
        this.environment = new Environment();
    }

    @PostMapping("/upload_video")
    public CommonReturn<TVideoInfo> upload(@RequestPart("video") MultipartFile file) {
        JwtUserDetails post_validated_request = authService.getAuthenticatedUserFromContext();

        try {
            TVideoInfo video_info = uploadService.saveVideo(file,post_validated_request);

            if(video_info != null){
                return CommonReturn.success("Video has been uploaded successfully", video_info);
            }

            return CommonReturn.error(400,"Video upload failed.");
        } catch (Exception e) {
            log("upload()",e.getMessage());
            return CommonReturn.error(400,"Internal Server Error.");
        }
    }


    @PostMapping("/upload_video_info")
    public CommonReturn<Boolean> upload_video_info(@RequestParam("title") String title,
                                                   @RequestParam("description") String description,
                                                   @RequestParam("is_public") boolean isPublic,
                                                   @RequestParam("thumbnail") MultipartFile thumbnail,
                                                   @RequestParam("video_info") String video_info_json) {
        JwtUserDetails post_validated_request = authService.getAuthenticatedUserFromContext();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.findAndRegisterModules();
            TVideoInfo video_info = objectMapper.readValue(video_info_json, TVideoInfo.class);

            boolean isVideoMetadataUploadDoneAndSuccessful = uploadService.saveVideoMetadata(video_info, title, description, isPublic, thumbnail, post_validated_request);

            return CommonReturn.success("Video has been uploaded successfully", isVideoMetadataUploadDoneAndSuccessful);
        } catch (Exception e) {
            log("upload()",e.getMessage());
            return CommonReturn.error(400,"Internal Server Error.");
        }
    }


    private void log(String function_name, String exception_msg){
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        String full_class_path = stackTraceElements[2].getClassName();
        String class_name = full_class_path.substring(full_class_path.lastIndexOf(".") + 1);

        String full_package_path = full_class_path.substring(0, full_class_path.lastIndexOf("."));
        String package_name = full_package_path.substring(full_package_path.lastIndexOf(".") + 1);

        logExceptionsService.saveLogException(new TLogExceptions(package_name,class_name,function_name,exception_msg,0L));
    }
}
