package com.zosh.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zosh.domain.HomeCategorySection;
import com.zosh.model.HomeCategory;

public interface HomeCategoryRepository extends JpaRepository<HomeCategory, Long> {

    Optional<HomeCategory> findByCategoryId(String categoryId);

    List<HomeCategory> findBySection(HomeCategorySection section);
}
