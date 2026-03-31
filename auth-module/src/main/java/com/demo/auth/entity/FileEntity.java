package com.demo.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.persistence.PrePersist;
import java.time.LocalDateTime;

/**
 * Entity for storing file metadata
 */
@Entity
@Table(name = "uploaded_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String fileName;


    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private String fileUrl;

    @Column(nullable = false)
    private String storageType; // LOCAL, AWS_S3, etc.

    @Column(nullable = false)
    private LocalDateTime uploadTime;

    @Column(nullable = false)
    private String type;
    @PrePersist
    protected void onCreate() {
        uploadTime = LocalDateTime.now();
    }
}
