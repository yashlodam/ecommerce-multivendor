package com.zosh.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zosh.model.Product;
import com.zosh.model.ProductVariant;
import com.zosh.repository.ProductRepository;
import com.zosh.repository.ProductVariantRepository;

/**
 * Runs once on startup to migrate all existing products that have no variants yet.
 *
 * For each such product, a single default variant is created using:
 *   - variantName = first entry in product.sizes (or "Standard" if blank)
 *   - mrpPrice / sellingPrice / discountPercent = product-level values
 *   - quantity = product.quantity
 *   - isDefault = true
 *
 * This migration is idempotent — it checks existsByProduct() before creating,
 * so restarting the server never creates duplicate variants.
 */
@Service
public class DataMigrationService {

    private static final Logger log = LoggerFactory.getLogger(DataMigrationService.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateProductsToVariants() {
        List<Product> allProducts = productRepository.findAll();
        int migrated = 0;

        for (Product product : allProducts) {
            if (!variantRepository.existsByProduct(product)) {
                // Product has no variants — create one default variant
                ProductVariant defaultVariant = new ProductVariant();
                defaultVariant.setProduct(product);

                String variantName = "Standard";
                if (product.getSizes() != null && !product.getSizes().isBlank()) {
                    // Take the first size in the comma-separated list
                    variantName = product.getSizes().split(",")[0].trim();
                }
                defaultVariant.setVariantName(variantName);

                defaultVariant.setMrpPrice(
                    product.getMrpPrice() != null ? product.getMrpPrice() : 0);
                defaultVariant.setSellingPrice(
                    product.getSellingPrice() != null ? product.getSellingPrice() : 0);
                defaultVariant.setDiscountPercent(
                    product.getDiscountPercent() != null ? product.getDiscountPercent() : 0);
                defaultVariant.setQuantity(
                    product.getQuantity() != null ? product.getQuantity() : 0);
                defaultVariant.setDefault(true);

                variantRepository.save(defaultVariant);
                migrated++;

                log.info("Migrated product id={} title='{}' → default variant '{}'",
                        product.getId(), product.getTitle(), variantName);
            }
        }

        if (migrated > 0) {
            log.info("DataMigrationService: migrated {} product(s) to variant model.", migrated);
        } else {
            log.info("DataMigrationService: all products already have variants — nothing to migrate.");
        }
    }
}
