package com.gentics.mesh.core.rest.schema;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface SchemaRestriction {
    /**
     * Return the list of allowed schemas.
     *
     * @return
     */
    String[] getAllowedSchemas();

    /**
     * Define a list of allowed schemas.
     *
     * @param allowedSchemas
     * @return
     */
    SchemaRestriction setAllowedSchemas(String... allowedSchemas);

    default boolean isAllowedSchema(String schemaName) {
        List<String> allowedSchemas = Optional.ofNullable(getAllowedSchemas())
            .map(Arrays::asList)
            .orElse(Collections.emptyList());

        // Empty or not set list means that all schemas are allowed
        if (allowedSchemas.isEmpty()) {
            return true;
        } else {
            return allowedSchemas.contains(schemaName);
        }
    }
}
