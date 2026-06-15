package com.zosh.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.zosh.config.JwtProvider;
import com.zosh.domain.AccountStatus;
import com.zosh.domain.USER_ROLE;
import com.zosh.model.Address;
import com.zosh.model.Seller;
import com.zosh.repository.AddressRepository;
import com.zosh.repository.SellerRepository;
import com.zosh.service.SellerService;

@Service
public class SellerServiceImpl implements SellerService {
	
	@Autowired
	private SellerRepository sellerepo;
	
	@Autowired
	private JwtProvider jwtprovider;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private AddressRepository addressrepo;

	@Override
	public Seller getSellerProfile(String jwt) {
		
		String email = jwtprovider.getEmailFromJwtToken(jwt);
		
		
		return this.getSellerByEmail(email);
	}

	@Override
	public Seller createSeller(Seller seller) {
		
		Seller sellerExist = sellerepo.findByEmail(seller.getEmail());
		if(sellerExist!=null) {
			throw new RuntimeException("Seller  already exist, used different email");
		}
		
		Address savedAddress = addressrepo.save(seller.getPickupAddress());
		
		Seller newSeller = new Seller();
		newSeller.setEmail(seller.getEmail());
		newSeller.setPassword(passwordEncoder.encode(seller.getPassword()));
		newSeller.setSellerName(seller.getSellerName());
		newSeller.setPickupAddress(savedAddress);
		newSeller.setGSTIN(seller.getGSTIN());
		newSeller.setRole(USER_ROLE.ROLE_SELLER);
		newSeller.setMobile(seller.getMobile());
		newSeller.setBankDetails(seller.getBankDetails());
		newSeller.setBusinesssDetails(seller.getBusinesssDetails());
		
		
		return sellerepo.save(newSeller);
	}

	@Override
	public Seller getSellerById(Long id) {
		
		
		
		return sellerepo.findById(id).orElseThrow(()-> new UsernameNotFoundException("seller not found with id"));
	}

	@Override
	public Seller getSellerByEmail(String email) {

	    Seller seller = sellerepo.findByEmail(email);

	    if (seller == null) {
	        throw new RuntimeException("Seller not found with email: " + email);
	    }

	    return seller;
	}

	@Override
	public List<Seller> getAllSellers(AccountStatus status) {
		
		return sellerepo.findByAccountStatus(status);
	}

	@Override
	public Seller updateSeller(Long id, Seller seller) {
		
		Seller existingSeller = sellerepo.findById(id).orElseThrow(()-> new UsernameNotFoundException("Seller not found"));
		
		
		if(seller.getSellerName() !=null) {
			
			existingSeller.setSellerName(seller.getSellerName());
		}
		
		if(seller.getMobile() !=null) {
			existingSeller.setMobile(seller.getMobile());
		}
		
		if(seller.getEmail() !=null) {
			existingSeller.setEmail(seller.getEmail());
		}
		if(seller.getBusinesssDetails()!=null && seller.getBusinesssDetails().getBusinessName()!=null) {
			existingSeller.getBusinesssDetails().setBusinessName(seller.getBusinesssDetails().getBusinessName());
		}
		
		if(seller.getBankDetails()!=null && seller.getBankDetails().getAccountHolderName()!=null
				&& seller.getBankDetails().getIfscCode() !=null
				&& seller.getBankDetails().getAccountNumber()!=null
				) {
			existingSeller.getBankDetails().setAccountHolderName(seller.getBankDetails().getAccountHolderName());
			
			
			existingSeller.getBankDetails().setAccountNumber(seller.getBankDetails().getAccountNumber());
			
			existingSeller.getBankDetails().setIfscCode(seller.getBankDetails().getIfscCode());
		}
		
		if(seller.getPickupAddress()!=null
				&& seller.getPickupAddress().getAddress() !=null
				&& seller.getPickupAddress().getCity() !=null
				&& seller.getPickupAddress().getMobile() !=null
				&& seller.getPickupAddress().getState() !=null
				) {
			
			existingSeller.getPickupAddress().setAddress(seller.getPickupAddress().getAddress());
			existingSeller.getPickupAddress().setCity(seller.getPickupAddress().getCity());
			existingSeller.getPickupAddress().setState(seller.getPickupAddress().getState());
			existingSeller.getPickupAddress().setMobile(seller.getPickupAddress().getMobile());
			existingSeller.getPickupAddress().setPinCode(seller.getPickupAddress().getPinCode());
			
		}
		
		if(seller.getGSTIN()!=null) {
			existingSeller.setGSTIN(seller.getGSTIN());
		}
		
		
		
		return sellerepo.save(existingSeller);
	}

	@Override
	public void deleteSeller(Long id) {
	
		Seller seller = getSellerById(id);
		
		sellerepo.delete(seller);
		
	}

	@Override
	public Seller verifyEmail(String email, String otp) {
		
		Seller seller = getSellerByEmail(email);
		seller.setEmailVerified(true);
		
		return sellerepo.save(seller);
	}

	@Override
	public Seller updateSellerAccountStatus(Long id, AccountStatus status) {
		
		Seller seller = getSellerById(id);
		seller.setAccountStatus(status);
		
		return sellerepo.save(seller);
	}

	
}
