package com.demo.auth.controller;

import com.demo.auth.enums.FileType;
import com.demo.auth.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * @description:
 * @Creator: 管理後台
 * @CreateTime: 2026-03-20 18:12
 */
@RestController
@RequestMapping("/api")
public class CommonController {

    private final static Logger logger = LoggerFactory.getLogger(CommonController.class);


    private final FileStorageService fileStorageService;

    private final List<String> types = Arrays.asList("1","2","3","4","5");

    public CommonController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }
    @PostMapping("/uploadFiles")
    public Object uploadFiles(@RequestParam("file") MultipartFile file ,String type) {
        try {
            logger.info("file:{}", file.getOriginalFilename());
            return fileStorageService.storeFile(file , type);
        } catch (Exception e) {
            logger.error("Failed to process file upload", e);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("message", "文件上传失败: " + e.getMessage());
            return result;
        }
    }

    @PostMapping("/photos/deleteFiles")
    public Object deleteFiles(@RequestParam("fileName") String fileName) {
        try {
            logger.info("fileName:{}", fileName);
            return fileStorageService.deleteFile(fileName);
        } catch (Exception e) {
            logger.error("Failed to process file delete", e);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("message", "文件删除失败: " + e.getMessage());
            return result;
        }
    }

    @GetMapping("/photos/all")
    public Object findPhotos(){
        return fileStorageService.findAllPhotos();
    }

//    @GetMapping("/photos/{types}")
//    public Object findPhotos(@PathVariable FileType types){
//        return fileStorageService.findPhotos(types);
//    }

}
