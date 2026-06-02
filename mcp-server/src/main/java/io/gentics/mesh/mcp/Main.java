package io.gentics.mesh.mcp;

import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.domain.AppError;
import io.gentics.mesh.mcp.mesh.MeshClient;
import io.gentics.mesh.mcp.server.AuthValidator;
import io.gentics.mesh.mcp.server.BearerTokenValidator;
import io.gentics.mesh.mcp.server.HttpMcpServer;
import io.gentics.mesh.mcp.server.McpServerRunner;
import io.gentics.mesh.mcp.server.MeshCallGuard;
import io.gentics.mesh.mcp.server.Metrics;
import io.gentics.mesh.mcp.server.ToolDispatcher;
import io.gentics.mesh.mcp.showcase.ShowcaseServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the <strong>Gentics Mesh MCP Server (Java)</strong>.
 *
 * <h2>Modes</h2>
 * <ul>
 *   <li><strong>MCP mode</strong> (default) – communicates over stdin/stdout via JSON-RPC 2.0.
 *       Used when Claude Desktop or another MCP client spawns the process.</li>
 *   <li><strong>Showcase mode</strong> ({@code --showcase} flag) – starts a local HTTP server
 *       on port 3000 and serves the interactive browser demo at {@code http://localhost:3000}.</li>
 * </ul>
 *
 * <h2>Startup sequence</h2>
 * <ol>
 *   <li>Load and validate all configuration from environment variables via {@link MeshConfig#load()}.
 *       The process exits immediately with a human-readable message if required variables are
 *       missing or invalid – no silent mis-configuration.</li>
 *   <li>Construct a {@link MeshClient} that wraps the Gentics Mesh REST/GraphQL API.</li>
 *   <li>Depending on the mode: start the MCP server (blocking stdio) or the showcase HTTP server.</li>
 * </ol>
 *
 * <h2>Configuration</h2>
 * All settings are read from environment variables. See {@code .env.example} for a full
 * reference. The minimum required set is:
 * <pre>
 *   MESH_BASE_URL          http://localhost:8080
 *   MESH_USERNAME          admin
 *   MESH_PASSWORD          admin
 *   MESH_ALLOWED_PROJECTS  demo
 * </pre>
 *
 * @see MeshConfig
 * @see MeshClient
 * @see McpServerRunner
 * @see ShowcaseServer
 * @since 0.1.0
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    /**
     * Application entry point.
     *
     * @param args pass {@code --showcase} to start the browser demo instead of the MCP server
     */
    public static void main(String[] args) {
        boolean showcaseMode = args.length > 0 && "--showcase".equals(args[0]);
        boolean httpFlag     = hasFlag(args, "--http");
        boolean stdioFlag    = hasFlag(args, "--stdio");

        try {
            // 1. Load + validate configuration
            MeshConfig config = MeshConfig.load();
            log.info("[OK] Konfiguration geladen – Projekte: {}", config.meshAllowedProjects());

            // 2. Build Mesh client
            MeshClient client = new MeshClient(config);

            if (showcaseMode) {
                // 3a. Showcase mode: HTTP server on port 3000
                new ShowcaseServer(config, client).start();
                // Keep the main thread alive; the HTTP server runs on daemon threads
                Thread.currentThread().join();
            } else {
                // 3b. Determine MCP transport: CLI flag overrides MCP_TRANSPORT env var.
                String transport;
                if (httpFlag)        transport = "http";
                else if (stdioFlag)  transport = "stdio";
                else                 transport = config.mcpTransport();

                String serverVersion = resolveServerVersion();
                // Protection layer & metrics: identical for both transports.
                Metrics metrics = new Metrics();
                MeshCallGuard guard = new MeshCallGuard(config, metrics);
                ToolDispatcher dispatcher = new ToolDispatcher(config, client, guard);

                if ("http".equals(transport)) {
                    AuthValidator authValidator = config.mcpAuthDisabled()
                            ? AuthValidator.ALLOW_ALL
                            : new BearerTokenValidator(config.mcpAuthToken());
                    new HttpMcpServer(config, dispatcher, authValidator, metrics, serverVersion).start();
                } else {
                    new McpServerRunner(dispatcher, serverVersion).run();
                }
            }

        } catch (AppError e) {
            System.err.println("[FATAL] Konfigurationsfehler: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("[FATAL] Unerwarteter Fehler beim Start: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String a : args) if (flag.equals(a)) return true;
        return false;
    }

    /**
     * Resolves the server version reported to MCP clients.
     *
     * <p>Reads {@code Implementation-Version} from the JAR manifest, which the
     * maven-shade-plugin populates from {@code ${project.version}}. When the
     * server is launched from an IDE without a built JAR the manifest entry is
     * absent — in that case we fall back to {@code "dev"} so the field is
     * never null in the MCP {@code serverInfo} payload.
     */
    private static String resolveServerVersion() {
        String v = Main.class.getPackage().getImplementationVersion();
        return (v != null && !v.isBlank()) ? v : "dev";
    }
}
