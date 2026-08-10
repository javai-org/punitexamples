package org.mavai.punit.examples.declarative;

import org.mavai.punit.api.ProbabilisticTest;
import org.mavai.punit.runtime.PUnit;

/**
 * The declarative worked example, end to end: the claim lives in
 * {@code shopping-basket.yaml}, the service it names is declared in
 * {@code mavai-services.yaml} (a language-model type — no bindings
 * class, no Java beyond this one line), and the method name
 * kebab-cases to resolve the contract. The bundled
 * {@link StubLanguageModel} stands in for the model, so the example
 * runs offline through the real punit-lm wire path — token usage
 * included, which is why the artefact cost blocks carry totals.
 *
 * <p>Authoring loop: {@code ./gradlew mavaiCheck} validates the pair
 * with zero samples; when the file runs out of room,
 * {@code ./gradlew mavaiMaterialise} emits the equivalent
 * {@code ServiceContract} class to graduate to.
 */
class ShoppingBasketDeclarativeTest {

    @ProbabilisticTest
    void shoppingBasketBuildsValidActions() {
        try (StubLanguageModel stub = StubLanguageModel.start().install()) {
            PUnit.declared().assertPasses();
        }
    }
}
