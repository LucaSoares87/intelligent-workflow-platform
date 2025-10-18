package com.foursys.demo.connectors.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/invoke")
public class ConnectorController {
  @PostMapping("/{name}")
  public String invoke(@PathVariable String name){
    return "OK:" + name;
  }
}
