package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.ElementType.SCHEMA;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_UPDATED;

import java.util.Objects;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.search.BucketableElement;

/**
 * A schema container is a graph element which stores the JSON schema data.
 */
public interface SchemaContainer extends GraphFieldSchemaContainer<SchemaResponse, SchemaReference, SchemaContainer, SchemaContainerVersion>, BucketableElement {

	TypeInfo TYPE_INFO = new TypeInfo(SCHEMA, SCHEMA_CREATED, SCHEMA_UPDATED, SCHEMA_DELETED);

	/**
	 * Compose the index name for the schema index.
	 * 
	 * @return
	 */
	static String composeIndexName() {
		return "schemacontainer";
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
	TraversalResult<? extends Node> getNodes();

	/**
	 * Return a list of all schema container roots to which the schema container was added.
	 * 
	 * @return
	 */
	TraversalResult<? extends SchemaContainerRoot> getRoots();

}
