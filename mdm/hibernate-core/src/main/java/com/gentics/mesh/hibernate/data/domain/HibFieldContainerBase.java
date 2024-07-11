package com.gentics.mesh.hibernate.data.domain;

import java.util.stream.Stream;

import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.rest.common.ReferenceType;
import com.gentics.mesh.hibernate.data.node.field.impl.AbstractBasicHibField;

/**
 * A common contract for field containers.
 * 
 * @author plyhun
 *
 */
public interface HibFieldContainerBase extends HibDatabaseElement, FieldContainer, BaseElement {

	/**
	 * Get the reference type that the implementor provides.
	 */
	ReferenceType getReferenceType();

	/**
	 * Store the value into the unmanaged database table.
	 * 
	 * @param field
	 * @param value
	 */
	void storeValue(AbstractBasicHibField<?> field, Object value);

	/**
	 * Find the node field containers, that this field container belongs to.
	 * 
	 * @return
	 */
	Stream<? extends NodeFieldContainer> getNodeFieldContainers();
}
