package com.gentics.mesh.core.data.dao;

/**
 * Interface for DAO's which provide methods which allow installation wide queries on elements. The provided methods should not be scoped to a project, branch.
 * 
 * @param <T>
 */
public interface DaoGlobal<T> {

	/**
	 * Find the element globally.
	 * 
	 * @param uuid
	 * @return
	 */
	T findByUuidGlobal(String uuid);

	/**
	 * Return total amount of elements which are stored.
	 * 
	 * @return
	 */
	long globalCount();

}
