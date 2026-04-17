package org.javai.punit.examples.probabilistictests;

import org.javai.punit.api.UseCaseProvider;
import org.javai.punit.examples.sentinels.ShoppingBasketReliability;
import org.javai.punit.examples.usecases.ShoppingBasketUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * JUnit test for ShoppingBasketUseCase reliability, derived from the
 * {@link ShoppingBasketReliability @Sentinel} specification.
 *
 * <p>Test methods and input sources are inherited from the Sentinel spec.
 * This class provides the JUnit-native setup via {@code @RegisterExtension}
 * and {@code @BeforeEach}, making it runnable on developer workstations and in CI.
 *
 * @see ShoppingBasketReliability
 */
public class ShoppingBasketReliabilityTest extends ShoppingBasketReliability {

    @RegisterExtension
    UseCaseProvider provider = new UseCaseProvider();

    @BeforeEach
    void setUp() {
        provider.register(ShoppingBasketUseCase.class, ShoppingBasketUseCase::new);
    }
}
