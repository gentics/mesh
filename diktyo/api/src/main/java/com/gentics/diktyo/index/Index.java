package com.gentics.diktyo.index;

public interface Index {

	/**
	 * Return the name of the index.
	 * 
	 * @return
	 */
	String name();

	/**
	 * Refresh the index.
	 */
	void refresh();

	/**
	 * Remove the index.
	 */
	void remove();

}
