package com.example.documenttracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class DocumentTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentTrackerApplication.class, args);
    }
}