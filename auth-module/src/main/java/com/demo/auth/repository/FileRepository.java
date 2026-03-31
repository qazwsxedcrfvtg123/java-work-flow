package com.demo.auth.repository;

import com.demo.auth.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for file entity
 */
@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {
    Optional<FileEntity> findByFileName(String fileName);
    Optional<FileEntity> findByOriginalFileName(String originalFileName);
}