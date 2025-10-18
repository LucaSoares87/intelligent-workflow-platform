package com.foursys.demo.orchestrator.controller;

import com.foursys.demo.orchestrator.service.OrchestrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/execute")
public class OrchestratorController {
  private final OrchestrationService service;
  public OrchestratorController(OrchestrationService s){ this.service = s; }

  @PostMapping("/{workflowId}")
  public ResponseEntity<?> execute(@PathVariable String workflowId){
    return ResponseEntity.ok(service.execute(workflowId));
  }
}
