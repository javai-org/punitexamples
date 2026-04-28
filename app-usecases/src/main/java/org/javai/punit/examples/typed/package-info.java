/**
 * Worked example using the typed-compositional authoring API
 * ({@link org.javai.punit.api.ProbabilisticTest @ProbabilisticTest}
 * + {@link org.javai.punit.api.Experiment @Experiment} +
 * {@link org.javai.punit.junit5.Punit Punit} factories).
 *
 * <p>Self-contained — does not depend on the legacy
 * {@code ShoppingBasketUseCase} or {@code PaymentGatewayUseCase}
 * domain code. The {@link CoinTossUseCase} here is a deterministic
 * stand-in that lets the example demonstrate the full pattern
 * (measure / probabilistic test / empirical pair) without external
 * dependencies (LLM, payment gateway).
 *
 * <p>The {@link org.javai.punit.examples.sentinels} package next
 * door holds the legacy-annotation reliability specs; those will
 * migrate to the typed API as Stage 8 retires the legacy machinery.
 */
package org.javai.punit.examples.typed;
