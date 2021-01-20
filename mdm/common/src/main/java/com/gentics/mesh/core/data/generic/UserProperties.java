package com.gentics.mesh.core.data.generic;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.user.HibUser;

/**
 * Domain model extension for elements which store user references (creator, editor) via properties instead of edges.
 */
public interface UserProperties {

	/**
	 * Return the creator of the element.
	 * 
	 * @param element
	 * @return
	 */
	HibUser getCreator(HibBaseElement element);

	/**
	 * Return the editor of the element.
	 * 
	 * @param element
	 * @return
	 */
	HibUser getEditor(HibBaseElement element);

	/**
	 * Set the creator for the element.
	 * 
	 * @param element
	 * @param user
	 */
	void setCreator(HibBaseElement element, HibUser user);

	/**
	 * Set the editor for the element.
	 * 
	 * @param element
	 * @param user
	 */
	void setEditor(HibBaseElement element, HibUser user);

}
