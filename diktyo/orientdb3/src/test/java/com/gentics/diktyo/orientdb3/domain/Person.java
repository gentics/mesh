package com.gentics.diktyo.orientdb3.domain;

import com.gentics.diktyo.wrapper.element.WrappedVertex;

public interface Person extends WrappedVertex {

	/**
	 * Return the name of the person.
	 * 
	 * @return
	 */
	String getName();

	/**
	 * Set the name of the person.
	 * 
	 * @param name
	 */
	void setName(String name);

	/**
	 * Set the job of the person.
	 * 
	 * @param job
	 */
	void setJob(Job job);

	/**
	 * Return the job of the person.
	 * 
	 * @return
	 */
	Job getJob();

}
