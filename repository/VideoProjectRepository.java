package com.minitoon.repository;

import com.minitoon.model.ProjectStatus;
import com.minitoon.model.VideoProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Video Project Repository
 */
@Repository
public interface VideoProjectRepository extends JpaRepository<VideoProject, String> {

    List<VideoProject> findByStatus(ProjectStatus status);

    List<VideoProject> findByStatusAndCreatedAtAfter(ProjectStatus status, LocalDateTime date);

    List<VideoProject> findByStatusAndRetryCountLessThan(ProjectStatus status, int retryCount);

    @Query("SELECT p FROM VideoProject p WHERE p.status = 'UPLOADED' ORDER BY p.actualUploadTime DESC")
    List<VideoProject> findRecentUploads();

    @Query("SELECT COUNT(p) FROM VideoProject p WHERE p.status = 'UPLOADED' AND p.createdAt >= :startDate")
    long countUploadsSince(LocalDateTime startDate);
}
