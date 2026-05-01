/**
 * Use cases for the worked examples
 * ({@link org.javai.punit.api.ProbabilisticTest @ProbabilisticTest}
 * + {@link org.javai.punit.api.Experiment @Experiment} +
 * {@link org.javai.punit.runtime.PUnit PUnit} factories).
 *
 * <p>{@link org.javai.punit.examples.usecases.CoinTossUseCase} and
 * {@link org.javai.punit.examples.usecases.RegionalCoinTossUseCase}
 * are deterministic stand-ins that let the worked examples
 * demonstrate the full measure / test / empirical-pair pattern
 * without external dependencies. {@link
 * org.javai.punit.examples.usecases.ShoppingBasketUseCase} and
 * {@link org.javai.punit.examples.usecases.PaymentGatewayUseCase}
 * are the LLM- and gateway-backed
 * use cases used by the realistic examples.
 */
package org.javai.punit.examples.usecases;
