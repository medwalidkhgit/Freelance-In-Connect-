package org.sid.media_service.dto;

public record SignedUrlResponse(
        String container,
        String blobName,
        String url,
        String signedUrl,
        long expiresInMinutes
) {
}
