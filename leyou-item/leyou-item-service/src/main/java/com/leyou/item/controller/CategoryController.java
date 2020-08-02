package com.leyou.item.controller;

import com.leyou.item.pojo.Category;
import com.leyou.item.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

@GetMapping("list")
public ResponseEntity<List<Category>> queryCategoryByPid(@RequestParam(value = "pid",defaultValue = "0") Long pid){

     if (pid == null || pid < 0) {
         //return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
         return ResponseEntity.badRequest().build();
     }
     List<Category> categories = this.categoryService.queryCategoriesByPid(pid);
     if (CollectionUtils.isEmpty(categories)) {
         //return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
         return ResponseEntity.notFound().build();
     }

     return ResponseEntity.ok(categories);
    }


    @GetMapping
    public ResponseEntity<List<String>> queryNameByIds(@RequestParam("ids") List<Long> ids){
        List<String> names = this.categoryService.queryNameByIds(ids);
        if (CollectionUtils.isEmpty(names)) {
            //return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(names);
    }

    }

