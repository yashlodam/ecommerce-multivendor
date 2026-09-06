package com.zosh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.exceptions.ResourceNotFoundException;
import com.zosh.model.Address;
import com.zosh.model.User;
import com.zosh.repository.AddressRepository;
import com.zosh.repository.UserRepository;
import com.zosh.request.UserUpdateRequest;
import com.zosh.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PutMapping;
import com.zosh.response.ApiResponse;

@RestController
@RequestMapping("/api")
@Tag(name = "Customer - Profile", description = "User profile and address management endpoints")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @GetMapping("/users/profile")
    @Operation(summary = "Get profile of authenticated customer")
    public ResponseEntity<User> getUserDetails(@RequestHeader("Authorization") String jwt) {
        User user = userService.findUserByJwtToken(jwt);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/users/add-address")
    @Operation(summary = "Add a delivery address to authenticated customer profile")
    public ResponseEntity<User> addAddress(
            @Valid @RequestBody Address address,
            @RequestHeader("Authorization") String jwt) {

        User user = userService.findUserByJwtToken(jwt);
        address.setId(null); // Ensure transient state — prevent detached entity error

        user.getAddresses().add(address);
        User savedUser = userRepository.save(user);

        return ResponseEntity.ok(savedUser);
    }

    @PutMapping("/users/address/{addressId}")
    @Operation(summary = "Update an existing delivery address in customer profile")
    public ResponseEntity<User> updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody Address updatedAddress,
            @RequestHeader("Authorization") String jwt) {

        User user = userService.findUserByJwtToken(jwt);

        Address existing = user.getAddresses().stream()
                .filter(addr -> addr.getId() != null && addr.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Address not found or does not belong to your account."));

        if (updatedAddress.getName() != null && !updatedAddress.getName().isBlank()) {
            existing.setName(updatedAddress.getName().trim());
        }
        if (updatedAddress.getMobile() != null && !updatedAddress.getMobile().isBlank()) {
            existing.setMobile(updatedAddress.getMobile().trim());
        }
        if (updatedAddress.getAddress() != null && !updatedAddress.getAddress().isBlank()) {
            existing.setAddress(updatedAddress.getAddress().trim());
        }
        if (updatedAddress.getLocality() != null && !updatedAddress.getLocality().isBlank()) {
            existing.setLocality(updatedAddress.getLocality().trim());
        }
        if (updatedAddress.getCity() != null && !updatedAddress.getCity().isBlank()) {
            existing.setCity(updatedAddress.getCity().trim());
        }
        if (updatedAddress.getState() != null && !updatedAddress.getState().isBlank()) {
            existing.setState(updatedAddress.getState().trim());
        }
        if (updatedAddress.getPinCode() != null && !updatedAddress.getPinCode().isBlank()) {
            existing.setPinCode(updatedAddress.getPinCode().trim());
        }

        addressRepository.save(existing);
        User savedUser = userRepository.save(user);

        return ResponseEntity.ok(savedUser);
    }

    @DeleteMapping("/users/address/{addressId}")
    @Operation(summary = "Delete an address from authenticated customer profile (with IDOR check)")
    public ResponseEntity<User> deleteAddress(
            @PathVariable Long addressId,
            @RequestHeader("Authorization") String jwt) {

        User user = userService.findUserByJwtToken(jwt);

        boolean removed = user.getAddresses().removeIf(addr -> addr.getId() != null && addr.getId().equals(addressId));

        if (!removed) {
            throw new AccessDeniedException("You do not have permission to delete this address or it does not exist.");
        }

        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(savedUser);
    }

    @PatchMapping("/users/profile/update")
    @Operation(summary = "Update customer profile information")
    public ResponseEntity<User> updateProfile(
            @Valid @RequestBody UserUpdateRequest request,
            @RequestHeader("Authorization") String jwt) {

        User user = userService.findUserByJwtToken(jwt);

        String fullName = request.getEffectiveFullName();
        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName);
        }

        String mobile = request.getEffectiveMobile();
        if (mobile != null && !mobile.isBlank()) {
            user.setMobile(mobile);
        }

        User updatedUser = userRepository.save(user);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/users/profile")
    @Operation(summary = "Delete or permanently deactivate authenticated customer account")
    public ResponseEntity<ApiResponse> deleteCustomerAccount(@RequestHeader("Authorization") String jwt) {
        User user = userService.findUserByJwtToken(jwt);
        userService.deleteCustomerAccount(user);
        ApiResponse response = new ApiResponse("Your account has been deleted successfully.", true);
        return ResponseEntity.ok(response);
    }
}
