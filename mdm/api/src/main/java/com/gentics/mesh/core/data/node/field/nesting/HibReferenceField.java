package com.gentics.mesh.core.data.node.field.nesting;

import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.data.HibField;

/**
 * A field that references another entity.
 * 
 * @author plyhun
 *
 */
public interface HibReferenceField<T extends HibElement> extends HibField {

	/**
	 * Get the referenced entity.
	 * 
	 * @return
	 */
	T getReferencedEntity();

}
