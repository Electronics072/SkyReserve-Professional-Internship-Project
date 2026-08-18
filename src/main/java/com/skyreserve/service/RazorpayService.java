package com.skyreserve.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorpayService {
    private final String keyId;
    private final String keySecret;

    public RazorpayService(
            @Value("${razorpay.key.id:}") String keyId,
            @Value("${razorpay.key.secret:}") String keySecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    public String getKeyId() {
        return keyId;
    }

    public Order createOrder(double amountInRupees, String receipt) throws Exception {
        if (keyId.isBlank() || keySecret.isBlank()) {
            throw new IllegalStateException("Razorpay test keys are not configured on the server.");
        }

        RazorpayClient client = new RazorpayClient(keyId, keySecret);
        JSONObject request = new JSONObject();
        request.put("amount", Math.round(amountInRupees * 100));
        request.put("currency", "INR");
        request.put("receipt", receipt);
        request.put("payment_capture", 1);

        // razorpay-java exposes the order client as the lowercase `orders` field.
        return client.orders.create(request);
    }

    public boolean verify(String orderId, String paymentId, String signature) throws Exception {
        if (keySecret.isBlank()) {
            return false;
        }

        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", orderId);
        options.put("razorpay_payment_id", paymentId);
        options.put("razorpay_signature", signature);
        return Utils.verifyPaymentSignature(options, keySecret);
    }
}
