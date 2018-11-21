package com.gentics.diktyo.index;

import java.util.List;

public interface IndexManager {

	boolean exists(String name);

	/**
	 * Fetch the index with the given name.
	 * 
	 * @param name
	 * @return
	 */
	Index get(String name);

	/**
	 * List indices.
	 * 
	 * @return
	 */
	List<Index> list();

}
