package com.gentics.mesh.core.search.index.node;

import java.util.Optional;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.search.index.MappingProvider;

import io.vertx.core.json.JsonObject;

/**
 * Elasticsearch index mapping provider for the node indices which store node contents.
 */
public interface NodeContainerMappingProvider extends MappingProvider {

	/**
	 * Return the type specific mapping which is constructed using the provided schema.
	 * 
	 * @param schema
	 *            Schema from which the mapping should be constructed
	 * @param branch
	 *            The branch-version which should be used for the construction
	 * @param language
	 *            The language override to use
	 * @return An ES-Mapping for the given Schema in the branch
	 */
	Optional<JsonObject> getMapping(SchemaModel schema, HibBranch branch, String language);

	/**
	 * Return the type specific mapping which is constructed using the provided schema.
	 * 
	 * @param schema
	 *            Schema from which the mapping should be constructed
	 * @return An ES-Mapping for the given Schema
	 */
	Optional<JsonObject> getMapping(SchemaModel schema);
}
