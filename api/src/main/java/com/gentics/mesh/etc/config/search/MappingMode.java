package com.gentics.mesh.etc.config.search;

/**
 * The mapping mode controls how elasticsearch mappings are handled.
 */
public enum MappingMode {

	// Only add the field and the mapping if a custom mapping has been provided.
	STRICT,

	// Add the default Gentics Mesh mapping for the field.
	DYNAMIC;
}
