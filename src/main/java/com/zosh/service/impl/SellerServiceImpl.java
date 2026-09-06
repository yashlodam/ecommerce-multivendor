package com.zosh.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zosh.config.JwtProvider;
import com.zosh.domain.AccountStatus;
import com.zosh.domain.NotificationType;
import com.zosh.domain.USER_ROLE;
import com.zosh.exceptions.DuplicateResourceException;
import com.zosh.exceptions.ResourceNotFoundException;
import com.zosh.exceptions.SellerException;
import com.zosh.model.Address;
import com.zosh.model.BankDetails;
import com.zosh.model.BusinessDetails;
import com.zosh.model.Seller;
import com.zosh.repository.OrderRepository;
import com.zosh.repository.ProductRepository;
import com.zosh.repository.SellerRepository;
import com.zosh.request.SellerRequest;
import com.zosh.service.NotificationService;
import com.zosh.service.SellerService;

@Service
public class SellerServiceImpl implements SellerService {

    private static final Logger log = LoggerFactory.getLogger(SellerServiceImpl.class);

    @Autowired
    private SellerRepository sellerepo;

    @Autowired
    private JwtProvider jwtprovider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private NotificationService notificationService;

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Seller getSellerProfile(String jwt) {
        String email = jwtprovider.getEmailFromJwtToken(jwt);
        return this.getSellerByEmail(email);
    }

    @Override
    public Seller getSellerById(Long id) throws SellerException {
        return sellerepo.findById(id)
                .orElseThrow(() -> new SellerException("Seller not found with id: " + id));
    }

    @Override
    public Seller getSellerByEmail(String email) {
        Seller seller = sellerepo.findByEmail(email);
        if (seller == null) {
            throw new ResourceNotFoundException("Seller", "email", email);
        }
        return seller;
    }

    @Override
    public List<Seller> getAllSellers(AccountStatus status) {
        if (status != null) {
            return sellerepo.findByAccountStatus(status);
        }
        return sellerepo.findAll();
    }

