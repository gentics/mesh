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
	 * Get the field key, stored within this edge.
	 * 
	 * @return
	 */
	String getStoredFieldName();
}
