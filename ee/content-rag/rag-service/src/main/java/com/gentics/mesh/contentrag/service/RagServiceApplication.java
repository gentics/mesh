package com.gentics.mesh.contentrag.service;

import com.gentics.mesh.contentrag.service.config.RagProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Einstiegspunkt des RAG-Service (Konzept Abschnitt 4/8).
 */
@SpringBootApplication
@EnableConfigurationProperties(RagProperties.class)
public class RagServiceApplication {

    /**
     * Startet die Spring-Boot-Anwendung.
     *
     * @param args Kommandozeilenargumente
     */
    public static void main(String[] args) {
        SpringApplication.run(RagServiceApplication.class, args);
    }
}
