package com.zosh.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.zosh.domain.USER_ROLE;
import com.zosh.model.Seller;
import com.zosh.model.User;
import com.zosh.repository.SellerRepository;
import com.zosh.repository.UserRepository;

@Service
public class CustomeUserServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepo;
    
    @Autowired
    private SellerRepository sellerepo;

    private static final String SELLER_PREFIX = "seller_";

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        if (username.startsWith(SELLER_PREFIX)) {

            String actualUsername =
                    username.substring(SELLER_PREFIX.length());

            Seller seller = sellerepo.findByEmail(actualUsername);

            if (seller != null) {
                return buildUserDetails(
                        seller.getEmail(),
                        seller.getPassword(),
                        seller.getRole());
            }

            throw new UsernameNotFoundException(
                    "Seller not found with email: " + actualUsername);
        }

        User user = userRepo.findByEmail(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found with email: " + username));

        return buildUserDetails(
                user.getEmail(),
                user.getPassword(),
                user.getRole());
    }
    private UserDetails buildUserDetails(
            String email,
            String password,
            USER_ROLE role) {

        if (role == null) {
            role = USER_ROLE.ROLE_CUSTOMER;
        }

        List<GrantedAuthority> authorities = new ArrayList<>();

        authorities.add(
                new SimpleGrantedAuthority(role.name()));

        return org.springframework.security.core.userdetails.User
                .builder()
                .username(email)
                .password(password)
                .authorities(authorities)
                .build();
    }
}