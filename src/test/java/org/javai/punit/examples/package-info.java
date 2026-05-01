/**
 * Teaching examples for PUnit probabilistic testing.
 *
 * <ul>
 *   <li>{@code experiments/} — MEASURE, EXPLORE, and OPTIMIZE experiment examples.</li>
 *   <li>{@code probabilistictests/} — probabilistic tests covering threshold
 *       approaches, covariates, budgets, pacing, exception handling, and the
 *       deterministic CoinToss / RegionalCoinToss walkthroughs.</li>
 *   <li>{@code integration/} — end-to-end operational-flow checks.</li>
 * </ul>
 *
 * <h2>Running</h2>
 *
 * <pre>{@code
 * ./gradlew test --tests "ShoppingBasketTest"
 * ./gradlew experiment -Prun=ShoppingBasketMeasure
 * }</pre>
 */
package org.javai.punit.examples;
