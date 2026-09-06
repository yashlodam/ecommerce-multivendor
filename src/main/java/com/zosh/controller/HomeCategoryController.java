package com.zosh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.model.HomeCategory;
import com.zosh.service.HomeCategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/admin/home-category")
@Tag(name = "Admin - Home Categories", description = "Administrative endpoints for homepage categories and banners")
@PreAuthorize("hasRole('ADMIN')")
public class HomeCategoryController {

    @Autowired
    private HomeCategoryService homeCategoryService;

    @GetMapping
    @Operation(summary = "Get all homepage promotional categories")
    public ResponseEntity<List<HomeCategory>> getHomeCategory() {
        List<HomeCategory> categories = homeCategoryService.getAllHomeCategories();
        return ResponseEntity.ok(categories);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a homepage promotional category by ID")
    public ResponseEntity<HomeCategory> updateHomeCategory(
            @PathVariable Long id,
            @RequestBody HomeCategory homeCategory) {
        HomeCategory updateCategory = homeCategoryService.updateHomeCategory(homeCategory, id);
        return ResponseEntity.ok(updateCategory);
    }
}
