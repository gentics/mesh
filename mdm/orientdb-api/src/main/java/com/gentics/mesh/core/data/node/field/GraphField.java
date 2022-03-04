package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;

/**
 * Common interface for all graph fields. Every field has a key and can be removed from a container.
 */
public interface GraphField extends HibField {

	String FIELD_KEY_PROPERTY_KEY = "fieldkey";

	/**
	 * Remove this field from the container.
	 * 
	 * @param bac
	 * @param container
	 *            container
	 */
	void removeField(BulkActionContext bac, HibFieldContainer container);

	/**
	 * Remove the field and use a dummy bulk action context.
	 * 
	 * @param container
	 */
	default void removeField(HibFieldContainer container) {
		removeField(new DummyBulkActionContext(), container);
	}
}
