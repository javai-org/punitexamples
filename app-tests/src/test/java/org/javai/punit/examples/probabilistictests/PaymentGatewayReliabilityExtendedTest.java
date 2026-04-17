package org.javai.punit.examples.probabilistictests;

import static org.assertj.core.api.Assertions.assertThat;

import org.javai.punit.api.ProbabilisticTest;
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
 * Extended JUnit test for PaymentGatewayUseCase reliability, demonstrating
 * that JUnit-specific concerns layer on top of the {@link PaymentGatewayReliability
 * @Sentinel} specification without modifying it.
 *
 * <p>This class inherits all test and measure methods from the Sentinel spec
 * and adds:
 * <ul>
 *   <li>{@code @DisplayName} for human-readable test names in reports</li>
 *   <li>{@code @Tag} for filtering and categorization</li>
 *   <li>Additional JUnit-only tests that complement the reliability specification</li>
 * </ul>
 *
 * <p>This pattern shows that JUnit concerns are additive — the reliability
 * specification remains the single source of truth, and the JUnit subclass
 * enriches it with framework-specific metadata and supplementary assertions.
 *
 * @see PaymentGatewayReliability
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

    // =========================================================================
    // ADDITIONAL JUNIT-ONLY TESTS
    // =========================================================================

    /**
     * Verifies gateway consistency with a higher sample count.
     *
     * <p>This test is specific to the JUnit test suite — it uses a different
     * card token and amount to exercise a broader input surface than the
     * Sentinel spec defines. The reliability specification captures the
     * canonical verification; this test extends coverage for CI.
     */
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

    /**
     * Non-probabilistic sanity check: verifies the mock gateway is a singleton.
     *
     * <p>This is a standard JUnit test (not probabilistic), demonstrating that
     * JUnit subclasses can freely mix probabilistic and deterministic tests.
     */
    @Test
    @DisplayName("Mock gateway is a consistent singleton")
    void mockGatewaySingleton() {
        assertThat(MockPaymentGateway.instance())
                .isSameAs(MockPaymentGateway.instance());
    }
}
