package org.sid.application_service.dto;

import lombok.Data;
import org.sid.application_service.entity.ApplicationStatus;

@Data
public class ApplicationStatusRequest {
    private ApplicationStatus status;
}
