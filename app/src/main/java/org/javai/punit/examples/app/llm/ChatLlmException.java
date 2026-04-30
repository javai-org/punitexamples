package org.javai.punit.examples.app.llm;

/**
 * Checked exception thrown by {@link ChatLlm} implementations when an
 * anticipated LLM transport-level failure occurs — HTTP error,
 * timeout, malformed response, network fault.
 *
 * <p>Checked rather than unchecked because these are failures the
 * caller is expected to handle: transient network errors are part of
 * the contract of any remote service, and a probabilistic-test author
 * decides whether to count them as a sample failure (translate to
 * {@code Outcome.fail("llm-error", ...)}) or to retry. The standard
 * Java idiom for "fault-prone external resource may fail in
 * anticipated ways" is a checked exception, mirroring
 * {@link java.io.IOException} and {@link java.sql.SQLException}.
 *
 * <p>Defects — missing API keys, unknown model names, invalid
 * configuration — are signalled separately via
 * {@link LlmConfigurationException}, which remains unchecked because
 * these indicate caller mistakes, not anticipated failure.
 */
public class ChatLlmException extends Exception {

    public ChatLlmException(String message) {
        super(message);
    }

    public ChatLlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
