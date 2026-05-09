package com.minitoon.repository;

import com.minitoon.model.Scene;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Scene Repository
 */
@Repository
public interface SceneRepository extends JpaRepository<Scene, String> {

    List<Scene> findByProjectIdOrderBySceneNumber(String projectId);
}
