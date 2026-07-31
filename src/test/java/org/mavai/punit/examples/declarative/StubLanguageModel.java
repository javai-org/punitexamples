package org.mavai.punit.examples.declarative;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * A local language-model endpoint for the pure-services example: an
 * HTTP server speaking the ollama chat shape, returning deterministic
 * shopping-basket action JSON with token counts. The declarative
 * examples point {@code mavai.llm.endpoint} here, so the whole
 * punit-lm wire path — request encoding, delivery taxonomy, usage
 * extraction — runs offline, exactly as it would against a live
 * model. The prompt-engineer stepper's meta calls land here too (it
 * defaults to the optimized service's own provider and model).
 */
final class StubLanguageModel implements AutoCloseable {

    private static final String BASKET_JSON = """
            {"actions": [{"context": "SHOP", "name": "add", "parameters": [\
            {"name": "item", "value": "apples"}, {"name": "quantity", "value": "2"}]}]}""";

    private final HttpServer server;

    private StubLanguageModel(HttpServer server) {
        this.server = server;
    }

    static StubLanguageModel start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                String reply = """
                        {"message": {"role": "assistant", "content": %s},
                         "prompt_eval_count": 42, "eval_count": 17, "done": true}
                        """.formatted(quoted(BASKET_JSON));
                byte[] body = reply.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            server.start();
            return new StubLanguageModel(server);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    /** Points the punit-lm endpoint tier at this stub; cleared on close. */
    StubLanguageModel install() {
        System.setProperty("mavai.llm.endpoint",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/api/chat");
        return this;
    }

    private static String quoted(String text) {
        return '"' + text.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    @Override
    public void close() {
        System.clearProperty("mavai.llm.endpoint");
        server.stop(0);
    }
}
