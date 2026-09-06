package com.zosh.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zosh.domain.OrderStatus;
import com.zosh.domain.PaymentStatus;
import com.zosh.exceptions.InsufficientInventoryException;
import com.zosh.exceptions.InvalidOrderStateException;
import com.zosh.exceptions.ResourceNotFoundException;
import com.zosh.model.Address;
import com.zosh.model.Cart;
import com.zosh.model.CartItem;
import com.zosh.model.Order;
import com.zosh.model.OrderItem;
import com.zosh.model.Product;
import com.zosh.model.ProductVariant;
import com.zosh.model.User;
import com.zosh.domain.NotificationType;
import com.zosh.dto.pricing.ProductPricingDto;
import com.zosh.repository.AddressRepository;
import com.zosh.repository.DealRepository;
import com.zosh.repository.OrderItemRepository;
import com.zosh.repository.OrderRepository;
import com.zosh.repository.ProductRepository;
import com.zosh.repository.ProductVariantRepository;
import com.zosh.repository.SellerRepository;
import com.zosh.repository.UserRepository;
import com.zosh.service.NotificationService;
import com.zosh.service.OrderService;
import com.zosh.service.PricingService;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired private OrderRepository orderRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private PricingService pricingService;
    @Autowired private DealRepository dealRepository;
    @Autowired private SellerRepository sellerRepository;
    @Autowired private NotificationService notificationService;

    /**
     * Creates one Order per vendor from the cart contents.
     *
     * This method is @Transactional — all DB operations succeed or roll back together.
     * Inventory is deducted atomically using PESSIMISTIC_WRITE locks (SELECT FOR UPDATE)
     * to prevent overselling race conditions.
     *
     * @throws InsufficientInventoryException if any product has insufficient stock
     */
    @Override
    @Transactional
    public Set<Order> createOrder(User user, Address shippingAddress, Cart cart) {

        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot create an order with an empty cart.");
        }

        // Resolve shipping address
        Address address;
        if (shippingAddress.getId() != null) {
            address = addressRepository.findById(shippingAddress.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Address", "id", shippingAddress.getId()));
        } else {
            address = addressRepository.save(shippingAddress);
            user.getAddresses().add(address);
            userRepository.save(user);
        }

        // ============================================================
        // STEP 1: Validate and deduct inventory with pessimistic locks
        // If CartItem has a variant → lock + deduct variant stock.
        // Otherwise fall back to product-level stock (legacy behavior).
        // ============================================================
        for (CartItem item : cart.getCartItems()) {
            Long productId = item.getProduct().getId();

            if (item.getVariant() != null) {
                // ── Variant-aware path ──────────────────────────────
                Long variantId = item.getVariant().getId();
                ProductVariant variant = variantRepository.findByIdWithLock(variantId)
                        .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", variantId));

                if (variant.getQuantity() < item.getQuantity()) {
                    throw new InsufficientInventoryException(
                            productId, item.getQuantity(), variant.getQuantity());
                }

                variant.setQuantity(variant.getQuantity() - item.getQuantity());
                variantRepository.save(variant);

                // Keep flat product.quantity in sync
                Product product = productRepository.findByIdWithLock(productId)
                        .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
                int totalVariantStock = variantRepository.findByProduct(product).stream()
                        .mapToInt(v -> v.getQuantity() != null ? v.getQuantity() : 0)
                        .sum();
                product.setQuantity(totalVariantStock);
                productRepository.save(product);

                log.debug("Variant inventory deducted: variant={} qty={} remaining={}",
                        variantId, item.getQuantity(), variant.getQuantity());

                if (variant.getQuantity() <= 5 && product.getSeller() != null) {
                    notificationService.notifySeller(
                        product.getSeller(),
                        NotificationType.LOW_STOCK,
                        "Low Stock Alert",
                        "Variant (" + (variant.getVariantName() != null ? variant.getVariantName() : "Default") + ") for \"" + product.getTitle() + "\" has only " + variant.getQuantity() + " unit(s) left!",
                        String.valueOf(product.getId()),
                        "PRODUCT",
                        "/seller/products"
                    );
                }

            } else {
                // ── Legacy path — no variant ────────────────────────
                Product product = productRepository.findByIdWithLock(productId)
                        .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

                if (product.getQuantity() < item.getQuantity()) {
                    throw new InsufficientInventoryException(
                            productId, item.getQuantity(), product.getQuantity());
                }

                product.setQuantity(product.getQuantity() - item.getQuantity());
                productRepository.save(product);

                log.debug("Product inventory deducted: product={} qty={} remaining={}",
                        productId, item.getQuantity(), product.getQuantity());

                if (product.getQuantity() <= 5 && product.getSeller() != null) {
                    notificationService.notifySeller(
                        product.getSeller(),
                        NotificationType.LOW_STOCK,
                        "Low Stock Alert",
                        "Product \"" + product.getTitle() + "\" has only " + product.getQuantity() + " unit(s) left in stock!",
                        String.valueOf(product.getId()),
                        "PRODUCT",
                        "/seller/products"
                    );
                }
            }
        }

        // ============================================================
        // STEP 2: Group cart items by seller and create one Order per seller
        // ============================================================
        Map<Long, List<CartItem>> itemsBySeller = cart.getCartItems().stream()
                .collect(Collectors.groupingBy(item -> item.getProduct().getSeller().getId()));

        Set<Order> orders = new HashSet<>();
        Set<Long> appliedDealIds = new HashSet<>();

        for (Map.Entry<Long, List<CartItem>> entry : itemsBySeller.entrySet()) {
            Long sellerId = entry.getKey();
            List<CartItem> items = entry.getValue();

            Order order = new Order();
            order.setUser(user);
            order.setSellerId(sellerId);
            order.setShippingAddress(address);
            order.setOrderStatus(OrderStatus.PENDING);
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setOrderId("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

            Order savedOrder = orderRepository.save(order);

            int totalMrpPrice = 0;
            int totalSellingPrice = 0;
            int totalItems = 0;

            List<OrderItem> orderItems = new ArrayList<>();
            for (CartItem item : items) {
                // Re-evaluate live deal pricing for authoritative checkout integrity
                ProductPricingDto pricing = pricingService.calculateProductPricing(item.getProduct(), item.getVariant());

                int unitBasePrice = pricing.getBasePrice();
                int unitMrp = pricing.getMrpPrice();
                int unitFinalPrice = pricing.getEffectivePrice();
                int unitDiscount = pricing.getDiscountAmount();
                int qty = item.getQuantity() > 0 ? item.getQuantity() : 1;

                int itemTotalMrp = unitMrp * qty;
                int itemTotalSelling = unitFinalPrice * qty;

                totalMrpPrice += itemTotalMrp;
                totalSellingPrice += itemTotalSelling;
                totalItems += qty;

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(savedOrder);
                orderItem.setProduct(item.getProduct());
                orderItem.setVariant(item.getVariant());
                orderItem.setQuantity(qty);
                orderItem.setSize(item.getSize());
                orderItem.setMrpPrice(itemTotalMrp);
                orderItem.setSellingPrice(itemTotalSelling);

                // Immutable historical snapshot
                orderItem.setBasePrice(unitBasePrice);
                orderItem.setDiscountAmount(unitDiscount);
                orderItem.setFinalUnitPrice(unitFinalPrice);
                orderItem.setAppliedDealId(pricing.getAppliedDealId());
                orderItem.setAppliedDealTitle(pricing.getAppliedDealTitle());
                orderItem.setUserId(user.getId());

                orderItems.add(orderItemRepository.save(orderItem));

                if (pricing.getAppliedDealId() != null) {
                    appliedDealIds.add(pricing.getAppliedDealId());
                }
            }

            savedOrder.setOrderItems(orderItems);
            savedOrder.setTotalMrpPrice(totalMrpPrice);
            savedOrder.setTotalSellingPrice(totalSellingPrice);
            savedOrder.setDiscount(totalMrpPrice - totalSellingPrice);
            savedOrder.setTotalItems(totalItems);

            savedOrder = orderRepository.save(savedOrder);
            orders.add(savedOrder);

            log.info("Order created: orderId={} seller={} amount={}",
                    savedOrder.getOrderId(), sellerId, totalSellingPrice);

            // Notify Customer
            notificationService.notifyUser(
                user,
                NotificationType.ORDER_PLACED,
                "Order Placed Successfully",
                "Your order #" + savedOrder.getOrderId() + " with " + totalItems + " item(s) has been placed for ₹" + totalSellingPrice + ".",
                savedOrder.getOrderId(),
                "ORDER",
                "/account/orders/" + savedOrder.getId()
            );

            // Notify Seller
            final Order orderForSeller = savedOrder;
            final int itemsCount = totalItems;
            final int sellingAmount = totalSellingPrice;
            sellerRepository.findById(sellerId).ifPresent(seller -> {
                notificationService.notifySeller(
                    seller,
                    NotificationType.NEW_ORDER,
                    "New Order Received",
                    "You received order #" + orderForSeller.getOrderId() + " with " + itemsCount + " item(s) worth ₹" + sellingAmount + ".",
                    orderForSeller.getOrderId(),
                    "ORDER",
                    "/seller/orders"
                );
            });

            // Broadcast to Admins
            notificationService.broadcastToAdmins(
                NotificationType.NEW_ORDER_ADMIN,
                "New Platform Order",
                "Order #" + savedOrder.getOrderId() + " placed by " + (user.getFullName() != null ? user.getFullName() : user.getEmail()) + " for ₹" + totalSellingPrice + ".",
                savedOrder.getOrderId(),
                "ORDER",
                "/admin/orders"
            );
        }

        // Safely increment usage count for applied deals
        for (Long dealId : appliedDealIds) {
            dealRepository.findById(dealId).ifPresent(deal -> {
                deal.setUsageCount((deal.getUsageCount() != null ? deal.getUsageCount() : 0) + 1);
                dealRepository.save(deal);
            });
        }

        return orders;
    }

    @Override
    public Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
    }

    @Override
    public List<Order> usersOrderHistory(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    public List<Order> sellersOrder(Long sellerId) {
        return orderRepository.findBySellerId(sellerId);
    }

    /**
     * Updates the order status. Validates the seller owns this order to prevent IDOR.
     */
    @Override
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus, Long sellerId) {
        Order order = findOrderById(orderId);

        // SECURITY: Verify the seller owns this order
        if (!order.getSellerId().equals(sellerId)) {
            throw new InvalidOrderStateException(
                    "You do not have permission to update this order.");
        }

        if (order.getOrderStatus() == newStatus) {
            return order;
        }

        // Validate state transitions
        validateStatusTransition(order.getOrderStatus(), newStatus);

        if (newStatus == OrderStatus.CANCELLED) {
            restoreInventory(order);
        }

        order.setOrderStatus(newStatus);
        Order saved = orderRepository.save(order);

        log.info("Order status updated: orderId={} from={} to={} by seller={}",
                orderId, order.getOrderStatus(), newStatus, sellerId);

        // Notify customer of order status update
        switch (newStatus) {
            case CONFIRMED -> notificationService.notifyUser(
                order.getUser(),
                NotificationType.ORDER_CONFIRMED,
                "Order Confirmed",
                "Your order #" + order.getOrderId() + " has been confirmed by the seller.",
                order.getOrderId(),
                "ORDER",
                "/account/orders/" + order.getId()
            );
            case SHIPPED -> notificationService.notifyUser(
                order.getUser(),
                NotificationType.ORDER_SHIPPED,
                "Order Shipped",
                "Your order #" + order.getOrderId() + " is on its way!",
                order.getOrderId(),
                "ORDER",
                "/account/orders/" + order.getId()
            );
            case DELIVERED -> notificationService.notifyUser(
                order.getUser(),
                NotificationType.ORDER_DELIVERED,
                "Order Delivered",
                "Your order #" + order.getOrderId() + " has been delivered. Thank you for shopping with us!",
                order.getOrderId(),
                "ORDER",
                "/account/orders/" + order.getId()
            );
            case CANCELLED -> notificationService.notifyUser(
                order.getUser(),
                NotificationType.ORDER_CANCELLED,
                "Order Cancelled",
                "Your order #" + order.getOrderId() + " was cancelled.",
                order.getOrderId(),
                "ORDER",
                "/account/orders/" + order.getId()
            );
            default -> {}
        }

        return saved;
    }

    /**
     * Legacy method for backward compat — admin use only (no seller ownership check).
     */
    @Override
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus orderStatus) {
        Order order = findOrderById(orderId);
        if (order.getOrderStatus() == orderStatus) {
            return order;
        }
        validateStatusTransition(order.getOrderStatus(), orderStatus);
        if (orderStatus == OrderStatus.CANCELLED) {
            restoreInventory(order);
        }
        order.setOrderStatus(orderStatus);
        Order saved = orderRepository.save(order);

        switch (orderStatus) {
            case CONFIRMED -> notificationService.notifyUser(
                order.getUser(),
                NotificationType.ORDER_CONFIRMED,
                "Order Confirmed",
                "Your order #" + order.getOrderId() + " has been confirmed.",
                order.getOrderId(),
                "ORDER",
                "/account/orders/" + order.getId()
            );
            case SHIPPED -> notificationService.notifyUser(
                order.getUser(),
                NotificationType.ORDER_SHIPPED,
                "Order Shipped",
                "Your order #" + order.getOrderId() + " is on its way!",
                order.getOrderId(),
                "ORDER",
                "/account/orders/" + order.getId()
            );
            case DELIVERED -> notificationService.notifyUser(
                order.getUser(),
                NotificationType.ORDER_DELIVERED,
                "Order Delivered",
                "Your order #" + order.getOrderId() + " has been delivered.",
                order.getOrderId(),
                "ORDER",
                "/account/orders/" + order.getId()
            );
            case CANCELLED -> notificationService.notifyUser(
                order.getUser(),
                NotificationType.ORDER_CANCELLED,
                "Order Cancelled",
                "Your order #" + order.getOrderId() + " was cancelled.",
                order.getOrderId(),
                "ORDER",
                "/account/orders/" + order.getId()
            );
            default -> {}
        }

        return saved;
    }

    @Override
    @Transactional
    public Order cancelOrder(Long orderId, User user) {
        Order order = findOrderById(orderId);

        // SECURITY: Verify the user owns this order
        if (!user.getId().equals(order.getUser().getId())) {
            throw new InvalidOrderStateException("You do not have permission to cancel this order.");
        }

        // Business rule: cannot cancel orders that are already shipped/delivered
        if (order.getOrderStatus() == OrderStatus.SHIPPED
                || order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException(
                    "Cannot cancel an order that has already been " + order.getOrderStatus().name().toLowerCase() + ".");
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException("Order is already cancelled.");
        }

        // Restore inventory on cancellation
        restoreInventory(order);

        order.setOrderStatus(OrderStatus.CANCELLED);
        log.info("Order cancelled: orderId={} by user={}", orderId, user.getId());

        Order saved = orderRepository.save(order);

        // Notify customer
        notificationService.notifyUser(
            user,
            NotificationType.ORDER_CANCELLED,
            "Order Cancelled",
            "You have cancelled order #" + order.getOrderId() + ".",
            order.getOrderId(),
            "ORDER",
            "/account/orders/" + order.getId()
        );

        // Notify seller
        sellerRepository.findById(order.getSellerId()).ifPresent(seller -> {
            notificationService.notifySeller(
                seller,
                NotificationType.ORDER_CANCELLED_SELLER,
                "Order Cancelled by Customer",
                "Order #" + order.getOrderId() + " was cancelled by the customer.",
                order.getOrderId(),
                "ORDER",
                "/seller/orders"
            );
        });

        return saved;
    }

    @Override
    public OrderItem getOrderById(Long id) {
        return orderItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem", "id", id));
    }

    // ---- Private Helpers ----

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        if (current == next) return;

        // Define allowed forward transitions
        boolean valid = switch (current) {
            case PENDING    -> next == OrderStatus.PLACED || next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case PLACED     -> next == OrderStatus.CONFIRMED || next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED;
            case CONFIRMED  -> next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED;
            case SHIPPED    -> next == OrderStatus.DELIVERED;
            case DELIVERED  -> false; // terminal state
            case CANCELLED  -> false; // terminal state
        };

        if (!valid) {
            throw new InvalidOrderStateException(
                    "Invalid order status transition: " + current + " → " + next);
        }
    }

    @Transactional
    protected void restoreInventory(Order order) {
        if (order.getOrderItems() == null) return;

        for (OrderItem item : order.getOrderItems()) {
            if (item == null || item.getQuantity() <= 0) continue;
            int qty = item.getQuantity();

            if (item.getVariant() != null && item.getVariant().getId() != null) {
                Long variantId = item.getVariant().getId();
                variantRepository.findByIdWithLock(variantId).ifPresent(variant -> {
                    int currentVarQty = variant.getQuantity() != null ? variant.getQuantity() : 0;
                    variant.setQuantity(currentVarQty + qty);
                    variantRepository.save(variant);

                    // Keep flat product.quantity in sync
                    if (item.getProduct() != null && item.getProduct().getId() != null) {
                        productRepository.findByIdWithLock(item.getProduct().getId()).ifPresent(product -> {
                            int totalVariantStock = variantRepository.findByProduct(product).stream()
                                    .mapToInt(v -> v.getQuantity() != null ? v.getQuantity() : 0)
                                    .sum();
                            product.setQuantity(totalVariantStock);
                            productRepository.save(product);
                            log.info("Variant and Product inventory restored: variant={} product={} qty={}",
                                    variantId, product.getId(), qty);
                        });
                    }
                });
            } else if (item.getProduct() != null && item.getProduct().getId() != null) {
                productRepository.findByIdWithLock(item.getProduct().getId()).ifPresent(product -> {
                    int current = product.getQuantity() != null ? product.getQuantity() : 0;
                    product.setQuantity(current + qty);
                    productRepository.save(product);
                    log.info("Product inventory restored: product={} qty={}", product.getId(), qty);
                });
            }
        }
    }
}
