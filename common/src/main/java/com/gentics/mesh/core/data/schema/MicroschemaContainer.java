package com.gentics.mesh.core.data.schema;

import java.util.Objects;

import com.gentics.mesh.core.data.IndexableElement;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

/**
 * A microschema container is a graph element which stores the JSON microschema data.
 */
public interface MicroschemaContainer
		extends GraphFieldSchemaContainer<Microschema, MicroschemaReference, MicroschemaContainer, MicroschemaContainerVersion>, IndexableElement {

	/**
	 * Type Value: {@value #TYPE}
	 */
	static final String TYPE = "microschema";

	@Override
	default String getType() {
		return TYPE;
	}

	/**
	 * Compose the index name for the microschema index.
	 * 
	 * @return
	 */
	static String composeIndexName() {
		return TYPE.toLowerCase();
	}

	/**
	 * Compose the index type for the microschema index.
	 * 
	 * @return
	 */
	static String composeTypeName() {
		return TYPE.toLowerCase();
	}

	/**
	 * Compose the document id for microschema index documents.
	 * 
	 * @param elementUuid
	 * @return
	 */
	static String composeDocumentId(String elementUuid) {
		Objects.requireNonNull(elementUuid, "A elementUuid must be provided.");
		return elementUuid;
	}

}
