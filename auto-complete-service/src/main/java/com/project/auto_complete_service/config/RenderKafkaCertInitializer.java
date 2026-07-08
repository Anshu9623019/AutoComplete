package com.project.auto_complete_service.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class RenderKafkaCertInitializer implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        try {
            System.out.println("Render Lifecycle Hook: Extracting Kafka certificates before context initializes...");
            extractCert("client.truststore.jks", "client.truststore.jks");
            extractCert("client.keystore.jks", "client.keystore.jks");
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Pre-startup certificate extraction failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void extractCert(String classpathPath, String absoluteTargetPath) throws Exception {
        ClassPathResource resource = new ClassPathResource(classpathPath);

        if (!resource.exists()) {
            resource = new ClassPathResource("/" + classpathPath);
        }

        if (!resource.exists()) {
            throw new java.io.FileNotFoundException("Could not find classpath resource at: " + classpathPath);
        }

        File targetFile = new File(absoluteTargetPath);
        if (targetFile.getParentFile() != null) {
            targetFile.getParentFile().mkdirs();
        }

        try (InputStream in = resource.getInputStream();
             FileOutputStream out = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        System.out.println("Render Cert Extractor: Extracted file successfully to " + targetFile.getAbsolutePath());
    }
}