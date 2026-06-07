package org.sid.media_service.controller;

import org.sid.media_service.dto.MediaUploadResponse;
import org.sid.media_service.dto.SignedUrlResponse;
import org.sid.media_service.service.MediaStorageService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final MediaStorageService mediaStorageService;

    public MediaController(MediaStorageService mediaStorageService) {
        this.mediaStorageService = mediaStorageService;
    }

    @PostMapping(value = "/profile-pictures", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MediaUploadResponse uploadProfilePicture(@RequestPart("file") MultipartFile file,
                                                    Authentication authentication) {
        return mediaStorageService.uploadProfilePicture(file, ownerId(authentication));
    }

    @PostMapping(value = "/company-logos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MediaUploadResponse uploadCompanyLogo(@RequestPart("file") MultipartFile file,
                                                 Authentication authentication) {
        return mediaStorageService.uploadCompanyLogo(file, ownerId(authentication));
    }

    @PostMapping(value = "/cvs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MediaUploadResponse uploadCv(@RequestPart("file") MultipartFile file,
                                        Authentication authentication) {
        return mediaStorageService.uploadCv(file, ownerId(authentication));
    }

    @GetMapping("/read-url")
    public SignedUrlResponse createReadUrl(@RequestParam(required = false) String url,
                                           @RequestParam(required = false) String container,
                                           @RequestParam(required = false) String blobName) {
        return mediaStorageService.createReadUrl(url, container, blobName);
    }

    private String ownerId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "registration";
        }
        return authentication.getName();
    }
}