    @Override
    public List<Seller> getAllSellers() {
        return sellerepo.findAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE — accepts SellerRequest DTO, never a JPA entity from the client
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new Seller from a validated DTO.
     *
     * Root cause of the old bug:
     *   The old code accepted a raw Seller JPA entity from the client JSON.
     *   If the client included "pickupAddress": {"id": 123}, Jackson set address.id=123,
     *   making Hibernate consider it a DETACHED entity.
     *   Calling addressrepo.save(detachedAddress) then triggered:
     *     "Detached entity passed to persist: com.zosh.model.Address"
     *
     * Fix:
     *   Accept SellerRequest DTO (no 'id' field on AddressRequest).
     *   Build a brand-new Address entity in the service — id is always null → TRANSIENT → persist works.
     *   Seller owns Address lifecycle via CascadeType.ALL + orphanRemoval=true.
     *   We no longer need addressrepo.save() separately — cascading from sellerrepo.save() handles it.
     */
    @Override
    @Transactional
    public Seller createSeller(SellerRequest req) throws DuplicateResourceException {

        // 1. Duplicate check
        if (sellerepo.findByEmail(req.getEmail()) != null) {
            throw new DuplicateResourceException("A seller account already exists for email: " + req.getEmail());
        }

        // 2. Build a brand-new Address from the DTO — id is NEVER taken from client
        Address pickupAddress = null;
        if (req.getPickupAddress() != null) {
            pickupAddress = new Address();                                   // always transient
            pickupAddress.setName(req.getPickupAddress().getName());
            pickupAddress.setLocality(req.getPickupAddress().getLocality());
            pickupAddress.setAddress(req.getPickupAddress().getAddress());
            pickupAddress.setCity(req.getPickupAddress().getCity());
            pickupAddress.setState(req.getPickupAddress().getState());
            pickupAddress.setPinCode(req.getPickupAddress().getPinCode());
            pickupAddress.setMobile(req.getPickupAddress().getMobile());
            // id is null → Hibernate sees it as TRANSIENT → persist() works correctly
        }

        // 3. Build BusinessDetails (embedded — no entity lifecycle issue)
        BusinessDetails businessDetails = new BusinessDetails();
        if (req.getBusinessDetails() != null) {
            businessDetails.setBusinessName(req.getBusinessDetails().getBusinessName());
            businessDetails.setBusinessEmail(req.getBusinessDetails().getBusinessEmail());
            businessDetails.setBusinessMobile(req.getBusinessDetails().getBusinessMobile());
            businessDetails.setBusinessAddress(req.getBusinessDetails().getBusinessAddress());
            businessDetails.setLogo(req.getBusinessDetails().getLogo());
            businessDetails.setBanner(req.getBusinessDetails().getBanner());
        }

        // 4. Build BankDetails (embedded — no entity lifecycle issue)
        BankDetails bankDetails = new BankDetails();
        if (req.getBankDetails() != null) {
            bankDetails.setAccountHolderName(req.getBankDetails().getAccountHolderName());
            bankDetails.setAccountNumber(req.getBankDetails().getAccountNumber());
            bankDetails.setIfscCode(req.getBankDetails().getIfscCode());
        }

        // 5. Build the new Seller — backend controls role, status, verification
        Seller newSeller = new Seller();
        newSeller.setEmail(req.getEmail());
        newSeller.setPassword(passwordEncoder.encode(req.getPassword()));
        newSeller.setSellerName(req.getSellerName());
        newSeller.setMobile(req.getMobile());
        newSeller.setGSTIN(req.getGSTIN());
        newSeller.setPickupAddress(pickupAddress);       // transient Address — cascade will persist it
        newSeller.setBusinesssDetails(businessDetails);
        newSeller.setBankDetails(bankDetails);

        // Backend-controlled fields — never from client:
        newSeller.setRole(USER_ROLE.ROLE_SELLER);
        newSeller.setEmailVerified(false);
        newSeller.setAccountStatus(AccountStatus.PENDING_VERIFICATION);

        // 6. Save: CascadeType.ALL on pickupAddress → Hibernate persists Address automatically
        Seller saved = sellerepo.save(newSeller);
        log.info("Seller created: id={}, email={}", saved.getId(), saved.getEmail());

        // Broadcast to Admins
        notificationService.broadcastToAdmins(
            NotificationType.NEW_SELLER,
            "New Seller Registration",
            "New vendor \"" + saved.getSellerName() + "\" (" + saved.getEmail() + ") has registered and requires account review.",
            String.valueOf(saved.getId()),
            "SELLER",
            "/admin/sellers"
        );

        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE — updates fields on the MANAGED entity loaded from DB
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates a seller's own profile.
     *
     * Key design: we always load the existing Seller from the DB first (managed entity).
     * We then update individual fields from the request.
     * For pickupAddress: we update fields on the EXISTING managed Address entity
     * (existingSeller.getPickupAddress()) rather than replacing the reference.
     * This avoids orphan address rows and detached-entity issues on update.
     */
    @Override
    @Transactional
    public Seller updateSeller(Long id, SellerRequest req) throws SellerException {

        Seller existingSeller = sellerepo.findById(id)
                .orElseThrow(() -> new SellerException("Seller not found with id: " + id));

        if (req.getSellerName() != null) {
            existingSeller.setSellerName(req.getSellerName());
        }
        if (req.getMobile() != null) {
            existingSeller.setMobile(req.getMobile());
        }
        // Do NOT allow email change via update (would break auth)

        if (req.getGSTIN() != null) {
            existingSeller.setGSTIN(req.getGSTIN());
        }

        // Update embedded BusinessDetails in place
        if (req.getBusinessDetails() != null) {
            SellerRequest.BusinessDetailsRequest bd = req.getBusinessDetails();
            if (bd.getBusinessName() != null)
                existingSeller.getBusinesssDetails().setBusinessName(bd.getBusinessName());
            if (bd.getBusinessEmail() != null)
                existingSeller.getBusinesssDetails().setBusinessEmail(bd.getBusinessEmail());
            if (bd.getBusinessMobile() != null)
                existingSeller.getBusinesssDetails().setBusinessMobile(bd.getBusinessMobile());
            if (bd.getBusinessAddress() != null)
                existingSeller.getBusinesssDetails().setBusinessAddress(bd.getBusinessAddress());
            if (bd.getLogo() != null)
                existingSeller.getBusinesssDetails().setLogo(bd.getLogo());
            if (bd.getBanner() != null)
                existingSeller.getBusinesssDetails().setBanner(bd.getBanner());
        }

        // Update embedded BankDetails in place
        if (req.getBankDetails() != null) {
            SellerRequest.BankDetailsRequest bk = req.getBankDetails();
            if (bk.getAccountHolderName() != null)
                existingSeller.getBankDetails().setAccountHolderName(bk.getAccountHolderName());
            if (bk.getAccountNumber() != null)
                existingSeller.getBankDetails().setAccountNumber(bk.getAccountNumber());
            if (bk.getIfscCode() != null)
                existingSeller.getBankDetails().setIfscCode(bk.getIfscCode());
        }

        // Update Address fields on the EXISTING managed Address entity
        // NEVER replace the address reference — that would leave an orphan row
        if (req.getPickupAddress() != null) {
            SellerRequest.AddressRequest ar = req.getPickupAddress();
            Address existingAddress = existingSeller.getPickupAddress();

            if (existingAddress == null) {
                // Seller somehow has no address yet — create a new transient one
                existingAddress = new Address();
                existingSeller.setPickupAddress(existingAddress);
            }

            if (ar.getName() != null)     existingAddress.setName(ar.getName());
            if (ar.getLocality() != null) existingAddress.setLocality(ar.getLocality());
            if (ar.getAddress() != null)  existingAddress.setAddress(ar.getAddress());
            if (ar.getCity() != null)     existingAddress.setCity(ar.getCity());
            if (ar.getState() != null)    existingAddress.setState(ar.getState());
            if (ar.getPinCode() != null)  existingAddress.setPinCode(ar.getPinCode());
            if (ar.getMobile() != null)   existingAddress.setMobile(ar.getMobile());
        }

        return sellerepo.save(existingSeller);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteSeller(Long id) throws SellerException {
        Seller seller = getSellerById(id);

        if (!orderRepository.findBySellerId(id).isEmpty()) {
            throw new SellerException(
                "Cannot delete seller with existing customer orders. Orders must be preserved for financial auditing. Please suspend or deactivate the account instead."
            );
        }

        if (!productRepository.findBySellerId(id).isEmpty()) {
            throw new SellerException(
                "Cannot delete seller with active products. Please delete or reassign all products before deleting this seller."
            );
        }

        sellerepo.delete(seller);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STATUS / VERIFICATION
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Seller verifyEmail(String email, String otp) {
        Seller seller = getSellerByEmail(email);
        seller.setEmailVerified(true);
        return sellerepo.save(seller);
    }

    @Override
    public Seller updateSellerAccountStatus(Long id, AccountStatus status) throws SellerException {
        Seller seller = getSellerById(id);
        seller.setAccountStatus(status);
        Seller saved = sellerepo.save(seller);

        notificationService.notifySeller(
            saved,
            NotificationType.SELLER_ACCOUNT_UPDATE,
            "Account Status Updated",
            "Your seller account status has been updated to: " + status + ".",
            String.valueOf(saved.getId()),
            "SELLER",
            "/seller"
        );

        return saved;
    }

    public void verifySellerIsActive(Seller seller) throws SellerException {
        if (seller.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new SellerException(
                "Your seller account is under verification. You cannot perform this action until your account is approved."
            );
        }
    }
}
