package com.zosh.service.ai;

import org.springframework.stereotype.Service;

@Service
public class StorePolicyService {

    public String getReturnPolicy() {
        return "ShopSphere offers a 7-day hassle-free return and exchange policy. " +
               "Items must be unused, in their original packaging, with all tags intact. " +
               "Once the seller inspects the returned item, your refund is credited within 3-5 business days.";
    }

    public String getShippingPolicy() {
        return "ShopSphere provides Free Express Delivery on all orders above Rs.499. " +
               "Standard delivery takes 2 to 4 business days across India. " +
               "Real-time tracking is provided for every order as soon as it ships.";
    }

    public String getPaymentMethods() {
        return "We accept all major secure payment options: " +
               "1. Razorpay (UPI, Google Pay, PhonePe, Paytm, NetBanking, Credit and Debit Cards). " +
               "2. Cash on Delivery (COD) on eligible pin codes.";
    }

    public String getAuthenticityGuarantee() {
        return "Every product on ShopSphere is 100% authentic and sourced directly " +
               "from verified multi-vendor brands and official sellers. " +
               "All vendors undergo rigorous identity and business verification.";
    }

    public String getFAQ() {
        return "Frequently Asked Questions:\n" +
               "- How do I cancel an order? You can cancel from My Orders before it is shipped.\n" +
               "- How do I track delivery? Use Where is my order in chat or check Orders.\n" +
               "- Are there shipping fees? Free on orders above Rs.499 (Rs.40 below Rs.499).\n" +
               "- What is the return window? 7 days from the delivery date.";
    }
}
