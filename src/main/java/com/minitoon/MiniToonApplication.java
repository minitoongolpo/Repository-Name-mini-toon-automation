package com.minitoon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MiniToon AI Automation Application
 * 
 * A production-ready full-stack Java Spring Boot application that automatically
 * generates Bengali YouTube Shorts and Instagram Reels using AI services.
 * 
 * Features:
 * - Daily automatic AI video generation
 * - Daily automatic YouTube Shorts upload
 * - Daily automatic Instagram Reels upload
 * - Daily automatic Facebook Page upload
 * - Cloud-hosted 24/7 automation with Docker support
 * 
 * @author MiniToon Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class MiniToonApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniToonApplication.class, args);
    }
}
