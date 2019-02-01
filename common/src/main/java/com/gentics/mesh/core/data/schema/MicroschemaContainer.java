package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_UPDATED;
import java.util.Objects;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

/**
 * A microschema container is a graph element which stores the JSON microschema data.
 */
public interface MicroschemaContainer extends
		GraphFieldSchemaContainer<MicroschemaResponse, MicroschemaReference, MicroschemaContainer, MicroschemaContainerVersion> {

	/**
	 * Type Value: {@value #TYPE}
	 */
	String TYPE = "microschema";

	TypeInfo TYPE_INFO = new TypeInfo(TYPE, MICROSCHEMA_CREATED.address, MICROSCHEMA_UPDATED.address, MICROSCHEMA_DELETED.address);

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
