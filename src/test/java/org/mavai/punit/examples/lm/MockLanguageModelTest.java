package org.mavai.punit.examples.lm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mavai.outcome.Outcome;
import org.mavai.punit.examples.servicecontracts.ShoppingBasketServiceContract;
import org.mavai.punit.lm.api.LmReply;

@DisplayName("MockLanguageModel")
class MockLanguageModelTest {

    private static final String PROMPT = ShoppingBasketServiceContract.DEFAULT_SYSTEM_PROMPT;

    @Test
    @DisplayName("at temperature 0 every reply is the well-formed actions JSON")
    void temperatureZeroIsFaithful() {
        MockLanguageModel model = new MockLanguageModel("gpt-4o-mini", 0.0, PROMPT);
        for (int i = 0; i < 200; i++) {
            Outcome<LmReply> reply = model.invoke("Add 2 apples");
            assertThat(reply.isOk()).isTrue();
            assertThat(reply.getOrThrow().text()).startsWith("{\"actions\": [{\"context\": \"SHOP\"");
        }
    }

    @Test
    @DisplayName("every reply states its token usage")
    void repliesCarryUsage() {
        LmReply reply = new MockLanguageModel("gpt-4o-mini", 0.3, PROMPT).invoke("Add 2 apples").getOrThrow();
        assertThat(reply.usage()).isPresent();
        assertThat(reply.usage().get().inputTokens()).isPositive();
        assertThat(reply.usage().get().outputTokens()).isPositive();
    }

    @Test
    @DisplayName("states covariates that distinguish a mock run from a real one")
    void statesItsOwnIdentity() {
        assertThat(new MockLanguageModel("gpt-4o-mini", 0.3, PROMPT).configurationCovariates())
                .containsEntry("serviceType", "mock-language-model")
                .containsEntry("provider", "mock")
                .containsEntry("model", "gpt-4o-mini")
                .containsEntry("temperature", "0.3")
                .containsEntry("systemPrompt", PROMPT);
    }

    @Test
    @DisplayName("a high temperature produces the modelled failures")
    void highTemperatureDeviates() {
        MockLanguageModel model = new MockLanguageModel("gpt-4o-mini", 1.0, PROMPT);
        long deviations = 0;
        for (int i = 0; i < 400; i++) {
            String text = model.invoke("Add 2 apples").getOrThrow().text();
            if (!text.startsWith("{\"actions\": [{\"context\": \"SHOP\", \"name\": \"add\"")) deviations++;
        }
        assertThat(deviations).isPositive();
    }
}
