package org.javai.punit.examples.app.shopping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.javai.outcome.Outcome;
import org.javai.punit.examples.app.llm.ChatResponse;

/**
 * Validates and parses LLM responses into {@link ShoppingAction} instances.
 *
 * <p>This validator attempts to deserialize JSON content into shopping actions,
 * capturing validation failures as {@link Outcome} results rather than exceptions.
 */
public class ShoppingActionValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * The result of validating an LLM response.
     *
     * @param actions the parsed actions (empty if validation failed)
     */
	public record BasketTranslation(List<ShoppingAction> actions) {
        static BasketTranslation of(List<ShoppingAction> actions) {
            return new BasketTranslation(List.copyOf(actions));
        }
    }

    /**
     * Parses and validates a chat response as shopping actions.
     *
     * <p>Expects the wrapped format: {@code {"actions": [{"context": "SHOP", ...}, ...]}}
     *
     * @param response the chat response containing JSON content
     * @return an outcome containing the validation result, or a failure with details
     */
    public static Outcome<BasketTranslation> validate(ChatResponse response) {
        return parse(response.content());
    }

    /**
     * Parses and validates a JSON string as shopping actions. Used by
     * the typed pipeline's contract clause when the use case's output
     * type is the raw LLM response — the contract's {@code deriving}
     * step delegates to this method.
     *
     * <p>Expects the wrapped format: {@code {"actions": [{"context": "SHOP", ...}, ...]}}
     *
     * @param json the JSON content to parse
     * @return an outcome containing the validation result, or a failure with details
     */
    public static Outcome<BasketTranslation> parse(String json) {
        if (json == null || json.isBlank()) {
            return Outcome.fail("validation", "Response content is null or blank");
        }

        JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            return Outcome.fail("validation", "Invalid JSON: " + e.getMessage());
        }

        if (!root.isObject() || !root.has("actions")) {
            return Outcome.fail("validation", "Expected JSON object with 'actions' array");
        }

        JsonNode actionsNode = root.get("actions");
        if (!actionsNode.isArray()) {
            return Outcome.fail("validation", "Expected 'actions' to be an array");
        }

        return parseActionArray(actionsNode);
    }

    private static Outcome<BasketTranslation> parseActionArray(JsonNode arrayNode) {
        List<ShoppingAction> actions = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        int index = 0;
        for (JsonNode node : arrayNode) {
            try {
                ShoppingAction action = MAPPER.treeToValue(node, ShoppingAction.class);
                actions.add(action);
            } catch (JsonProcessingException e) {
                errors.add("Action[%d]: %s".formatted(index, e.getMessage()));
            } catch (IllegalArgumentException e) {
                errors.add("Action[%d]: %s".formatted(index, e.getMessage()));
            }
            index++;
        }

        if (!errors.isEmpty()) {
            return Outcome.fail("validation", String.join("; ", errors));
        }

        if (actions.isEmpty()) {
            return Outcome.fail("validation", "Empty actions array");
        }

        return Outcome.ok(BasketTranslation.of(actions));
    }
}
