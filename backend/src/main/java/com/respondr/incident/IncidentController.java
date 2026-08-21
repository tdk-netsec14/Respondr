package com.respondr.incident;

import com.respondr.incident.dto.*;
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
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    public Page<IncidentResponse> list(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return incidentService.listIncidents(pageable);
    }

    @GetMapping("/{id}")
    public IncidentResponse getById(@PathVariable UUID id) {
        return incidentService.getById(id);
    }

    @PostMapping
    public ResponseEntity<IncidentResponse> create(@Valid @RequestBody CreateIncidentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.create(req));
    }

    @PostMapping("/{id}/acknowledge")
    public IncidentResponse acknowledge(@PathVariable UUID id) {
        return incidentService.acknowledge(id);
    }

    @PostMapping("/{id}/investigate")
    public IncidentResponse investigate(@PathVariable UUID id) {
        return incidentService.investigate(id);
    }

    @PostMapping("/{id}/resolve")
    public IncidentResponse resolve(@PathVariable UUID id) {
        return incidentService.resolve(id);
    }

    @PostMapping("/{id}/reopen")
    public IncidentResponse reopen(@PathVariable UUID id) {
        return incidentService.reopen(id);
    }

    @PostMapping("/{id}/cancel")
    public IncidentResponse cancel(@PathVariable UUID id) {
        return incidentService.cancel(id);
    }

    @GetMapping("/{id}/timeline")
    public List<IncidentEventResponse> getTimeline(@PathVariable UUID id) {
        return incidentService.getTimeline(id);
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> getComments(@PathVariable UUID id) {
        return incidentService.getComments(id);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody CommentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.addComment(id, req));
    }
}
