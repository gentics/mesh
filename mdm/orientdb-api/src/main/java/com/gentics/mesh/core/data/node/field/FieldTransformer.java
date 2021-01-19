package com.gentics.mesh.core.data.node.field;

import java.util.List;
import java.util.function.Supplier;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;

/**
 * Transformer for domain models of fields into REST models.
 * 
 * @param <T>
 *            REST model type of the field
 */
@FunctionalInterface
public interface FieldTransformer<T extends Field> {

	/**
	 * Load the field with the given key from the container and transform it to its rest representation.
	 * 
	 * @param container
	 *            Container which holds the field
	 * @param ac
	 * @param fieldKey
	 *            Key of the field to be transformed
	 * @param fieldSchema
	 *            Field schema to be used during transformation
	 * @param languageTags
	 * @param level
	 *            Current level of transformation
	 * @param parentNode
	 * @return
	 */
	T transform(GraphFieldContainer container, InternalActionContext ac, String fieldKey, FieldSchema fieldSchema, List<String> languageTags,
		int level, Supplier<HibNode> parentNode);

}
