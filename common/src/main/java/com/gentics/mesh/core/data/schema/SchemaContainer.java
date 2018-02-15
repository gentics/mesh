package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.Events.EVENT_SCHEMA_CREATED;
import static com.gentics.mesh.Events.EVENT_SCHEMA_DELETED;
import static com.gentics.mesh.Events.EVENT_SCHEMA_UPDATED;
import static com.gentics.mesh.search.SearchProvider.INDEX_PREFIX;

import java.util.List;
import java.util.Objects;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.IndexableElement;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;

/**
 * A schema container is a graph element which stores the JSON schema data.
 */
public interface SchemaContainer extends GraphFieldSchemaContainer<SchemaResponse, SchemaReference, SchemaContainer, SchemaContainerVersion>,
		IndexableElement {

	/**
	 * Type Value: {@value #TYPE}
	 */
	String TYPE = "schemaContainer";

	TypeInfo TYPE_INFO = new TypeInfo(TYPE, EVENT_SCHEMA_CREATED, EVENT_SCHEMA_UPDATED, EVENT_SCHEMA_DELETED);

	/**
	 * Compose the index name for the schema index.
	 * 
	 * @return
	 */
	static String composeIndexName() {
		return INDEX_PREFIX + TYPE.toLowerCase();
	}

	/**
	 * Compose the documentId for schema index documents.
	 * 
	 * @param schemaContainerUuid
	 * @return
	 */
	static String composeDocumentId(String schemaContainerUuid) {
		Objects.requireNonNull(schemaContainerUuid, "A schemaContainerUuid must be provided.");
		return schemaContainerUuid;
	}

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Returns an iterable of nodes which are referencing the schema container.
	 * 
	 * @return
	 */
	Iterable<? extends Node> getNodes();

	/**
	 * Return a list of all schema container roots to which the schema container was added.
	 * 
	 * @return
	 */
	List<? extends SchemaContainerRoot> getRoots();

}
