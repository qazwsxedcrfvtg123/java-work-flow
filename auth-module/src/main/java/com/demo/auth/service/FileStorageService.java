package com.demo.auth.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.demo.auth.entity.FileEntity;
import com.demo.auth.enums.PhotosType;
import com.demo.auth.mapper.PhotoMapper;
import com.demo.auth.repository.FileRepository;
import com.demo.auth.enums.FileType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling file storage operations across multiple storage systems
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {
    private final AmazonS3 amazonS3;
    private final FileRepository fileRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PhotoMapper photoMapper;

    @Value("${aws.s3.bucket-name}")
    private String bucketName; //bucket name更改

    @Value("${file.upload.path:./uploads/}")
    private String uploadPath;

    // Cache expiration time (1 hour)
    private static final long CACHE_EXPIRATION_TIME = 60 * 60;

    @PostConstruct
    public void init() {
        // Ensure upload directory exists
        try {
            Files.createDirectories(java.nio.file.Paths.get(uploadPath));
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", uploadPath, e);
        }
    }

    /**
     * Store a file using all available storage methods (database, AWS S3, Redis)
     */
    public Map<String, Object> storeFile(MultipartFile file, String type) throws IOException {
        Map<String, Object> result = new HashMap<>();

        if (file == null || file.isEmpty()) {
            result.put("status", "error");
            result.put("message", "文件为空");
            return result;
        }

        try {
            // Generate unique file name
            String originalFileName = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFileName);
            String uniqueFileName = generateUniqueFileName(originalFileName);

            // Create file entity
            FileEntity fileEntity = FileEntity.builder()
                .fileName(uniqueFileName)
                .originalFileName(originalFileName)
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .uploadTime(LocalDateTime.now())
                .build();

            // Store in different locations
            String filePath = storeInLocalFileSystem(file, uniqueFileName);
            String s3Url = storeInS3(file, uniqueFileName);

            // Set file paths
            fileEntity.setFilePath(filePath);
            fileEntity.setFileUrl(s3Url);
            fileEntity.setStorageType("AWS_S3");
            fileEntity.setType( type);
            // Save to database
            fileEntity = fileRepository.save(fileEntity);

            // Cache file metadata in Redis
            cacheFileMetadata(fileEntity);

            // Prepare response
            result.put("status", "success");
            result.put("message", "文件上传成功");
            result.put("filename", originalFileName);
            result.put("folder", "common");
            result.put("size", file.getSize());
            result.put("fileId", fileEntity.getId());
            result.put("fileUrl", s3Url);

            log.info("File uploaded successfully: {} (ID: {})", originalFileName, fileEntity.getId());

        } catch (Exception e) {
            log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            result.put("status", "error");
            result.put("message", "文件上传失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Store file in local file system as backup
     */
    private String storeInLocalFileSystem(MultipartFile file, String uniqueFileName) throws IOException {
        String filePath = uploadPath + uniqueFileName;
        File dest = new File(filePath);
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            fos.write(file.getBytes());
        }
        return filePath;
    }

    /**
     * Store file in AWS S3
     */
    private String storeInS3(MultipartFile file, String uniqueFileName) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            // Set content type and disable download behavior
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentDisposition("inline"); // This prevents automatic download

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, uniqueFileName, inputStream, metadata);
            amazonS3.putObject(putObjectRequest);

            // Generate URL for the uploaded file
            return amazonS3.getUrl(bucketName, uniqueFileName).toString();
        }
    }

    /**
     * Cache file metadata in Redis using RedisTemplate with object support
     */
    private void cacheFileMetadata(FileEntity fileEntity) {
        try {
            // Cache by ID
            String key = "file_metadata:" + fileEntity.getId();
            redisTemplate.opsForValue().set(key, fileEntity, CACHE_EXPIRATION_TIME, TimeUnit.SECONDS);

            // Also cache by filename
            String filenameKey = "file_metadata_by_name:" + fileEntity.getFileName();
            redisTemplate.opsForValue().set(filenameKey, fileEntity, CACHE_EXPIRATION_TIME, TimeUnit.SECONDS);

            log.debug("Successfully cached file metadata in Redis: ID={}, Keys=[{}, {}]",
                fileEntity.getId(), key, filenameKey);
        } catch (Exception e) {
            log.warn("Failed to cache file metadata in Redis: {}", e.getMessage());
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
    }

    /**
     * Generate unique filename using timestamp and UUID
     */
    private String generateUniqueFileName(String originalFileName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String fileExtension = getFileExtension(originalFileName);
        return timestamp + "_" + uuid + fileExtension;
    }

//    public List<FileEntity> findPhotos(List<String> type) {
//
//        List<FileEntity> selectPhotos = photoMapper.selectPhotosBytypes(type);
//
//        return selectPhotos;
//    }

    /**
     * 前台枚举查询照片
     */
    public List<PhotosType> findPhotos(List<String> types) {
        log.info("查询照片类型: {}", types);
        return photoMapper.selectPhotosBytypes(types);
    }

    /**
     * 后台查询所有类型的照片
     */
    public List<PhotosType> findAllPhotos() {
        return photoMapper.selectPhotosBytypes(PhotosType.getAllCodes());
    }

    /**
     * Delete file from all storage systems (S3, local file system, database, Redis)
     */
    public Map<String, Object> deleteFile(String fileName) {
        Map<String, Object> result = new HashMap<>();

        if (fileName == null || fileName.isEmpty()) {
            result.put("status", "error");
            result.put("message", "文件名为空");
            return result;
        }

        try {
            FileEntity fileEntity = fileRepository.findByFileName(fileName)
                .orElseThrow(() -> new RuntimeException("文件不存在：" + fileName));

            // Delete from S3
            deleteFromS3(fileName);

            // Delete from local file system
            deleteFromLocalFileSystem(fileEntity.getFilePath());

            // Delete from Redis cache
            deleteFromRedis(fileEntity.getId(), fileName);

            // Delete from database
            fileRepository.delete(fileEntity);

            result.put("status", "success");
            result.put("message", "文件删除成功");
            result.put("fileName", fileName);

            log.info("File deleted successfully: {}", fileName);

        } catch (Exception e) {
            log.error("Failed to delete file: {}", fileName, e);
            result.put("status", "error");
            result.put("message", "文件删除失败：" + e.getMessage());
        }

        return result;
    }

    /**
     * Delete file from AWS S3
     */
    private void deleteFromS3(String fileName) {
        try {
            amazonS3.deleteObject(new DeleteObjectRequest(bucketName, fileName));
            log.info("Deleted file from S3: {}", fileName);
        } catch (Exception e) {
            log.warn("Failed to delete file from S3: {}", e.getMessage());
        }
    }

    /**
     * Delete file from local file system
     */
    private void deleteFromLocalFileSystem(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                Files.deleteIfExists(file.toPath());
                log.info("Deleted file from local storage: {}", filePath);
            }
        } catch (Exception e) {
            log.warn("Failed to delete file from local storage: {}", e.getMessage());
        }
    }

    /**
     * Delete file metadata from Redis cache
     */
    private void deleteFromRedis(Long fileId, String fileName) {
        try {
            String key = "file_metadata:" + fileId;
            String filenameKey = "file_metadata_by_name:" + fileName;
            redisTemplate.delete(key);
            redisTemplate.delete(filenameKey);
            log.debug("Deleted file metadata from Redis: {}", key);
        } catch (Exception e) {
            log.warn("Failed to delete file metadata from Redis: {}", e.getMessage());
        }
    }
}
