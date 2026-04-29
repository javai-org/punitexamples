/**
 * Teaching examples for PUnit probabilistic testing.
 *
 * <ul>
 *   <li>{@code experiments/} — MEASURE, EXPLORE, and OPTIMIZE experiment examples.</li>
 *   <li>{@code probabilistictests/} — probabilistic tests covering threshold
 *       approaches, covariates, budgets, pacing, and exception handling.</li>
 *   <li>{@code typed/} — typed authoring surface (CoinToss, RegionalCoinToss).</li>
 *   <li>{@code integration/} — end-to-end operational-flow checks.</li>
 *   <li>{@code selfdocument/} — generates the verdict catalogue used in the
 *       project documentation.</li>
 *   <li>{@code app/} — supporting domain code (LLM clients, etc.).</li>
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
