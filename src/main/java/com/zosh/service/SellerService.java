package com.zosh.service;

import java.util.List;

import com.zosh.domain.AccountStatus;
import com.zosh.exceptions.DuplicateResourceException;
import com.zosh.exceptions.SellerException;
import com.zosh.model.Seller;
import com.zosh.request.SellerRequest;

public interface SellerService {

    Seller getSellerProfile(String jwt);

    /** Creates a new Seller from a validated DTO — never from a raw JPA entity. */
    Seller createSeller(SellerRequest req) throws DuplicateResourceException;

    Seller getSellerById(Long id) throws SellerException;

    Seller getSellerByEmail(String email);

    List<Seller> getAllSellers(AccountStatus status);

    /** Updates an existing seller's profile from a DTO. */
    Seller updateSeller(Long id, SellerRequest req) throws SellerException;

    void deleteSeller(Long id) throws SellerException;

    Seller verifyEmail(String email, String otp);

    Seller updateSellerAccountStatus(Long id, AccountStatus status) throws SellerException;

    List<Seller> getAllSellers();

    void verifySellerIsActive(Seller seller) throws SellerException;
}
