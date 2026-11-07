package io.gentics.mesh.mcp.domain.model;

import java.util.List;

public record MeshSchema(String uuid, String name, List<SchemaField> fields) {}
