package com.gentics.mesh.core.data;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;

/**
 * A core element is a public element which is also usually accessible via REST.
 */
public interface HibCoreElement extends HibElement {

	/**
	 * Method which is being invoked once the element has been created.
	 */
	MeshElementEventModel onCreated();

	/**
	 * Method which is being invoked once the element has been updated.
	 * 
	 * @return Created event
	 */
	MeshElementEventModel onUpdated();

	/**
	 * Method which is being invoked once the element has been deleted.
	 * 
	 * @return Created event
	 */
	MeshElementEventModel onDeleted();

	/**
	 * Return the type info of the element.
	 * 
	 * @return
	 */
	TypeInfo getTypeInfo();
}
