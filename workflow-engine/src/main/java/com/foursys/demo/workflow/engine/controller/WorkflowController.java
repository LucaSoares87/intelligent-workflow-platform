package com.foursys.demo.workflow.engine.controller;

import com.foursys.demo.common.dto.WorkflowDTO;
import com.foursys.demo.workflow.engine.service.WorkflowRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/workflows")
public class WorkflowController {
  private final WorkflowRepository repo;
  public WorkflowController(WorkflowRepository repo) { this.repo = repo; }

  @PostMapping
  public ResponseEntity<?> create(@RequestBody WorkflowDTO dto) {
    repo.save(dto);
    return ResponseEntity.ok("created:"+dto.id);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> get(@PathVariable String id) {
    var wf = repo.find(id);
    if (wf == null) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(wf);
  }
}
