package org.sid.media_service.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.sid.media_service.config.StorageProperties;
import org.sid.media_service.dto.MediaUploadResponse;
import org.sid.media_service.dto.SignedUrlResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MediaStorageService {

    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> PDF_CONTENT_TYPES = Set.of("application/pdf");
    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "application/pdf", ".pdf"
    );

    private final StorageProperties properties;
    private final BlobServiceClient blobServiceClient;

    public MediaStorageService(StorageProperties properties) {
        if (properties.getConnectionString() == null || properties.getConnectionString().isBlank()) {
            throw new IllegalStateException("AZURE_STORAGE_CONNECTION_STRING is required");
        }
        this.properties = properties;
        this.blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(properties.getConnectionString())
                .buildClient();
    }

    public MediaUploadResponse uploadProfilePicture(MultipartFile file, String ownerId) {
        return upload(
                file,
                properties.getProfilePicturesContainer(),
                "profile-pictures",
                ownerId,
                IMAGE_CONTENT_TYPES,
                properties.getImageMaxBytes()
        );
    }

    public MediaUploadResponse uploadCompanyLogo(MultipartFile file, String ownerId) {
        return upload(
                file,
                properties.getCompanyLogosContainer(),
                "company-logos",
                ownerId,
                IMAGE_CONTENT_TYPES,
                properties.getImageMaxBytes()
        );
    }

    public MediaUploadResponse uploadCv(MultipartFile file, String ownerId) {
        return upload(
                file,
                properties.getCvsContainer(),
                "cvs",
                ownerId,
                PDF_CONTENT_TYPES,
                properties.getCvMaxBytes()
        );
    }

    public SignedUrlResponse createReadUrl(String url, String container, String blobName) {
        BlobLocation location = resolveLocation(url, container, blobName);
        assertKnownContainer(location.container());

        BlobClient blobClient = blobServiceClient
                .getBlobContainerClient(location.container())
                .getBlobClient(location.blobName());
        try {
            if (!blobClient.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Blob not found");
            }
            return toSignedUrlResponse(blobClient, location.container(), location.blobName());
        } catch (BlobStorageException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Azure Blob Storage read failed", ex);
        }
    }

    private MediaUploadResponse upload(MultipartFile file,
                                       String containerName,
                                       String category,
                                       String ownerId,
                                       Set<String> allowedContentTypes,
                                       long maxBytes) {
        validate(file, allowedContentTypes, maxBytes);

        String contentType = file.getContentType();
        String blobName = blobName(category, ownerId, contentType);
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobName);

        try (InputStream inputStream = file.getInputStream()) {
            containerClient.createIfNotExists();
            blobClient.upload(inputStream, file.getSize(), true);
            blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));
            String signedUrl = generateReadSas(blobClient);
            return new MediaUploadResponse(
                    containerName,
                    blobName,
                    blobClient.getBlobUrl(),
                    signedUrl,
                    contentType,
                    file.getSize()
            );
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read uploaded file", ex);
        } catch (BlobStorageException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Azure Blob Storage upload failed", ex);
        }
    }

    private void validate(MultipartFile file, Set<String> allowedContentTypes, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File is too large");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedContentTypes.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported file type");
        }
    }

    private String blobName(String category, String ownerId, String contentType) {
        String owner = ownerId == null || ownerId.isBlank()
                ? "unknown"
                : ownerId.replaceAll("[^a-zA-Z0-9._-]", "_");
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String extension = EXTENSIONS_BY_CONTENT_TYPE.getOrDefault(contentType, ".bin");
        return "%s/%s/%s/%s%s".formatted(category, owner, today, UUID.randomUUID(), extension);
    }

    private SignedUrlResponse toSignedUrlResponse(BlobClient blobClient, String container, String blobName) {
        return new SignedUrlResponse(
                container,
                blobName,
                blobClient.getBlobUrl(),
                generateReadSas(blobClient),
                properties.getReadSasTtlMinutes()
        );
    }

    private String generateReadSas(BlobClient blobClient) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(
                now.plusMinutes(properties.getReadSasTtlMinutes()),
                permission
        ).setStartTime(now.minusMinutes(5));
        return blobClient.getBlobUrl() + "?" + blobClient.generateSas(values);
    }

    private BlobLocation resolveLocation(String url, String container, String blobName) {
        if (url != null && !url.isBlank()) {
            return fromUrl(url);
        }
        if (container == null || container.isBlank() || blobName == null || blobName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide either url or container/blobName");
        }
        return new BlobLocation(container, blobName);
    }

    private BlobLocation fromUrl(String url) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            if (path == null || path.length() <= 1) {
                throw new IllegalArgumentException("Missing blob path");
            }
            String[] parts = path.substring(1).split("/", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                throw new IllegalArgumentException("Invalid blob path");
            }
            return new BlobLocation(
                    URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid blob url", ex);
        }
    }

    private void assertKnownContainer(String container) {
        if (!Set.of(
                properties.getProfilePicturesContainer(),
                properties.getCompanyLogosContainer(),
                properties.getCvsContainer()
        ).contains(container)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown media container");
        }
    }

    private record BlobLocation(String container, String blobName) {
    }
}
