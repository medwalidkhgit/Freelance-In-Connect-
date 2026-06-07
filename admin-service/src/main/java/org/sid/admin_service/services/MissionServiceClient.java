package org.sid.admin_service.services;


import org.sid.admin_service.dto.MissionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;


@FeignClient(name = "mission-service", url = "${services.mission.url:http://mission-service:8084}")
public interface MissionServiceClient {

    @GetMapping("/api/missions/admin/all")
    List<MissionDto> getAllMissions();

    @GetMapping("/api/missions/company/{companyId}/all")
    List<MissionDto> getAllMissionsByCompany(@PathVariable("companyId") Long companyId);

}
