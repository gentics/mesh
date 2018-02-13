package com.gentics.mesh.core.rest.schema;

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
}
