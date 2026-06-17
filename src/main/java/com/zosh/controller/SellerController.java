package com.zosh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.config.JwtProvider;
import com.zosh.domain.AccountStatus;
import com.zosh.exceptions.SellerException;
import com.zosh.model.Seller;
import com.zosh.model.SellerReport;
import com.zosh.model.VerificationCode;
import com.zosh.repository.VerificationCodeRepository;
import com.zosh.response.ApiResponse;
import com.zosh.response.AuthResponse;
import com.zosh.response.LoginOtpRequest;
import com.zosh.response.LoginRequest;
import com.zosh.service.AuthService;
import com.zosh.service.EmailService;
import com.zosh.service.SellerReportService;
import com.zosh.service.SellerService;
import com.zosh.utils.OtpUtil;

@RestController
@RequestMapping("/sellers")
public class SellerController {

	@Autowired
	private SellerService sellerService;
	
	@Autowired
	private AuthService authservice;

	
	@Autowired
	private VerificationCodeRepository verificationCoderepository;
	
	@Autowired
	private AuthService authService;
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private JwtProvider jwtprovider;
	
	@Autowired
	private SellerReportService sellerReportService;
	

	
	@PostMapping("/login")
	public ResponseEntity<AuthResponse> loginSeller(@RequestBody LoginRequest req){
		
		
		
		req.setEmail("seller_"+req.getEmail());
		System.out.println(req.getEmail());
		
		AuthResponse res = authService.siging(req);
		
		
		return ResponseEntity.ok(res);
	}
	
	@PatchMapping("/verify/{otp}")
	public ResponseEntity<Seller> verifySellerEmail(
			@PathVariable String otp
			){
		
		VerificationCode code = verificationCoderepository.findByOtp(otp);
		
		if(code==null || code.getOtp().equals(otp)) {
			throw new RuntimeException("worong otp...");
		}
		
		Seller seller = sellerService.verifyEmail(code.getEmail(), otp);
		
		return new ResponseEntity<>(seller,HttpStatus.OK);
	}
	
	@PostMapping
	public ResponseEntity<Seller> createSeller(@RequestBody Seller seller){
		
		Seller savedSeller = sellerService.createSeller(seller);
		
		String otp = OtpUtil.generateOtp();

	    VerificationCode verificationCode =
	            new VerificationCode();

	    verificationCode.setEmail(seller.getEmail());
	    verificationCode.setOtp(otp);

	    verificationCoderepository.save(verificationCode);
	    String frontend_url = "http://localhost:3000/verify-seller/";

	    emailService.sendVerificationOtpEmail(
	            seller.getEmail(),
	            otp,
	            "Zosh Bazar Login/Signup OTP",
	            "Use the following OTP to verify your account."+frontend_url
	    );
	    
	    return new ResponseEntity<>(savedSeller,HttpStatus.CREATED);
		
		
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<Seller> getSellerById(@PathVariable Long id) throws SellerException{
		
		Seller seller = sellerService.getSellerById(id);
		return new ResponseEntity<>(seller,HttpStatus.OK);
		
	}
	
	
	@GetMapping("/profile")
	public ResponseEntity<Seller> getSellerByJwt(@RequestHeader("Authorization") String jwt)
	{
		
		Seller seller = sellerService.getSellerProfile(jwt);
		
		return new ResponseEntity<>(seller,HttpStatus.OK);
	}
	
	@GetMapping("/report")
	public ResponseEntity<SellerReport> getSellerReport(
			@RequestHeader("Authorization") String jwt
			){
		
		Seller seller = sellerService.getSellerProfile(jwt);
		
		SellerReport report = sellerReportService.getSellerReport(seller);
		return new ResponseEntity<>(report,HttpStatus.OK);
	}
	
	
	
	@GetMapping
	public ResponseEntity<List<Seller>> getAllSellers(
			@RequestParam(required = false) AccountStatus status
			){
		
		List<Seller> sellers = sellerService.getAllSellers(status);
		
		return ResponseEntity.ok(sellers);
	}
	
	
	@PatchMapping()
	public ResponseEntity<Seller> updateSeller(@RequestHeader("Authorization") String jwt,@RequestBody Seller seller){
		
		Seller profile = sellerService.getSellerProfile(jwt);
		Seller updateSeller = sellerService.updateSeller(profile.getId(), seller);
		
		return ResponseEntity.ok(updateSeller);
		
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteSeller(@PathVariable Long id) throws SellerException{
		
		sellerService.deleteSeller(id);
		return ResponseEntity.noContent().build();
	}
	
	
	
	
	
	
	
	
	
	
	
	
}
