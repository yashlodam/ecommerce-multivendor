package com.zosh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.domain.HomeCategorySection;
import com.zosh.model.HomeCategory;
import com.zosh.response.ApiResponse;
import com.zosh.service.HomeCategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/admin/home-category")
@Tag(name = "Admin - Home Categories", description = "Administrative endpoints for homepage categories, shop-by-category, and banners")
@PreAuthorize("hasRole('ADMIN')")
public class HomeCategoryController {

    @Autowired
    private HomeCategoryService homeCategoryService;

    @GetMapping
    @Operation(summary = "Get all homepage promotional categories, optionally filtered by section")
    public ResponseEntity<List<HomeCategory>> getHomeCategories(
            @RequestParam(required = false) HomeCategorySection section) {
        List<HomeCategory> categories = section != null
                ? homeCategoryService.getHomeCategoriesBySection(section)
                : homeCategoryService.getAllHomeCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a homepage promotional category by ID")
    public ResponseEntity<HomeCategory> getHomeCategoryById(@PathVariable Long id) {
        HomeCategory category = homeCategoryService.getHomeCategoryById(id);
        return ResponseEntity.ok(category);
    }

    @PostMapping
    @Operation(summary = "Create a new homepage category (e.g. Shop By Category, Electronics, Grid, Deals)")
    public ResponseEntity<HomeCategory> createHomeCategory(@RequestBody HomeCategory homeCategory) {
        HomeCategory createdCategory = homeCategoryService.createHomeCategory(homeCategory);
        return new ResponseEntity<>(createdCategory, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a homepage promotional category by ID")
    public ResponseEntity<HomeCategory> updateHomeCategory(
            @PathVariable Long id,
            @RequestBody HomeCategory homeCategory) {
        HomeCategory updateCategory = homeCategoryService.updateHomeCategory(homeCategory, id);
        return ResponseEntity.ok(updateCategory);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a homepage promotional category by ID")
    public ResponseEntity<ApiResponse> deleteHomeCategory(@PathVariable Long id) {
        homeCategoryService.deleteHomeCategory(id);
        ApiResponse response = new ApiResponse();
        response.setMessage("Home category deleted successfully");
        return ResponseEntity.ok(response);
    }
}
