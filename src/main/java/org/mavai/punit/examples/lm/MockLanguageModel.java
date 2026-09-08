package org.mavai.punit.examples.lm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.mavai.outcome.Outcome;
import org.mavai.punit.lm.api.LanguageModel;
import org.mavai.punit.lm.api.LmReply;

/**
 * The examples' mock language model, on punit-lm's public
 * {@link LanguageModel} interface: one configured model per factor
 * bundle, answering the shopping-basket prompts offline with a
 * temperature-dependent chance of the failures a real model exhibits.
 *
 * <h2>Temperature-based reliability</h2>
 * <p>Per-aspect deviation chance is {@code temperature² * 0.1}; the
 * joint failure rate over the ~five independent paths is roughly 0% at
 * 0.0, ~1% at 0.3, ~3-4% at 0.5 and ~15% at 1.0.
 *
 * <h2>Failure modes</h2>
 * <p>Malformed JSON (fences, prose, refusals), a hallucinated schema
 * ({@code operations} instead of {@code actions}), invalid values (a
 * string quantity), and invalid actions.
 *
 * <h2>Token usage</h2>
 * <p>Estimated at ~1.3 tokens per word and stated on every reply, so
 * cost accounting works exactly as it does against a real model.
 *
 * <h2>Identity</h2>
 * <p>The mock states its own covariates ({@code serviceType},
 * {@code provider}, {@code model}, {@code temperature},
 * {@code systemPrompt}), so a baseline measured in mock mode never
 * silently matches a test run against a real provider.
 */
// mavai-ref: JVI-EMJBHE4 — do not remove (resolves in mavai-orchestrator)
public final class MockLanguageModel implements LanguageModel {

    /** Approximate tokens per word (GPT-style tokenization). */
    private static final double TOKENS_PER_WORD = 1.3;

    /** One deviation source for every mock in the run, seedable for reproducible runs. */
    private static final Random RANDOM = new Random(System.currentTimeMillis());

    private final String model;
    private final double temperature;
    private final String systemPrompt;
    private final PromptRequirements requirements;

    public MockLanguageModel(String model, double temperature, String systemPrompt) {
        this.model = model;
        this.temperature = temperature;
        this.systemPrompt = systemPrompt;
        this.requirements = analyzePromptRequirements(systemPrompt);
    }

    /** Reseeds the shared deviation source for a reproducible run. */
    public static void seed(long seed) {
        synchronized (RANDOM) {
            RANDOM.setSeed(seed);
        }
    }

    @Override
    public Outcome<LmReply> invoke(Object input) {
        String userMessage = String.valueOf(input);
        String response;
        synchronized (RANDOM) {
            response = generateResponse(userMessage, requirements, temperature);
        }
        int promptTokens = estimateTokens(systemPrompt) + estimateTokens(userMessage);
        int completionTokens = estimateTokens(response);
        return Outcome.ok(LmReply.of(response, promptTokens, completionTokens));
    }

    @Override
    public Map<String, String> configurationCovariates() {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("serviceType", "mock-language-model");
        entries.put("provider", "mock");
        entries.put("model", model);
        entries.put("temperature", Double.toString(temperature));
        entries.put("systemPrompt", systemPrompt);
        return entries;
    }

