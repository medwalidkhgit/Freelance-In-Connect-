package org.sid.media_service.dto;

public record MediaUploadResponse(
        String container,
        String blobName,
        String url,
        String signedUrl,
        String contentType,
        long size
) {
}
