package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.HibBaseElement;

/**
 * A DAO for persistent state manipulation. Use {@link Dao}/{@link DaoGlobal} instead of this when possible,
 * since those are higher level APIs.
 * 
 * @author plyhun
 *
 * @param <T>
 */
public interface DaoPersistable<T extends HibBaseElement> {

	/**
	 * Create a new user with the given username and assign it to this aggregation node.
	 * 
	 * @param username
	 *            Username for the newly created user
	 * @param creator
	 *            User that is used to create creator and editor references
	 * @param uuid
	 *            Optional uuid
	 * @return
	 */
	T createPersisted(String uuid);
	
	/**
	 * Merge the data from given POJO into the persistent entity.
	 * 
	 * @param element
	 * @return
	 */
	T mergeIntoPersisted(T element);
	
	/**
	 * Delete the persistent entity.
	 * 
	 * @param element
	 */
	void deletePersisted(T element);
}
