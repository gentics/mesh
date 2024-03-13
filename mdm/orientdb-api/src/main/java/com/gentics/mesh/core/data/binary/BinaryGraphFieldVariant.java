package com.gentics.mesh.core.data.binary;

import com.gentics.mesh.core.data.HibFieldKeyElement;
import com.gentics.mesh.core.data.MeshEdge;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.GraphField;

/**
 * An edge connection between {@link BinaryGraphField} and {@link ImageVariant}.
 * 
 * @author plyhun
 *
 */
public interface BinaryGraphFieldVariant extends HibFieldKeyElement, MeshEdge {

	/**
	 * Get the connected image variant.
	 * 
	 * @return
	 */
	ImageVariant getVariant();

	@Override
	default void setFieldKey(String key) {
		property(GraphField.FIELD_KEY_PROPERTY_KEY, key);
	}

	@Override
	default String getFieldKey() {
		return property(GraphField.FIELD_KEY_PROPERTY_KEY);
	}
}
