package com.foursys.demo.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class WorkflowDTO {
  @NotBlank public String id;
  @NotBlank public String name;
  @NotNull public List<StepDTO> steps;

  public static class StepDTO {
    @NotBlank public String id;
    @NotBlank public String type; // validation | ai-decision | connector
    public String connector; // optional
  }
}
