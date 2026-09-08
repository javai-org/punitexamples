package org.mavai.punit.examples.lm;

import java.util.LinkedHashMap;
import java.util.Map;

import org.mavai.punit.lm.api.LanguageModel;
import org.mavai.punit.lm.api.LanguageModels;

/**
 * The mock-or-real switch for every language-model call the examples
 * make.
 *
 * <p>The mode is read from the {@code punit.llm.mode} system property,
 * then the {@code PUNIT_LLM_MODE} environment variable, and defaults to
 * {@code mock}, so a fresh clone runs every experiment and test offline
 * with no keys. In {@code real} mode the model comes from punit-lm's
 * public API: the same configuration keys a services file carries, the
 * same validation, and the same credential tier ({@code MAVAI_LLM_API_KEY},
 * then the vendor's own variable — {@code OPENAI_API_KEY} or
 * {@code ANTHROPIC_API_KEY}). The provider follows the model name, as
 * the examples' old router did: {@code claude-*} goes to Anthropic,
 * everything else to OpenAI.
 */
public final class LanguageModelMode {

    static final String MODE_PROPERTY = "punit.llm.mode";
    static final String MODE_ENV_VAR = "PUNIT_LLM_MODE";
    static final String DEFAULT_MODE = "mock";

    private LanguageModelMode() {
    }

    /**
     * A language model for the given tuning under the resolved mode.
     *
     * @throws IllegalArgumentException for a mode other than {@code mock} or {@code real}
     * @throws org.mavai.punit.decl.ContractConfigurationException when punit-lm refuses the configuration in real mode
     */
    public static LanguageModel resolve(String model, double temperature, String systemPrompt) {
        String mode = resolvedMode();
        return switch (mode.toLowerCase()) {
            case "mock" -> new MockLanguageModel(model, temperature, systemPrompt);
            case "real" -> LanguageModels.configure(configuration(model, temperature, systemPrompt));
            default -> throw new IllegalArgumentException(
                    "Unknown LLM mode: '%s'. Supported: mock, real".formatted(mode));
        };
    }

    /** The declarative configuration map a real-mode model is built from. */
    static Map<String, Object> configuration(String model, double temperature, String systemPrompt) {
        Map<String, Object> configuration = new LinkedHashMap<>();
        configuration.put("provider", providerFor(model));
        configuration.put("model", model);
        configuration.put("temperature", temperature);
        configuration.put("system-prompt", systemPrompt);
        return configuration;
    }

    /** The provider a model name routes to. */
    static String providerFor(String model) {
        return model != null && model.toLowerCase().startsWith("claude") ? "anthropic" : "openai";
    }

    public static String resolvedMode() {
        String value = System.getProperty(MODE_PROPERTY);
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = System.getenv(MODE_ENV_VAR);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return DEFAULT_MODE;
    }

    public static boolean isMock() {
        return "mock".equalsIgnoreCase(resolvedMode());
    }

    public static boolean isReal() {
        return "real".equalsIgnoreCase(resolvedMode());
    }
}
