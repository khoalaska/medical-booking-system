package fpt.medical.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileStorageConfig {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;
}
