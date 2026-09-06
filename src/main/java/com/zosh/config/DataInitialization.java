package com.zosh.config;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.zosh.domain.HomeCategorySection;
import com.zosh.domain.USER_ROLE;
import com.zosh.model.HomeCategory;
import com.zosh.model.User;
import com.zosh.repository.HomeCategoryRepository;
import com.zosh.repository.UserRepository;
import com.zosh.response.HomeService;

/**
 * Seeds the admin user and default home promotional categories on first startup.
 * Admin credentials are read from environment variables — never hardcoded.
 */
@Component
public class DataInitialization implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitialization.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private HomeCategoryRepository homeCategoryRepository;

    @Autowired
    private HomeService homeService;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.full-name:ShopSphere Admin}")
    private String adminFullName;

    @Override
    public void run(String... args) {
        // 1. Seed Admin User (only if credentials are provided in environment)
        if (adminEmail != null && !adminEmail.isBlank() && adminPassword != null && !adminPassword.isBlank()) {
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                User admin = new User();
                admin.setFullName(adminFullName);
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setRole(USER_ROLE.ROLE_ADMIN);

                userRepository.save(admin);
                log.info("Admin account created: {}", adminEmail);
            } else {
                log.debug("Admin account already exists: {}", adminEmail);
            }
        } else {
            log.info("No admin credentials configured via APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD; skipping initial admin creation.");
        }

        // 2. Seed Default Home Categories if none exist
        if (homeCategoryRepository.count() == 0) {
            log.info("Initializing default home promotional categories and deals...");
            List<HomeCategory> defaultCategories = buildDefaultHomeCategories();
            List<HomeCategory> saved = homeCategoryRepository.saveAll(defaultCategories);
            homeService.createHomePageData(saved);
            log.info("Successfully initialized {} home promotional categories", saved.size());
        }
    }

    private List<HomeCategory> buildDefaultHomeCategories() {
        List<HomeCategory> list = new ArrayList<>();

        // Deals
        list.add(createCat("Men T shirts", "men_tshirts", HomeCategorySection.DEALS, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRXdzuY2hiflaDMMxi2KF8peYWTyJwrSVfBCPzuoKdDrA&s=10"));
        list.add(createCat("Women Skirts", "women_skirts_plazzos", HomeCategorySection.DEALS, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRnd33ezyqf-MckjmGMqboYdJyhaHClKbfKpy8dhglVaw&s=10"));
        list.add(createCat("Men Formal Shirts", "men_formal_shirts", HomeCategorySection.DEALS, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSCHhneaDLnh7KcLcDyOnQxlOe9FJO_ggymwCXw6jBJ6A&s=10"));
        list.add(createCat("Women Saree", "women_sarees", HomeCategorySection.DEALS, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQCJPFi8X8Lahn25m2wu_2ZxZ-NKIdUYAEH83m7Vi0nCA&s=10"));
        list.add(createCat("Smart Watches", "smart_watches", HomeCategorySection.DEALS, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQOffUH0HVB4K168ehbplDKckhuecjV6LoatXnziDd1KA&s=10"));
        list.add(createCat("Men Festive Wear", "men_indian_and_festive_wear", HomeCategorySection.DEALS, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSIoV1WHgDusN9AyshrGZ-_Avd-Jhpr9OdMEiUzAF0yjA&s=10"));

        // Electric Categories
        list.add(createCat("Laptop", "laptops", HomeCategorySection.ELECTRIC_CATEGORIES, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQQbnn7bUe-pthNhCm296-VpXADc8tz1dBC1-EEI_ePtg&s"));
        list.add(createCat("Smartphones", "smartphones", HomeCategorySection.ELECTRIC_CATEGORIES, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSSQgZYXrrm3L5XX6naNP67gAEbpsuA9LeNN0Z0v4jzcg&s=10"));
        list.add(createCat("Headphones", "headphones", HomeCategorySection.ELECTRIC_CATEGORIES, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQWlP_1orqXc_aVIUx0K7Ku8y3R1OFwQUobV7RjC2xq-g&s"));
        list.add(createCat("Smart Watches", "smart_watches", HomeCategorySection.ELECTRIC_CATEGORIES, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQfW8zR4k6Vi_xdwbSlrCIy_akIOGcQtmCdQLERHLpLSg&s=10"));
        list.add(createCat("Speakers", "speakers", HomeCategorySection.ELECTRIC_CATEGORIES, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTjVu9jLGObIU_HGlvk8jX21mVlogM-N0B9hNTV-j7QEw&s=10"));
        list.add(createCat("Cameras", "cameras", HomeCategorySection.ELECTRIC_CATEGORIES, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTFOM-aH4mGk1kb9dj2s9YT_GBAcEhCRVRvRxnZty3rzg&s=10"));
        list.add(createCat("Smart TVs", "televisions", HomeCategorySection.ELECTRIC_CATEGORIES, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQhnIaFLKUE2uljOFsu1J_LHpiO1-jt49C47uf1K3NOXQ&s=10"));

        // Shop by Categories
        list.add(createCat("Men's TopWear", "men_topwear", HomeCategorySection.SHOP_BY_CATEGORIES, "https://rukminim1.flixcart.com/image/964/964/xif0q/t-shirt/e/g/x/xxl-po2-fs49-blk-notme-brwn-roar-leotude-original-imah8cru3sbpcyyw.jpeg?q=90"));
        list.add(createCat("Men's BottomWear", "men_bottomwear", HomeCategorySection.SHOP_BY_CATEGORIES, "https://rukminim1.flixcart.com/image/964/964/xif0q/jean/x/8/q/30-flipkart-ice-light-blue-30-hangerhood-original-imahm2cgkquakyed.jpeg?q=90"));
        list.add(createCat("Women's Western", "women_western_wear", HomeCategorySection.SHOP_BY_CATEGORIES, "https://rukminim1.flixcart.com/image/964/964/xif0q/top/z/n/q/s-1-d988-topss-deklook-original-imahnyhsmncbymyw.jpeg?q=90"));
        list.add(createCat("Women's Sarees", "women_sarees", HomeCategorySection.SHOP_BY_CATEGORIES, "https://rukminim1.flixcart.com/image/964/964/xif0q/sari/x/d/d/free-banarasi-avantika-fashion-unstitched-original-imahchzb6ywhh4af.jpeg?q=90"));
        list.add(createCat("Furniture", "home_furniture", HomeCategorySection.SHOP_BY_CATEGORIES, "https://rukminim1.flixcart.com/image/964/964/xif0q/sofa-sectional/g/e/h/symmetrical-72-39-grey-cushion-177-8-cotton-no-20-kd8-kendalwood-original-imahzkz8hmmjhsgk.jpeg?q=90"));

        return list;
    }

    private HomeCategory createCat(String name, String categoryId, HomeCategorySection section, String image) {
        HomeCategory c = new HomeCategory();
        c.setName(name);
        c.setCategoryId(categoryId);
        c.setSection(section);
        c.setImage(image);
        return c;
    }
}