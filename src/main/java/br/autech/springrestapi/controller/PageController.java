package br.autech.springrestapi.controller;

import br.autech.springrestapi.dtos.HomePageData;
import br.autech.springrestapi.service.PageDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/page_data")
@RequiredArgsConstructor
public class PageController {
   private final PageDataService pageDataService;

   @GetMapping("/home")
   ResponseEntity<HomePageData> getHomePageData(){
      return ResponseEntity.ok(pageDataService.getHomePageData());
   }
}
