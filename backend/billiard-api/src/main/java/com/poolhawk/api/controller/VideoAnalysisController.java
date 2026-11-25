package com.poolhawk.api.controller;

import com.poolhawk.api.dto.VideoUploadResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analysis")
public class VideoAnalysisController {

    @PostMapping("/upload")
    public ResponseEntity<VideoUploadResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "matchId", required = false) String matchId,
            HttpServletRequest request
    ) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo debe ser un video");
        }

        if (file.getSize() > 500 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El video no puede exceder 500MB");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                originalFilename = "video.mp4";
            }
            String filename = UUID.randomUUID() + "_" + originalFilename;

            Path uploadsDir = Paths.get("uploads/videos");
            Files.createDirectories(uploadsDir);

            Path filePath = uploadsDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String absolutePath = filePath.toAbsolutePath().toString();

            VideoUploadResponse response = new VideoUploadResponse(
                    filename,
                    absolutePath,
                    file.getSize(),
                    "Video guardado exitosamente. Use el agente de visión para procesarlo."
            );

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al guardar el video: " + e.getMessage());
        }
    }
}