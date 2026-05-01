/**
 * Use cases for the worked examples
 * ({@link org.javai.punit.api.ProbabilisticTest @ProbabilisticTest}
 * + {@link org.javai.punit.api.Experiment @Experiment} +
 * {@link org.javai.punit.runtime.PUnit PUnit} factories).
 *
 * <p>{@link CoinTossUseCase} and {@link RegionalCoinTossUseCase}
 * are deterministic stand-ins that let the worked examples
 * demonstrate the full measure / test / empirical-pair pattern
 * without external dependencies. {@link ShoppingBasketUseCase} and
 * {@link PaymentGatewayUseCase} are the LLM- and gateway-backed
 * use cases used by the realistic examples.
 */
package org.javai.punit.examples.usecases;
