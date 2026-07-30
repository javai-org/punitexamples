package org.mavai.punit.examples.declarative;

import org.mavai.outcome.Outcome;
import org.mavai.punit.decl.Binding;
import org.mavai.punit.examples.app.llm.ChatLlm;
import org.mavai.punit.examples.app.llm.ChatLlmException;
import org.mavai.punit.examples.app.llm.ChatLlmProvider;
import org.mavai.punit.examples.servicecontracts.ShoppingBasketServiceContract;

/**
 * The conventional bindings class for this package's contract files —
 * discovered by name, beside the tests. One binding is the whole code
 * surface of the declarative example: the contract file owns the
 * claim, this method owns the call.
 *
 * <p>The expected-failure discipline applies unchanged: an anticipated
 * bad response travels back as an {@link Outcome} failure for the
 * criteria to judge; only genuine defects throw.
 */
class MavaiBindings {

    private final ChatLlm llm = ChatLlmProvider.resolve();

    @Binding("basket-builder")
    Outcome<String> buildBasket(String instruction) {
        try {
            return Outcome.ok(llm.chat(
                    ShoppingBasketServiceContract.DEFAULT_SYSTEM_PROMPT,
                    instruction,
                    ShoppingBasketServiceContract.DEFAULT_MODEL,
                    ShoppingBasketServiceContract.DEFAULT_TEMPERATURE));
        } catch (ChatLlmException unavailable) {
            return Outcome.fail("llm-unavailable", unavailable.getMessage());
        }
    }
}
