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
public interface PersistingDao<T extends HibBaseElement> {

	/**
	 * Get the final type of the persistence entity of the dao.
	 * 
	 * @return
	 */
	Class<? extends T> getPersistenceClass();

	/**
	 * Created a persisted entity.
	 * 
	 * @param uuid
	 *            Optional uuid
	 * @return
	 */
	T createPersisted(String uuid);

	/**
	 * Merge the entity into its persisted state.
	 * 
	 * @param entity
	 * @return
	 */
	T mergeIntoPersisted(T entity);

	/**
	 * Delete the entity from the persistent storage.
	 * 
	 * @param entity
	 */
	void deletePersisted(T entity);
}