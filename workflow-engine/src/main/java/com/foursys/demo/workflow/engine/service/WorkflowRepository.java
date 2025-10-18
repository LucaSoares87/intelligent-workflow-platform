package com.foursys.demo.workflow.engine.service;

import com.foursys.demo.common.dto.WorkflowDTO;
import org.springframework.stereotype.Repository;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Repository
public class WorkflowRepository {
  private final Map<String, WorkflowDTO> store = new ConcurrentHashMap<>();

  public void save(WorkflowDTO dto) { store.put(dto.id, dto); }
  public WorkflowDTO find(String id) { return store.get(id); }
}
