package com.skyreserve.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorpayService {

    private final RazorpayClient client;
    private final String keyId;
    private final String keySecret;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public RazorpayService(
            @Value("${razorpay.key.id}") String keyId,
            @Value("${razorpay.key.secret}") String keySecret) {

        this.keyId = keyId;
        this.keySecret = keySecret;

        if (keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException(
                    "Razorpay Key ID is missing"
            );
        }

        if (keySecret == null || keySecret.isBlank()) {
            throw new IllegalArgumentException(
                    "Razorpay Key Secret is missing"
            );
        }

        try {

            this.client = new RazorpayClient(
                    keyId,
                    keySecret
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to initialize Razorpay client",
                    e
            );
        }
    }


    // =========================================================
    // CREATE RAZORPAY ORDER
    // =========================================================

    public String createOrder(
            double amount,
            String receipt) {

        try {

            // -------------------------------------------------
            // Convert INR to paise
            // -------------------------------------------------

            int amountInPaise =
                    (int) Math.round(amount * 100);


            // -------------------------------------------------
            // Validate amount
            // -------------------------------------------------

            if (amountInPaise < 100) {

                throw new IllegalArgumentException(
                        "Payment amount must be at least ₹1.00"
                );
            }


            // -------------------------------------------------
            // Generate receipt if missing
            // -------------------------------------------------

            if (receipt == null ||
                    receipt.trim().isEmpty()) {

                receipt =
                        "SKYRESERVE_"
                                + System.currentTimeMillis();
            }


            // =================================================
            // CREATE ORDER REQUEST
            // =================================================

            JSONObject orderRequest =
                    new JSONObject();


            /*
             * Use explicit Object values.
             *
             * This avoids JSONObject.put() overload problems
             * with the Razorpay / org.json version used by
             * this project.
             */

            orderRequest.put(
                    "amount",
                    (Object) Integer.valueOf(amountInPaise)
            );

            orderRequest.put(
                    "currency",
                    (Object) "INR"
            );

            orderRequest.put(
                    "receipt",
                    (Object) receipt
            );


            // =================================================
            // CREATE RAZORPAY ORDER
            // =================================================

            Order order =
                    client.orders.create(
                            orderRequest
                    );


            // =================================================
            // PREPARE RESPONSE
            // =================================================

            JSONObject response =
                    new JSONObject();


            /*
             * IMPORTANT:
             *
             * Do NOT use:
             *
             * String.valueOf(order.get("id"))
             *
             * because the JSONObject/Razorpay API combination
             * in this project can cause:
             *
             * String cannot be cast to [C
             *
             * Instead, retrieve the value as Object first.
             */


            // -------------------------------------------------
            // Order ID
            // -------------------------------------------------

            Object orderId =
                    order.get("id");

            response.put(
                    "id",
                    (Object) orderId
            );


            // -------------------------------------------------
            // Order Amount
            // -------------------------------------------------

            Object orderAmount =
                    order.get("amount");

            response.put(
                    "amount",
                    (Object) orderAmount
            );


            // -------------------------------------------------
            // Currency
            // -------------------------------------------------

            Object orderCurrency =
                    order.get("currency");

            response.put(
                    "currency",
                    (Object) orderCurrency
            );


            // -------------------------------------------------
            // Razorpay Key ID
            // -------------------------------------------------

            response.put(
                    "keyId",
                    (Object) keyId
            );


            // -------------------------------------------------
            // Receipt
            // -------------------------------------------------

            response.put(
                    "receipt",
                    (Object) receipt
            );


            // -------------------------------------------------
            // Return JSON response
            // -------------------------------------------------

            return response.toString();


        } catch (Exception e) {

            System.err.println(
                    "========================================"
            );

            System.err.println(
                    "RAZORPAY ORDER CREATION FAILED"
            );

            System.err.println(
                    "========================================"
            );

            System.err.println(
                    "Amount: " + amount
            );

            System.err.println(
                    "Receipt: " + receipt
            );

            System.err.println(
                    "Error: " + e.getMessage()
            );

            e.printStackTrace();


            throw new RuntimeException(
                    "Unable to create Razorpay order",
                    e
            );
        }
    }


    // =========================================================
    // VERIFY RAZORPAY PAYMENT SIGNATURE
    // =========================================================

    public boolean verifyPaymentSignature(
            String orderId,
            String paymentId,
            String signature) {

        try {

            // -------------------------------------------------
            // Validate values
            // -------------------------------------------------

            if (orderId == null ||
                    paymentId == null ||
                    signature == null) {

                return false;
            }


            if (orderId.isBlank() ||
                    paymentId.isBlank() ||
                    signature.isBlank()) {

                return false;
            }


            // =================================================
            // CREATE VERIFICATION ATTRIBUTES
            // =================================================

            JSONObject attributes =
                    new JSONObject();


            attributes.put(
                    "razorpay_order_id",
                    (Object) orderId
            );


            attributes.put(
                    "razorpay_payment_id",
                    (Object) paymentId
            );


            attributes.put(
                    "razorpay_signature",
                    (Object) signature
            );


            // =================================================
            // VERIFY SIGNATURE
            // =================================================

            boolean verified =
                    Utils.verifyPaymentSignature(
                            attributes,
                            keySecret
                    );


            // =================================================
            // LOG RESULT
            // =================================================

            if (verified) {

                System.out.println(
                        "========================================"
                );

                System.out.println(
                        "RAZORPAY PAYMENT VERIFIED"
                );

                System.out.println(
                        "Order ID: " + orderId
                );

                System.out.println(
                        "Payment ID: " + paymentId
                );

                System.out.println(
                        "========================================"
                );

            } else {

                System.out.println(
                        "========================================"
                );

                System.out.println(
                        "RAZORPAY PAYMENT VERIFICATION FAILED"
                );

                System.out.println(
                        "Order ID: " + orderId
                );

                System.out.println(
                        "Payment ID: " + paymentId
                );

                System.out.println(
                        "========================================"
                );
            }


            return verified;


        } catch (Exception e) {

            System.err.println(
                    "Razorpay signature verification error:"
            );

            System.err.println(
                    e.getMessage()
            );

            e.printStackTrace();

            return false;
        }
    }
}