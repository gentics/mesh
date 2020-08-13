package com.gentics.mesh.core.data.dao;

/**
 * Interface for DAO's which provide methods which allow installation wide queries on elements. The provided methods should not be scoped to a project, branch.
 * 
 * @param <T>
 */
public interface DaoGlobal<T> {

	T findByUuidGlobal(String uuid);

	long computeGlobalCount();

}