    private static int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        String[] words = text.trim().split("\\s+");
        return (int) Math.ceil(words.length * TOKENS_PER_WORD);
    }

    /** What the system prompt explicitly requires — this decides the response's quality. */
    private static PromptRequirements analyzePromptRequirements(String systemPrompt) {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            return new PromptRequirements(false, false, false, false, false);
        }
        String lower = systemPrompt.toLowerCase();
        boolean requiresJsonOnly = lower.contains("only") && lower.contains("json")
                || lower.contains("no explanation") || lower.contains("no markdown");
        boolean specifiesSchema = lower.contains("operations")
                && (lower.contains("{") || lower.contains("structure") || lower.contains("format"));
        boolean specifiesFields = lower.contains("action") && lower.contains("item") && lower.contains("quantity");
        boolean specifiesActions = (lower.contains("\"add\"") && lower.contains("\"remove\""))
                || (lower.contains("add") && lower.contains("remove") && lower.contains("clear")
                        && (lower.contains("must be") || lower.contains("one of")));
        boolean specifiesConstraints = lower.contains("positive") || lower.contains("integer")
                || lower.contains("≥") || lower.contains(">= 1");
        return new PromptRequirements(
                requiresJsonOnly, specifiesSchema, specifiesFields, specifiesActions, specifiesConstraints);
    }

    /**
     * A response shaped by what the prompt asks for and by the
     * temperature: per-aspect deviation chance {@code temperature² * 0.1}.
     */
    private static String generateResponse(String userMessage, PromptRequirements req, double temperature) {
        double deviationChance = temperature * temperature * 0.1;
        StringBuilder response = new StringBuilder();

        boolean addProse = !req.requiresJsonOnly && RANDOM.nextDouble() < deviationChance;
        if (addProse) {
            response.append("I'd be happy to help! Here's the JSON:\n\n");
        }

        boolean deviateActions = !req.specifiesActions || RANDOM.nextDouble() < deviationChance;
        String actionValue = deviateActions ? randomAction() : "add";

        boolean deviateQuantity = !req.specifiesConstraints || RANDOM.nextDouble() < deviationChance;
        Object quantityValue = deviateQuantity ? randomQuantity() : extractQuantity(userMessage, "add");

        String item = extractItem(userMessage, "add");
        if (item.equals("item")) item = "apple";

        boolean deviateSchema = RANDOM.nextDouble() < deviationChance;
        if (deviateSchema) {
            // Wrong schema, but parseable JSON: the failure should be a
            // downstream type or schema mismatch, not a parse error.
            String quantityJson = quantityValue instanceof Number
                    ? quantityValue.toString()
                    : "\"" + quantityValue + "\"";
            response.append(String.format("{\"operations\": [{\"action\": \"%s\", ", actionValue));
            response.append(String.format("\"item\": \"%s\", ", item));
            response.append(String.format("\"quantity\": %s}]}", quantityJson));
        } else {
            response.append("{\"actions\": [{\"context\": \"SHOP\", ");
            response.append(String.format("\"name\": \"%s\", ", actionValue));
            response.append("\"parameters\": [");
            response.append(String.format("{\"name\": \"item\", \"value\": \"%s\"}, ", item));
            response.append(String.format("{\"name\": \"quantity\", \"value\": \"%s\"}", quantityValue));
            response.append("]}]}");
        }

        if (RANDOM.nextDouble() < deviationChance * 0.3) {
            return unparseableResponse(response.toString(), userMessage);
        }
        return response.toString();
    }

    private static String randomAction() {
        if (RANDOM.nextDouble() < 0.7) {
            String[] validOptions = {"add", "remove", "clear"};
            return validOptions[RANDOM.nextInt(validOptions.length)];
        }
        String[] invalidOptions = {"purchase", "buy", "insert", "delete"};
        return invalidOptions[RANDOM.nextInt(invalidOptions.length)];
    }

    private static Object randomQuantity() {
        return switch (RANDOM.nextInt(5)) {
            case 0 -> -1;
            case 1 -> 0;
            case 2 -> "two";
            case 3 -> 2;
            default -> 1;
        };
    }

    /** Fences, trailing prose or a refusal: what a model does when it ignores a JSON-only instruction. */
    private static String unparseableResponse(String json, String userMessage) {
        return switch (RANDOM.nextInt(3)) {
            case 0 -> "```json\n" + json + "\n```";
            case 1 -> "Sure thing! The JSON for your request is:\n\n" + json;
            default -> "I'm sorry, but I can't complete that request as stated. "
                    + "Could you clarify what you'd like to do with \"" + userMessage + "\"?";
        };
    }

    private static int extractQuantity(String message, String action) {
        String lower = message.toLowerCase();
        int actionIdx = lower.indexOf(action);
        if (actionIdx == -1) return 1;
        String window = message.substring(Math.max(0, actionIdx - 5),
                Math.min(message.length(), actionIdx + 30));
        for (String word : window.split("\\s+")) {
            try {
                int num = Integer.parseInt(word);
                if (num > 0 && num < 1000) return num;
            } catch (NumberFormatException ignored) {
                if (word.equalsIgnoreCase("one") || word.equals("a") || word.equals("an")) return 1;
                if (word.equalsIgnoreCase("two")) return 2;
                if (word.equalsIgnoreCase("three")) return 3;
            }
        }
        return 1;
    }

    private static String extractItem(String message, String action) {
        String lower = message.toLowerCase();
        int actionIdx = lower.indexOf(action);
        if (actionIdx == -1) return "item";
        String afterAction = message.substring(actionIdx + action.length()).trim();
        afterAction = afterAction.replaceFirst("^\\d+\\s*", "");
        String[] words = afterAction.split("\\s+");
        if (words.length == 0) return "item";
        StringBuilder item = new StringBuilder();
        for (String word : words) {
            String clean = word.replaceAll("[^a-zA-Z]", "").toLowerCase();
            if (clean.isEmpty()) continue;
            if (clean.equals("and") || clean.equals("the") || clean.equals("from")) break;
            if (item.length() > 0) item.append(" ");
            item.append(clean);
            if (item.length() > 20) break;
        }
        return item.length() > 0 ? item.toString() : "item";
    }

    /** What the system prompt explicitly specifies about the expected response. */
    private record PromptRequirements(
            boolean requiresJsonOnly,
            boolean specifiesSchema,
            boolean specifiesFields,
            boolean specifiesActions,
            boolean specifiesConstraints
    ) {}
}
