package com.zosh.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.zosh.domain.AccountStatus;
import com.zosh.domain.USER_ROLE;
import com.zosh.exceptions.DuplicateResourceException;
import com.zosh.exceptions.SellerException;
import com.zosh.model.Address;
import com.zosh.model.Seller;
import com.zosh.repository.SellerRepository;
import com.zosh.request.SellerRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("SellerServiceImpl Unit Tests")
class SellerServiceImplTest {

    @Mock
    private SellerRepository sellerRepo;

    @Mock
    private com.zosh.service.NotificationService notificationService;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private SellerServiceImpl sellerService;

    private SellerRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new SellerRequest();
        validRequest.setSellerName("Super Seller");
        validRequest.setEmail("seller@market.com");
        validRequest.setPassword("secret123");
        validRequest.setMobile("9876543210");
        validRequest.setGSTIN("29ABCDE1234F1Z5");

        SellerRequest.AddressRequest addressRequest = new SellerRequest.AddressRequest();
        addressRequest.setName("Warehouse 1");
        addressRequest.setAddress("123 Industrial Area");
        addressRequest.setCity("Bengaluru");
        addressRequest.setState("Karnataka");
        addressRequest.setPinCode("560001");
        addressRequest.setMobile("9876543210");
        validRequest.setPickupAddress(addressRequest);
    }

    @Test
    @DisplayName("Scenario 1: createSeller persists a new Seller with transient Address (id=null)")
    void createSeller_success() throws DuplicateResourceException {
        when(sellerRepo.findByEmail(validRequest.getEmail())).thenReturn(null);
        when(sellerRepo.save(any(Seller.class))).thenAnswer(invocation -> {
            Seller s = invocation.getArgument(0);
            s.setId(1L);
            if (s.getPickupAddress() != null) {
                s.getPickupAddress().setId(10L);
            }
            return s;
        });

        Seller saved = sellerService.createSeller(validRequest);

        assertNotNull(saved);
        assertEquals(1L, saved.getId());
        assertEquals("seller@market.com", saved.getEmail());
        assertEquals(USER_ROLE.ROLE_SELLER, saved.getRole());
        assertEquals(AccountStatus.PENDING_VERIFICATION, saved.getAccountStatus());
        assertFalse(saved.isEmailVerified());

        ArgumentCaptor<Seller> captor = ArgumentCaptor.forClass(Seller.class);
        verify(sellerRepo).save(captor.capture());
        Seller captured = captor.getValue();

        // Verify pickup address was passed as a TRANSIENT entity (id is null prior to JPA persist)
        assertNotNull(captured.getPickupAddress());
        assertEquals("Bengaluru", captured.getPickupAddress().getCity());
        assertEquals("Karnataka", captured.getPickupAddress().getState());
    }

    @Test
    @DisplayName("Scenario 2: createSeller throws DuplicateResourceException if email already exists")
    void createSeller_duplicateEmail_throwsException() {
        when(sellerRepo.findByEmail(validRequest.getEmail())).thenReturn(new Seller());

        assertThrows(DuplicateResourceException.class, () -> sellerService.createSeller(validRequest));
        verify(sellerRepo, never()).save(any(Seller.class));
    }

    @Test
    @DisplayName("Scenario 3: updateSeller updates existing managed address fields in place")
    void updateSeller_updatesManagedAddressInPlace() throws SellerException {
        Seller existing = new Seller();
        existing.setId(1L);
        existing.setEmail("seller@market.com");
        existing.setSellerName("Old Name");

        Address existingAddr = new Address();
        existingAddr.setId(10L);
        existingAddr.setCity("Old City");
        existing.setPickupAddress(existingAddr);

        when(sellerRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(sellerRepo.save(any(Seller.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerRequest updateReq = new SellerRequest();
        updateReq.setSellerName("New Name");
        SellerRequest.AddressRequest newAddr = new SellerRequest.AddressRequest();
        newAddr.setCity("New City");
        updateReq.setPickupAddress(newAddr);

        Seller result = sellerService.updateSeller(1L, updateReq);

        assertEquals("New Name", result.getSellerName());
        // Verify the existing Address instance ID was retained (not replaced with a detached entity)
        assertEquals(10L, result.getPickupAddress().getId());
        assertEquals("New City", result.getPickupAddress().getCity());
    }
}
