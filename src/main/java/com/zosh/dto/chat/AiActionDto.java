package com.zosh.dto.chat;

public class AiActionDto {
    private String type;
    private String label;
    private Long productId;
    private Long variantId;
    private Long orderId;
    private String url;

    public AiActionDto() {}

    public AiActionDto(String type, String label) {
        this.type = type;
        this.label = label;
    }

    public static AiActionDto viewProduct(Long productId, String label) {
        AiActionDto a = new AiActionDto("VIEW_PRODUCT", label != null ? label : "View Details");
        a.setProductId(productId);
        return a;
    }

    public static AiActionDto addToCart(Long productId, Long variantId, String label) {
        AiActionDto a = new AiActionDto("ADD_TO_CART", label != null ? label : "Add to Cart");
        a.setProductId(productId);
        a.setVariantId(variantId);
        return a;
    }

    public static AiActionDto viewCart() {
        return new AiActionDto("VIEW_CART", "Go to Cart");
    }

    public static AiActionDto trackOrder(Long orderId) {
        AiActionDto a = new AiActionDto("TRACK_ORDER", "Track Order #" + orderId);
        a.setOrderId(orderId);
        return a;
    }

    public static AiActionDto navigateLogin() {
        return new AiActionDto("NAVIGATE_LOGIN", "Sign In to Continue");
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getVariantId() { return variantId; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
