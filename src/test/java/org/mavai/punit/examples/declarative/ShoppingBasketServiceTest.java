package org.mavai.punit.examples.declarative;

import org.mavai.punit.api.ProbabilisticTest;
import org.mavai.punit.runtime.PUnit;

/**
 * The pure-services declarative path: the service is a language model
 * declared entirely in {@code mavai-services.yaml} — no bindings
 * class, no Java beyond this one-line test. The bundled
 * {@link StubLanguageModel} stands in for the model, so the example
 * runs offline through the real punit-lm wire path (and its reported
 * token counts reach the artefact cost blocks).
 */
class ShoppingBasketServiceTest {

    @ProbabilisticTest
    void shoppingBasketServiceBuildsValidActions() {
        try (StubLanguageModel stub = StubLanguageModel.start().install()) {
            PUnit.declared().assertPasses();
        }
    }
}
