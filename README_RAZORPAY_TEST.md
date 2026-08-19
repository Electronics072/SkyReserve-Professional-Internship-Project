# Razorpay Test Mode + Payment OTP

SkyReserve now supports an optional Razorpay Test Mode checkout with a SkyReserve email OTP gate before the Razorpay modal opens.

## Render environment variables

Required for Razorpay Test Mode:

- `RAZORPAY_KEY_ID` = Razorpay Test Mode Key ID (`rzp_test_...`)
- `RAZORPAY_KEY_SECRET` = Razorpay Test Mode Key Secret

Required for real email OTP delivery:

- `MAIL_HOST` = SMTP host (default `smtp.gmail.com`)
- `MAIL_PORT` = SMTP port (default `587`)
- `MAIL_USERNAME` = SMTP account email
- `MAIL_PASSWORD` = SMTP app password / SMTP password

If SMTP is not configured, SkyReserve remains usable in demo mode and the generated OTP is explicitly shown as a demo fallback on the payment page.

## Test flow

1. Sign in and choose a flight and seat.
2. Open checkout.
3. Click **Send OTP** and verify the six-digit OTP.
4. Click **Pay with Razorpay**.
5. Razorpay Test Checkout opens.
6. Use Razorpay's official Test Mode cards/UPI/bank test flows; no real money is charged.
7. SkyReserve verifies the Razorpay signature server-side before creating the booking.
