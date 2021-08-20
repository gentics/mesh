package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_UPDATED;

import java.util.Objects;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.search.GraphDBBucketableElement;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

/**
 * A microschema container is a graph element which stores the JSON microschema data.
 */
public interface Microschema extends
		GraphFieldSchemaContainer<MicroschemaResponse, MicroschemaReference, HibMicroschema, HibMicroschemaVersion>, HibMicroschema, GraphDBBucketableElement {

	TypeInfo TYPE_INFO = new TypeInfo(ElementType.MICROSCHEMA, MICROSCHEMA_CREATED, MICROSCHEMA_UPDATED, MICROSCHEMA_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Compose the index name for the microschema index.
	 * 
	 * @return
	 */
	static String composeIndexName() {
		return "microschema";
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
