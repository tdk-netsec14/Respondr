package com.respondr.oncall;

import com.respondr.oncall.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class OnCallController {

    private final OnCallService onCallService;

    public OnCallController(OnCallService onCallService) {
        this.onCallService = onCallService;
    }

    @GetMapping("/api/v1/on-call/schedules")
    public Page<ScheduleResponse> listSchedules(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return onCallService.listSchedules(pageable);
    }

    @GetMapping("/api/v1/on-call/schedules/{id}")
    public ScheduleResponse getById(@PathVariable UUID id) {
        return onCallService.getById(id);
    }

    @PostMapping("/api/v1/teams/{teamId}/on-call/schedules")
    public ResponseEntity<ScheduleResponse> createSchedule(
            @PathVariable UUID teamId,
            @Valid @RequestBody CreateScheduleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(onCallService.createSchedule(teamId, req));
    }

    @DeleteMapping("/api/v1/on-call/schedules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSchedule(@PathVariable UUID id) {
        onCallService.deleteSchedule(id);
    }

    @GetMapping("/api/v1/on-call/schedules/{id}/current-responder")
    public ResponderResponse getCurrentResponder(@PathVariable UUID id) {
        return onCallService.getCurrentResponder(id);
    }

    @GetMapping("/api/v1/on-call/schedules/{id}/overrides")
    public List<OverrideResponse> getOverrides(@PathVariable UUID id) {
        return onCallService.getOverrides(id);
    }

    @PostMapping("/api/v1/on-call/schedules/{id}/overrides")
    public ResponseEntity<OverrideResponse> createOverride(
            @PathVariable UUID id,
            @Valid @RequestBody CreateOverrideRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(onCallService.createOverride(id, req));
    }
}
