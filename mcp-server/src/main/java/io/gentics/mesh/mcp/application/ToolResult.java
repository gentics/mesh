package io.gentics.mesh.mcp.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gentics.mesh.mcp.domain.AppError;
import io.gentics.mesh.mcp.domain.ErrorCode;

/**
 * Serialises a tool result to the JSON string that use-cases hand back to the
 * transport layer.
 *
 * <p>Exists so that use-cases do not have to declare Jackson's checked
 * {@link JsonProcessingException}. They must not: every dispatch runs inside a
 * {@link java.util.function.Supplier} passed to
 * {@link io.gentics.mesh.mcp.server.MeshCallGuard#execute}, and a {@code Supplier}
 * cannot throw checked exceptions. Serialisation failure is a bug in the mapping
 * layer, not a client error, so it surfaces as
 * {@link ErrorCode#INTERNAL_ERROR} like every other unexpected fault.
 *
 * @since 0.3.0
 */
final class ToolResult {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Utility class – do not instantiate. */
    private ToolResult() {}

    /**
     * @param payload any Jackson-serialisable object (usually an {@code ObjectNode})
     * @return the payload as a compact JSON string
     * @throws AppError(INTERNAL_ERROR) if Jackson cannot serialise the payload
     */
    static String json(Object payload) {
        try {
            return MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new AppError(ErrorCode.INTERNAL_ERROR,
                    "Tool-Ergebnis konnte nicht serialisiert werden: " + e.getMessage(), e);
        }
    }
}
