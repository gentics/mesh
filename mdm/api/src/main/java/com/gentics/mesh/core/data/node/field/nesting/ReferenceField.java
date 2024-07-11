package com.gentics.mesh.core.data.node.field.nesting;

import com.gentics.mesh.core.data.Element;
import com.gentics.mesh.core.data.Field;

/**
 * A field that references another entity.
 * 
 * @author plyhun
 *
 */
public interface ReferenceField<T extends Element> extends Field {

	/**
	 * Get the referenced entity.
	 * 
	 * @return
	 */
	T getReferencedEntity();

}
