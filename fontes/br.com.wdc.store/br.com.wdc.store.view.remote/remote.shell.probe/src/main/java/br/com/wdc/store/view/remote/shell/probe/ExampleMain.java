package br.com.wdc.store.view.remote.shell.probe;

import java.io.PrintStream;
import java.util.Map;

import br.com.wdc.framework.cube.remote.bridge.java.HostClient;
import br.com.wdc.framework.cube.remote.bridge.java.model.HostResponse;
import br.com.wdc.framework.cube.remote.bridge.java.model.ScopeSnapshot;

/**
 * Demonstrates how to use {@link HostClient} to connect to a running Host,
 * navigate to the product list and print the scopes.
 *
 * <p>Run with a Host server running at {@code http://localhost:8080}.
 */
public class ExampleMain {
    
    static final PrintStream out = System.out; // NOSONAR
    static final PrintStream err = System.err; // NOSONAR

    public static void main(String[] args) throws Exception {
        String serverUrl = args.length > 0 ? args[0] : "http://localhost:8080";

        out.println("=== HostClient example — connecting to " + serverUrl + " ===");

        try (var client = HostClient.connect(serverUrl)) {

            // -- Step 1: Wait for the initial async scope push --
            out.println("\n[1] Awaiting initial scope push...");
            HostResponse initial = client.awaitResponse();
            out.println("    Response: " + initial);
            printScopes("Initial scopes", client);

            if (initial.uri() == null) {
                out.println("    No URI in initial push — server may still be loading.");
                out.println("    Waiting for second push...");
                initial = client.awaitResponse();
                out.println("    Response: " + initial);
                printScopes("After second push", client);
            }

            // -- Step 2: Navigate using the signed URI from the initial push --
            String navigationUri = initial.uri();
            if (navigationUri != null) {
                out.println("\n[2] Navigating to: " + navigationUri);
                client.navigate(navigationUri);
                HostResponse afterNav = client.awaitResponse();
                out.println("    Response: " + afterNav);
                printScopes("After navigation", client);
            } else {
                out.println("\n[2] Skipping navigation — no URI available.");
            }

            // -- Step 3: Submit an event on the first available view (if any) --
            var allScopes = client.allScopes();
            if (!allScopes.isEmpty()) {
                ScopeSnapshot firstView = allScopes.iterator().next();
                out.println("\n[3] Submitting event 1 on: " + firstView.instanceId());
                client.submit(firstView.instanceId(), 1, Map.of());
                try {
                    HostResponse afterEvent = client.awaitResponse();
                    out.println("    Response: " + afterEvent);
                    printScopes("After event", client);
                } catch (java.util.concurrent.TimeoutException e) {
                    out.println("    No response after event (event may have no handler).");
                }
            } else {
                out.println("\n[3] No scopes available — skipping event submission.");
            }

            out.println("\n=== Done ===");
        }
    }

    private static void printScopes(String label, HostClient client) {
        var all = client.allScopes();
        out.println("    " + label + " (" + all.size() + " views):");
        all.forEach(vs -> {
            out.println("      [" + vs.instanceId() + "] " + vs.values());
        });
    }
}
