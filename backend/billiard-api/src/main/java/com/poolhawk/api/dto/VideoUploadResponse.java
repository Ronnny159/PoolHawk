package com.poolhawk.api.dto;

public record VideoUploadResponse(
        String filename,
        String filePath,
        long fileSize,
        String message
) {
}