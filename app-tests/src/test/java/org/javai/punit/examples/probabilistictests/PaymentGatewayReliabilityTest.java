package org.javai.punit.examples.probabilistictests;

import org.javai.punit.api.UseCaseProvider;
import org.javai.punit.examples.sentinels.PaymentGatewayReliability;
import org.javai.punit.examples.usecases.PaymentGatewayUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * JUnit test for PaymentGatewayUseCase reliability. Inherits all probabilistic
 * test methods and the measure experiment from
 * {@link PaymentGatewayReliability @Sentinel}; supplies only the JUnit-native
 * use-case provider wiring.
 */
public class PaymentGatewayReliabilityTest extends PaymentGatewayReliability {

    @RegisterExtension
    UseCaseProvider provider = new UseCaseProvider();

    @BeforeEach
    void setUp() {
        provider.register(PaymentGatewayUseCase.class, PaymentGatewayUseCase::new);
    }
}
