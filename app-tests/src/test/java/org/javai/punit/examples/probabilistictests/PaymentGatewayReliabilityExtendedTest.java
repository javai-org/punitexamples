package org.javai.punit.examples.probabilistictests;

import static org.assertj.core.api.Assertions.assertThat;

import org.javai.punit.api.legacy.ProbabilisticTest;
import org.javai.punit.api.TestIntent;
import org.javai.punit.api.ThresholdOrigin;
import org.javai.punit.api.UseCaseProvider;
import org.javai.punit.examples.app.payment.MockPaymentGateway;
import org.javai.punit.examples.sentinels.PaymentGatewayReliability;
import org.javai.punit.examples.usecases.PaymentGatewayUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Extends a {@code @Sentinel} reliability specification
 * ({@link PaymentGatewayReliability}) with JUnit-only additions:
 * {@code @DisplayName}, {@code @Tag}, and supplementary tests
 * (probabilistic and standard) that share the same use case provider.
 */
@DisplayName("Payment Gateway Reliability Suite")
@Tag("reliability")
@Tag("sla")
public class PaymentGatewayReliabilityExtendedTest extends PaymentGatewayReliability {

    @RegisterExtension
    UseCaseProvider provider = new UseCaseProvider();

    @BeforeEach
    void setUp() {
        provider.register(PaymentGatewayUseCase.class, PaymentGatewayUseCase::new);
    }

    @ProbabilisticTest(
            useCase = PaymentGatewayUseCase.class,
            samples = 50,
            minPassRate = 0.99,
            intent = TestIntent.SMOKE,
            thresholdOrigin = ThresholdOrigin.SLA,
            contractRef = "Payment Provider SLA v2.3, Section 4.1"
    )
    @DisplayName("Smoke test with alternate card token")
    void smokeTestAlternateCard(PaymentGatewayUseCase useCase) {
        useCase.chargeCard("tok_mastercard_5555", 4999L).assertAll();
    }

    @Test
    @DisplayName("Mock gateway is a consistent singleton")
    void mockGatewaySingleton() {
        assertThat(MockPaymentGateway.instance())
                .isSameAs(MockPaymentGateway.instance());
    }
}
