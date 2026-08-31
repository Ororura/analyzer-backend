package com.ororura.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AnalyzerApplication {

    static void main(String[] args) {
        SpringApplication.run(AnalyzerApplication.class, args);
    }

}
