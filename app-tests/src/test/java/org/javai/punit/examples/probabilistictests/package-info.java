/**
 * Probabilistic test examples for the typed PUnit authoring surface.
 *
 * <ul>
 *   <li>{@link org.javai.punit.examples.probabilistictests.ShoppingBasketTest}
 *       — empirical-pair baseline verification.</li>
 *   <li>{@link org.javai.punit.examples.probabilistictests.ShoppingBasketThresholdApproachesTest}
 *       — sample-size-first, confidence-first, threshold-first.</li>
 *   <li>{@link org.javai.punit.examples.probabilistictests.ShoppingBasketCovariateTest}
 *       — covariate-aware baseline matching.</li>
 *   <li>{@link org.javai.punit.examples.probabilistictests.ShoppingBasketBudgetTest}
 *       — wall-clock and token budgets.</li>
 *   <li>{@link org.javai.punit.examples.probabilistictests.ShoppingBasketPacingTest}
 *       — rate-limiting via {@code Pacing}.</li>
 *   <li>{@link org.javai.punit.examples.probabilistictests.ShoppingBasketExceptionTest}
 *       — exception-handling policies.</li>
 *   <li>{@link org.javai.punit.examples.probabilistictests.ShoppingBasketDiagnosticsTest}
 *       — diagnostic output for criterion explanations and misalignments.</li>
 *   <li>{@link org.javai.punit.examples.probabilistictests.PaymentGatewaySlaTest}
 *       — contractual SLA verification with {@code TestIntent}.</li>
 * </ul>
 *
 * <h2>Setup</h2>
 *
 * <p>Shopping-basket tests require a baseline. Run the corresponding
 * MEASURE experiment first.
 *
 * <h2>Running</h2>
 *
 * <pre>{@code
 * ./gradlew experiment -Prun=ShoppingBasketMeasure
 * ./gradlew test --tests "ShoppingBasketTest"
 * }</pre>
 */
package org.javai.punit.examples.probabilistictests;
