package com.trustid.document.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trustid.document.entity.DocumentFolder;

public interface DocumentFolderRepository extends JpaRepository<DocumentFolder, Long> {
    List<DocumentFolder> findByUserIdOrderByFolderNameAsc(Long userId);
}
