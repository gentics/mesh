package io.gentics.mesh.mcp.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Normalised representation of a single Gentics Mesh node.
 *
 * <p>Instances are produced by {@link io.gentics.mesh.mcp.mesh.MeshMapper#mapNode}
 * and {@link io.gentics.mesh.mcp.mesh.MeshMapper#mapWebrootNode}. All field
 * values in {@link #fields()} have been processed by
 * {@link io.gentics.mesh.mcp.security.RedactionService#redact} so that
 * sensitive keys (password, token, secret, …) are replaced with
 * {@code "[REDACTED]"}.
 *
 * <p>The shape is intentionally flat and LLM-friendly: the raw Mesh API
 * response is more deeply nested and includes many fields (audit-log,
 * permissions, …) that are irrelevant for content retrieval.
 *
 * @param uuid        globally unique identifier of the node
 * @param project     project the node belongs to
 * @param language    ISO 639-1 language code of the loaded translation
 * @param schema      name of the Mesh schema (e.g. {@code "vehicle"})
 * @param displayName human-readable display name from the schema's display field
 * @param path        URL path (webroot path) of the node, may be empty for folders
 * @param published   {@code true} if the requested language variant is published
 * @param version     content version string (e.g. {@code "1.0"})
 * @param fields      schema field values – sensitive keys are redacted
 * @param references  list of node-reference fields extracted from {@code fields}
 *
 * @see io.gentics.mesh.mcp.mesh.MeshMapper
 * @see NodeReference
 * @since 0.1.0
 */
public record MeshNode(
        String              uuid,
        String              project,
        String              language,
        String              schema,
        String              displayName,
        String              path,
        boolean             published,
        String              version,
        Map<String, Object> fields,
        List<NodeReference> references
) {}
