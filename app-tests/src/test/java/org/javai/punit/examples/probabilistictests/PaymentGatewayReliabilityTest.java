package org.javai.punit.examples.probabilistictests;

import org.javai.punit.api.UseCaseProvider;
import org.javai.punit.examples.sentinels.PaymentGatewayReliability;
import org.javai.punit.examples.usecases.PaymentGatewayUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * JUnit test for PaymentGatewayUseCase reliability, derived from the
 * {@link PaymentGatewayReliability @Sentinel} specification.
 *
 * <p>Test methods ({@code testFunctionalCorrectness}, {@code testLatency},
 * {@code testCombinedReliability}) and the measure experiment are all inherited
 * from the Sentinel spec. This class provides only the JUnit-native setup via
 * {@code @RegisterExtension} and {@code @BeforeEach}, making it runnable on
 * developer workstations and in CI.
 *
 * <p>This is the <b>minimal JUnit adapter</b> pattern: a single line of
 * inheritance is sufficient to bridge a Sentinel reliability specification
 * into the JUnit ecosystem.
 *
 * @see PaymentGatewayReliability
 */
public class PaymentGatewayReliabilityTest extends PaymentGatewayReliability {

    @RegisterExtension
    UseCaseProvider provider = new UseCaseProvider();

    @BeforeEach
    void setUp() {
        provider.register(PaymentGatewayUseCase.class, PaymentGatewayUseCase::new);
    }
}
