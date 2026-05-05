package org.javai.punit.examples.experiments;

import java.util.List;
import java.util.Map;

import org.javai.punit.api.spec.FactorsStepper;
import org.javai.punit.api.spec.FactorsStepper.IterationResult;
import org.javai.punit.api.spec.FailureCount;
import org.javai.punit.api.spec.FailureExemplar;
import org.javai.punit.api.spec.NextFactor;
import org.javai.punit.examples.app.llm.ChatLlm;
import org.javai.punit.examples.app.llm.ChatLlmException;
import org.javai.punit.examples.app.llm.ChatLlmProvider;
import org.javai.punit.examples.usecases.ShoppingBasketUseCase.LlmTuning;

/**
 * Builds a {@link FactorsStepper} that uses a meta-LLM as a prompt
 * engineer. Each iteration sends the previous prompt, its score, and
 * a per-postcondition failure breakdown to the meta-LLM and treats
 * the response as the next system prompt.
 */
final class PromptEngineerStepper {

    private static final String META_LLM_MODEL = "gpt-4o";
    private static final double META_LLM_TEMPERATURE = 0.5;
    private static final int MAX_FAILURE_EXEMPLARS = 2;

    private static final String META_SYSTEM_PROMPT = """
            You are a prompt engineer. The user gives you a system prompt
            currently being used with an LLM-backed shopping-basket
            translation service, plus the success rate that prompt
            achieved on a JSON-translation task. Your job: propose an
            improved version of the prompt that addresses common LLM
            failure modes for structured-output tasks — vague schema,
            hallucinated action names, missing required fields, free-form
            commentary mixed in with JSON. Output only the new system
            prompt. No commentary, no preamble, no surrounding quotes.
            """;

    private PromptEngineerStepper() {}

    static FactorsStepper<LlmTuning> create() {
        ChatLlm metaLlm = ChatLlmProvider.resolve();
        return (current, history) -> {
            if (history.isEmpty()) {
                return NextFactor.stop();
            }
            IterationResult<LlmTuning> last = history.getLast();
            String suggested = askMetaLlm(metaLlm, buildUserMessage(last));
            return NextFactor.next(current.systemPrompt(suggested));
        };
    }

    private static String askMetaLlm(ChatLlm metaLlm, String userMessage) {
        try {
            return metaLlm.chat(
                    META_SYSTEM_PROMPT,
                    userMessage,
                    META_LLM_MODEL,
                    META_LLM_TEMPERATURE);
        } catch (ChatLlmException e) {
            // FactorsStepper is non-throwing; if the meta-LLM is
            // unreachable the optimize experiment cannot progress.
            // Bubble as runtime so the experiment surfaces the cause.
            throw new RuntimeException("Meta-LLM call failed: " + e.getMessage(), e);
        }
    }

    private static String buildUserMessage(IterationResult<LlmTuning> last) {
        return """
                Current system prompt:
                %s

                Success rate on the translation task: %.2f
                %s
                Suggest an improved version.
                """.formatted(
                        last.factors().systemPrompt(),
                        last.score(),
                        renderFailureSection(last.failuresByPostcondition()));
    }

    private static String renderFailureSection(Map<String, FailureCount> byClause) {
        if (byClause.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\nFailure breakdown:\n");
        for (var entry : sortedByCountDescending(byClause)) {
            appendClauseLine(sb, entry.getKey(), entry.getValue());
        }
        return sb.toString();
    }

    private static List<Map.Entry<String, FailureCount>> sortedByCountDescending(
            Map<String, FailureCount> byClause) {
        return byClause.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().count(), a.getValue().count()))
                .toList();
    }

    private static void appendClauseLine(StringBuilder sb, String clause, FailureCount bucket) {
        sb.append("- \"").append(clause).append("\" failed ")
          .append(bucket.count()).append(" times.");
        appendExemplars(sb, bucket.exemplars());
        sb.append('\n');
    }

    private static void appendExemplars(StringBuilder sb, List<FailureExemplar> exemplars) {
        if (exemplars.isEmpty()) {
            return;
        }
        sb.append(" Examples:");
        int limit = Math.min(MAX_FAILURE_EXEMPLARS, exemplars.size());
        for (FailureExemplar ex : exemplars.subList(0, limit)) {
            sb.append("\n    - input \"").append(ex.input())
              .append("\" → ").append(ex.reason());
        }
    }
}
