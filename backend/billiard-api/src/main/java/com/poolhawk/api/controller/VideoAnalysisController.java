package com.poolhawk.api.controller;

import com.poolhawk.api.dto.VideoUploadResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analysis")
public class VideoAnalysisController {

    private static final Path UPLOADS_DIR = Paths.get("uploads/videos").toAbsolutePath();

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

            Files.createDirectories(UPLOADS_DIR);

            Path filePath = UPLOADS_DIR.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Devolver URL accesible vía HTTP, no ruta absoluta del servidor
            String videoUrl = "/api/v1/analysis/video/" + filename;

            VideoUploadResponse response = new VideoUploadResponse(
                    filename,
                    videoUrl,
                    file.getSize(),
                    "Video guardado exitosamente. Use el agente de visión para procesarlo."
            );

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al guardar el video: " + e.getMessage());
        }
    }

    @GetMapping("/video/{filename}")
    public ResponseEntity<Resource> serveVideo(
            @PathVariable String filename,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // Validación básica de seguridad: solo nombres de archivo alfanuméricos + _ . -
        if (!filename.matches("^[a-f0-9\\-_]+\\.(mp4|webm|avi|mov|mkv)$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de archivo inválido");
        }

        Path filePath = UPLOADS_DIR.resolve(filename).normalize();

        // Prevenir path traversal
        if (!filePath.startsWith(UPLOADS_DIR)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado");
        }

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Video no encontrado");
        }

        try {
            Resource resource = new PathResource(filePath);
            long contentLength = Files.size(filePath);
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "video/mp4";
            }

            // Soporte Range requests para streaming
            String rangeHeader = request.getHeader("Range");
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                return handleRangeRequest(rangeHeader, resource, contentLength, contentType);
            }

            // Respuesta completa
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                    .body(resource);

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al leer el video: " + e.getMessage());
        }
    }

    private ResponseEntity<Resource> handleRangeRequest(
            String rangeHeader, Resource resource, long contentLength, String contentType
    ) throws IOException {
        // Parsear "bytes=start-end"
        String rangeValue = rangeHeader.substring("bytes=".length()).trim();
        String[] parts = rangeValue.split("-");
        long start = Long.parseLong(parts[0]);
        long end = parts.length > 1 && !parts[1].isEmpty() ? Long.parseLong(parts[1]) : contentLength - 1;

        if (start >= contentLength || end >= contentLength || start > end) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength)
                    .build();
        }

        long rangeLength = end - start + 1;
        PathResource pathResource = (PathResource) resource;
        
        try (var inputStream = Files.newInputStream(pathResource.getFile().toPath())) {
            inputStream.skip(start);
            byte[] buffer = new byte[(int) rangeLength];
            int read = inputStream.readNBytes(buffer, 0, (int) rangeLength);
            
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(read))
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + (start + read - 1) + "/" + contentLength)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                    .body(new org.springframework.core.io.ByteArrayResource(buffer) {
                        @Override
                        public String getFilename() {
                            return pathResource.getFilename();
                        }
                    });
        }
    }
}