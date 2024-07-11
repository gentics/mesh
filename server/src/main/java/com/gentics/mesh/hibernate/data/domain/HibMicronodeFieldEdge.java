package com.gentics.mesh.hibernate.data.domain;

import com.gentics.mesh.core.data.node.field.nesting.MicronodeField;

/**
 * Some common state-independent functionality for micronode field edges.
 * 
 * @author plyhun
 *
 */
public interface HibMicronodeFieldEdge extends MicronodeField, HibFieldEdge {

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
}
