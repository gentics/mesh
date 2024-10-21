package com.gentics.mesh.hibernate.data.domain;

import java.util.Optional;

import com.gentics.mesh.core.data.node.field.nesting.HibNodeFieldCommon;
import com.gentics.mesh.core.rest.common.ReferenceType;

/**
 * Some common state-independent functionality for node field edges.
 * 
 * @author plyhun
 *
 */
public interface HibNodeFieldEdge extends HibNodeFieldCommon, HibFieldEdge {

	@Override
	default Optional<String> getMicronodeFieldName() {
		if (ReferenceType.MICRONODE == getContainerType()) {
			return Optional.of(getStoredFieldName());
		} else {
			return Optional.empty();
		}
	}

	@Override
	default String getFieldName() {
		return maybeGetFieldNameFromMicronodeParent().orElse(getStoredFieldName());		
	}

	/**
	 * Get the field name, actually stored within this edge, on contrary to {@link #getFieldName()} API method, 
	 * which may return the name of a parent container field.
	 * 
	 * @return
	 */
	String getStoredFieldName();
}
