package com.leyou.search.controller;

import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


public class SearcherController {
    @Autowired
  private   SearchService searchService;

    @PostMapping("page")
    public ResponseEntity<SearchResult >search(@RequestBody SearchRequest request){

      SearchResult rseult  = this.searchService.search(request);
       if (rseult==null || CollectionUtils.isEmpty(rseult.getItems())){
           return ResponseEntity.notFound().build();
       }

       return ResponseEntity.ok(rseult);
        }

    }


