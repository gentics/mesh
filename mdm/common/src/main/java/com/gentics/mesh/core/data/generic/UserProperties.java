package com.gentics.mesh.core.data.generic;

import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.core.data.user.User;

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
	User getCreator(BaseElement element);

	/**
	 * Return the editor of the element.
	 * 
	 * @param element
	 * @return
	 */
	User getEditor(BaseElement element);

	/**
	 * Set the creator for the element.
	 * 
	 * @param element
	 * @param user
	 */
	void setCreator(BaseElement element, User user);

	/**
	 * Set the editor for the element.
	 * 
	 * @param element
	 * @param user
	 */
	void setEditor(BaseElement element, User user);

}
