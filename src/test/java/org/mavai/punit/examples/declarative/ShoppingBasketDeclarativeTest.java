package org.mavai.punit.examples.declarative;

import org.mavai.punit.api.ProbabilisticTest;
import org.mavai.punit.runtime.PUnit;

/**
 * The declarative newcomer path, end to end: the contract lives in
 * {@code shopping-basket.yaml} beside this class, the service call in
 * {@link MavaiBindings}, and the test is one line. The method name
 * kebab-cases and resolves against the file's {@code contract:} key;
 * the run sizes itself to the smallest sample count the declared
 * threshold can support.
 *
 * <p>Authoring loop: {@code ./gradlew mavaiCheck} validates every
 * contract file with zero samples; when the file runs out of room,
 * {@code ./gradlew mavaiMaterialise} emits the equivalent
 * {@code ServiceContract} class to graduate to — see the punit user
 * guide's declarative part.
 */
class ShoppingBasketDeclarativeTest {

    @ProbabilisticTest
    void shoppingBasketBuildsValidActions() {
        // The budget is the invocation's: 100 samples gives the 0.85
        // bar comfortable evidence against the simulated LLM's ~1%
        // failure rate (the derived minimum would be legal but leaves
        // a single unlucky sample able to sink the bound).
        PUnit.declared().samples(100).assertPasses();
    }
}
