package org.mavai.punit.examples.app.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ChatLlmProvider")
class ChatLlmProviderTest {

    @AfterEach
    void clearSystemProperties() {
        System.clearProperty("punit.llm.mode");
    }

    /**
     * Skip the test when the {@code PUNIT_LLM_MODE} environment
     * variable is set. The "default mode" assertions can only be
     * validated when neither the system property nor the env var is
     * present; the system property is cleared in {@link #clearSystemProperties()},
     * but env vars cannot be mutated from Java without a third-party
     * library, so a developer who has exported {@code PUNIT_LLM_MODE=real}
     * for experiment runs would see these tests fail misleadingly.
     */
    private static void assumeNoModeEnvVar() {
        assumeTrue(System.getenv("PUNIT_LLM_MODE") == null,
                "PUNIT_LLM_MODE env var is set; cannot validate default-mode behaviour");
    }

    @Nested
    @DisplayName("resolve()")
    class Resolve {

        @Test
        @DisplayName("returns MockChatLlm by default")
        void returnsMockChatLlmByDefault() {
            assumeNoModeEnvVar();
            ChatLlm llm = ChatLlmProvider.resolve();

            assertThat(llm).isInstanceOf(MockChatLlm.class);
        }

        @Test
        @DisplayName("returns MockChatLlm when mode is 'mock'")
        void returnsMockChatLlmWhenModeIsMock() {
            System.setProperty("punit.llm.mode", "mock");

            ChatLlm llm = ChatLlmProvider.resolve();

            assertThat(llm).isInstanceOf(MockChatLlm.class);
        }

        @Test
        @DisplayName("returns RoutingChatLlm when mode is 'real'")
        void returnsRoutingChatLlmWhenModeIsReal() {
            System.setProperty("punit.llm.mode", "real");

            ChatLlm llm = ChatLlmProvider.resolve();

            assertThat(llm).isInstanceOf(RoutingChatLlm.class);
        }

        @Test
        @DisplayName("mode is case-insensitive")
        void modeIsCaseInsensitive() {
            System.setProperty("punit.llm.mode", "REAL");

            ChatLlm llm = ChatLlmProvider.resolve();

            assertThat(llm).isInstanceOf(RoutingChatLlm.class);
        }

        @Test
        @DisplayName("throws for invalid mode")
        void throwsForInvalidMode() {
            System.setProperty("punit.llm.mode", "invalid");

            assertThatThrownBy(ChatLlmProvider::resolve)
                    .isInstanceOf(LlmConfigurationException.class)
                    .hasMessageContaining("Unknown LLM mode: 'invalid'")
                    .hasMessageContaining("mock, real");
        }
    }

    @Nested
    @DisplayName("resolvedMode()")
    class ResolvedMode {

        @Test
        @DisplayName("returns 'mock' by default")
        void returnsMockByDefault() {
            assumeNoModeEnvVar();
            assertThat(ChatLlmProvider.resolvedMode()).isEqualTo("mock");
        }

        @Test
        @DisplayName("returns system property value")
        void returnsSystemPropertyValue() {
            System.setProperty("punit.llm.mode", "real");

            assertThat(ChatLlmProvider.resolvedMode()).isEqualTo("real");
        }
    }

    @Nested
    @DisplayName("isRealMode()")
    class IsRealMode {

        @Test
        @DisplayName("returns false by default")
        void returnsFalseByDefault() {
            assumeNoModeEnvVar();
            assertThat(ChatLlmProvider.isRealMode()).isFalse();
        }

        @Test
        @DisplayName("returns true when mode is 'real'")
        void returnsTrueWhenModeIsReal() {
            System.setProperty("punit.llm.mode", "real");

            assertThat(ChatLlmProvider.isRealMode()).isTrue();
        }

        @Test
        @DisplayName("is case-insensitive")
        void isCaseInsensitive() {
            System.setProperty("punit.llm.mode", "REAL");

            assertThat(ChatLlmProvider.isRealMode()).isTrue();
        }
    }

    @Nested
    @DisplayName("isMockMode()")
    class IsMockMode {

        @Test
        @DisplayName("returns true by default")
        void returnsTrueByDefault() {
            assumeNoModeEnvVar();
            assertThat(ChatLlmProvider.isMockMode()).isTrue();
        }

        @Test
        @DisplayName("returns false when mode is 'real'")
        void returnsFalseWhenModeIsReal() {
            System.setProperty("punit.llm.mode", "real");

            assertThat(ChatLlmProvider.isMockMode()).isFalse();
        }
    }
}
