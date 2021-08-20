package com.gentics.mesh.core.data;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;

/**
 * A graph field container (eg. a container for fields of a node) is used to hold i18n specific graph fields.
 */
public interface GraphFieldContainer extends HibFieldContainer, BasicFieldContainer {

	/**
	 * Delete the field edge with the given key from the container.
	 * 
	 * @param key
	 */
	void deleteFieldEdge(String key);
}
