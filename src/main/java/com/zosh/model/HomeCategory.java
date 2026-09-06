package com.zosh.model;

import com.zosh.domain.HomeCategorySection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "home_categories",
    indexes = {
        @Index(name = "idx_home_cat_section", columnList = "section"),
        @Index(name = "idx_home_cat_id", columnList = "categoryId")
    }
)
public class HomeCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "home_cat_seq")
    @SequenceGenerator(name = "home_cat_seq", sequenceName = "home_category_sequence", allocationSize = 1)
    private Long id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String image;

    private String categoryId;

    @Enumerated(EnumType.STRING)
    private HomeCategorySection section;

    private Integer priority;

    public HomeCategory() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public HomeCategorySection getSection() {
        return section;
    }

    public void setSection(HomeCategorySection section) {
        this.section = section;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
