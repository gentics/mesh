package com.gentics.mesh.hibernate.data.node.field.impl;

import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.hibernate.data.domain.HibBinaryFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibImageDataFieldBase;

/**
 * Base functionality of Hibernate-backed binary field entity.
 * 
 * @author plyhun
 *
 */
public interface HibBinaryFieldBase extends HibBinaryField, HibImageDataFieldBase {

	/**
	 * Get the persisted edge of this field.
	 * 
	 * @return
	 */
	HibBinaryFieldEdgeImpl getEdge();
}
