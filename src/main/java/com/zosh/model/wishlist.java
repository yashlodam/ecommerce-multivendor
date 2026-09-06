package com.zosh.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "wishlists",
    indexes = {
        @Index(name = "idx_wishlist_user_id", columnList = "user_id")
    }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "wishlist_seq")
    @SequenceGenerator(name = "wishlist_seq", sequenceName = "wishlist_sequence", allocationSize = 1)
    private Long id;

    @OneToOne(optional = false)
    @JsonIgnoreProperties({"addresses", "usedCoupons", "password", "hibernateLazyInitializer", "handler"})
    private User user;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Product> products = new HashSet<>();

    public Wishlist() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Set<Product> getProducts() {
        return products;
    }

    public void setProducts(Set<Product> products) {
        this.products = products;
    }
}
