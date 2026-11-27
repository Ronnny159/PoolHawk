package com.poolhawk.api.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Carga variables de entorno desde archivo .env ubicado en la raíz del proyecto.
 * Esto permite que spring-boot:run funcione correctamente sin configuración manual.
 */
@Component
public class DotenvConfig implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DotenvConfig.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Buscar .env en directorio actual y directorios padre hasta la raíz
        File currentDir = new File(".").getAbsoluteFile();
        File dotenvFile = null;
        
        File searchDir = currentDir;
        while (searchDir != null) {
            File candidate = new File(searchDir, ".env");
            if (candidate.exists() && candidate.isFile()) {
                dotenvFile = candidate;
                break;
            }
            searchDir = searchDir.getParentFile();
        }

        if (dotenvFile == null) {
            log.warn("No se encontró archivo .env en la jerarquía de directorios desde: {}", currentDir);
            return;
        }

        log.info("Cargando variables de entorno desde: {}", dotenvFile.getAbsolutePath());
        
        Dotenv dotenv = Dotenv.configure()
                .directory(dotenvFile.getParent())
                .filename(dotenvFile.getName())
                .ignoreIfMissing()
                .load();

        Map<String, Object> envMap = new HashMap<>();
        for (io.github.cdimascio.dotenv.DotenvEntry entry : dotenv.entries()) {
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                envMap.put(entry.getKey(), value);
            }
        }

        if (!envMap.isEmpty()) {
            MapPropertySource propertySource = new MapPropertySource("dotenv", envMap);
            environment.getPropertySources().addFirst(propertySource);
            log.info("Cargadas {} variables desde .env", envMap.size());
        }
    }
}