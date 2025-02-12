package com.gentics.mesh.hibernate.data.domain;

import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;

/**
 * Some common state-independent functionality for micronode field edges.
 * 
 * @author plyhun
 *
 */
public interface HibMicronodeFieldEdge extends HibMicronodeField, HibFieldEdge {

	@Override
	default String getFieldKey() {
		return maybeGetFieldNameFromMicronodeParent().orElse(getStoredFieldKey());
	}

	/**
	 * Get the field key, stored within this edge.
	 * 
	 * @return
	 */
	String getStoredFieldKey();

	/**
	 * Get the microschema version of this edge.
	 * @return
	 */
	HibMicroschemaVersion getMicroschemaVersion();
}
