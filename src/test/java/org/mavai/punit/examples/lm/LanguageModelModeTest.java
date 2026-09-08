package org.mavai.punit.examples.lm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mavai.punit.lm.api.LanguageModel;

@DisplayName("LanguageModelMode")
class LanguageModelModeTest {

    @BeforeEach
    @AfterEach
    void clearModeProperty() {
        System.clearProperty(LanguageModelMode.MODE_PROPERTY);
    }

    private static void assumeNoModeEnvVar() {
        String env = System.getenv(LanguageModelMode.MODE_ENV_VAR);
        assumeTrue(env == null || env.isBlank(), "PUNIT_LLM_MODE is set in this environment");
    }

    @Test
    @DisplayName("defaults to mock and resolves the examples' mock")
    void defaultsToMock() {
        assumeNoModeEnvVar();
        assertThat(LanguageModelMode.resolvedMode()).isEqualTo("mock");
        assertThat(LanguageModelMode.isMock()).isTrue();
        LanguageModel model = LanguageModelMode.resolve("gpt-4o-mini", 0.3, "Respond with JSON only.");
        assertThat(model).isInstanceOf(MockLanguageModel.class);
    }

    @Test
    @DisplayName("the system property selects the mode, case-insensitively")
    void propertySelectsMode() {
        System.setProperty(LanguageModelMode.MODE_PROPERTY, "REAL");
        assertThat(LanguageModelMode.isReal()).isTrue();
        assertThat(LanguageModelMode.isMock()).isFalse();
    }

    @Test
    @DisplayName("an unknown mode is refused by name")
    void unknownModeRefused() {
        System.setProperty(LanguageModelMode.MODE_PROPERTY, "sometimes");
        assertThatThrownBy(() -> LanguageModelMode.resolve("gpt-4o-mini", 0.3, "prompt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sometimes");
    }

    @Test
    @DisplayName("real mode builds the declarative configuration punit-lm reads, provider from the model name")
    void realModeConfiguration() {
        assertThat(LanguageModelMode.configuration("gpt-4o-mini", 0.3, "prompt"))
                .containsEntry("provider", "openai")
                .containsEntry("model", "gpt-4o-mini")
                .containsEntry("temperature", 0.3)
                .containsEntry("system-prompt", "prompt");
        assertThat(LanguageModelMode.providerFor("claude-sonnet-4-5")).isEqualTo("anthropic");
        assertThat(LanguageModelMode.providerFor("o3-mini")).isEqualTo("openai");
    }
}
