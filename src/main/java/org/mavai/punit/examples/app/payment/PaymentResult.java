package org.mavai.punit.examples.app.payment;

/**
 * Result of a payment gateway transaction.
 *
 * @param paymentSucceeded whether the transaction succeeded
 * @param transactionId unique identifier for the transaction (null on failure)
 * @param errorCode error code if failed (null on paymentSucceeded)
 */
public record PaymentResult(
        boolean paymentSucceeded,
        String transactionId,
        String errorCode
) {

    /**
     * Creates a successful payment result.
     *
     * @param transactionId the transaction identifier
     * @return a successful result
     */
    public static PaymentResult success(String transactionId) {
        return new PaymentResult(true, transactionId, null);
    }

    /**
     * Creates a failed payment result.
     *
     * @param errorCode the error code indicating failure reason
     * @return a failed result
     */
    public static PaymentResult failure(String errorCode) {
        return new PaymentResult(false, null, errorCode);
    }
}
