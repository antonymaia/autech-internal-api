package br.autech.springrestapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/monitor")
public class MonitorController {

   @RequestMapping(name = "/teste", method = {RequestMethod.GET, RequestMethod.HEAD})
   public ResponseEntity<Void> teste(){
      return ResponseEntity.ok().build();
   }
}
