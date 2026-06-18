package com.zosh.service;

import java.util.List;

import com.zosh.domain.AccountStatus;
import com.zosh.exceptions.SellerException;
import com.zosh.model.Seller;

public interface SellerService {

     Seller getSellerProfile(String jwt);
     Seller createSeller(Seller seller);
     Seller getSellerById(Long id) throws SellerException;
     Seller getSellerByEmail(String email);
     List<Seller> getAllSellers(AccountStatus status);
     Seller updateSeller(Long id,Seller seller);
     void deleteSeller(Long id) throws SellerException;
     Seller verifyEmail(String email,String otp);
     Seller updateSellerAccountStatus(Long id,AccountStatus status) throws SellerException;
     List<Seller> getAllSellers();
}
