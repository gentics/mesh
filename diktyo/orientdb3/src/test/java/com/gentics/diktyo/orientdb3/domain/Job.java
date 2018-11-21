package com.gentics.diktyo.orientdb3.domain;

import com.gentics.diktyo.wrapper.element.WrappedVertex;

public interface Job extends WrappedVertex {

	/**
	 * Return the job name.
	 * 
	 * @return
	 */
	String getName();

	/**
	 * Set the job name.
	 * 
	 * @param name
	 */
	void setName(String name);

}
