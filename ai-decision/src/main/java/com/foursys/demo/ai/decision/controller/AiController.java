package com.foursys.demo.ai.decision.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class AiController {
  @PostMapping("/decide")
  public String decide(@RequestBody(required=false) Map<String,Object> payload){
    // MOCK simples: 10% reprova, 90% aprova
    double r = Math.random();
    return r < 0.1 ? "REPROVAR" : "APROVAR";
  }
}
