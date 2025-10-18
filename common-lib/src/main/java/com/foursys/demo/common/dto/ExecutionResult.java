package com.foursys.demo.common.dto;

import java.util.ArrayList;
import java.util.List;

public class ExecutionResult {
  public String workflowId;
  public String status; // SUCCESS | FAILED
  public List<String> logs = new ArrayList<>();
}
