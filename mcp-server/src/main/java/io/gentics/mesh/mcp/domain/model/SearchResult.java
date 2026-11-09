package io.gentics.mesh.mcp.domain.model;

public record SearchResult(
        String  uuid,
        String  title,
        String  schema,
        String  language,
        String  path,
        String  snippet,
        boolean published
) {}
