package org.mavai.punit.examples.declarative;

import org.mavai.punit.api.Experiment;
import org.mavai.punit.runtime.PUnit;

/**
 * The pure-services example's experiments, both declared in
 * {@code mavai-services.yaml}: the exploration grid (a temperature
 * sweep over the language-model service) and a prompt-engineer
 * optimization. Run with {@code ./gradlew exp
 * -Prun=ShoppingBasketExperiments}. Artefacts land under
 * {@code build/punit/explorations} and {@code build/punit/optimizations};
 * render them with the shared {@code mavai} tool — since the stub
 * reports token usage, the cost cells carry "ms · tok".
 */
class ShoppingBasketExperiments {

    @Experiment
    void exploreTemperature() {
        try (StubLanguageModel stub = StubLanguageModel.start().install()) {
            PUnit.declared("shopping-basket-builds-valid-actions")
                    .samplesPerConfig(5)
                    .explore();
        }
    }

    @Experiment
    void optimizePrompt() {
        try (StubLanguageModel stub = StubLanguageModel.start().install()) {
            PUnit.declared("shopping-basket-builds-valid-actions")
                    .samplesPerIteration(5)
                    .optimize("tune-prompt");
        }
    }
}
