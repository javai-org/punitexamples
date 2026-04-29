/**
 * Example experiments demonstrating PUnit's MEASURE, EXPLORE, and OPTIMIZE modes.
 *
 * <ul>
 *   <li>{@link org.javai.punit.examples.experiments.ShoppingBasketMeasure} —
 *       MEASURE: establish a baseline.</li>
 *   <li>{@link org.javai.punit.examples.experiments.ShoppingBasketExplore} —
 *       EXPLORE: compare configurations across a grid.</li>
 *   <li>{@link org.javai.punit.examples.experiments.ShoppingBasketOptimizeTemperature} —
 *       OPTIMIZE: tune a numeric parameter (LLM temperature).</li>
 *   <li>{@link org.javai.punit.examples.experiments.ShoppingBasketOptimizePrompt} —
 *       OPTIMIZE: iteratively refine a system prompt with a meta-LLM.</li>
 * </ul>
 *
 * <h2>Running</h2>
 *
 * <pre>{@code
 * ./gradlew experiment -Prun=ShoppingBasketMeasure
 * ./gradlew experiment -Prun=ShoppingBasketExplore.compareModels
 * ./gradlew experiment -Prun=ShoppingBasketOptimizeTemperature.optimizeTemperature
 * ./gradlew experiment -Prun=ShoppingBasketOptimizePrompt.optimizeSystemPrompt
 * }</pre>
 */
package org.javai.punit.examples.experiments;
