package com.gentics.mesh.core.db;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.dao.Dao;

/**
 * A set of low level entity persistence management methods.
 * 
 * @author plyhun
 *
 */
public interface TxEntityPersistenceManager {
	
	/**
	 * Create a new persisted entity with the given optional uuid. 
	 * If uuid parameter is null, a new generated UUID will be used.<br>
	 * 
	 * @param <T>
	 * @param uuid 
	 * @param classOfT the persistence class to use
	 * @return
	 */
	<T extends HibBaseElement> T create(String uuid, Class<? extends T> classOfT);
	
	/**
	 * Merge the data from given POJO into the persistent entity.<br>
	 * 
	 * @param element
	 * @param classOfT the persistence class to use
	 * @return
	 */
	<T extends HibBaseElement> T persist(T element, Class<? extends T> classOfT);
	
	/**
	 * Delete the persistent entity.<br>
	 * 
	 * @param element
	 * @param classOfT the persistence class to use
	 */
	<T extends HibBaseElement> void delete(T element, Class<? extends T> classOfT);

	/**
	 * Create a new persisted entity with the autogenerated uuid.
	 * Prefer {@link TxEntityPersistenceManager#create(String, Dao)} over this method, 
	 * to keep the creation business logic. This API method serves test purposes.
	 * 
	 * @param <T>
	 * @param classOfT
	 * @return
	 */
	default <T extends HibBaseElement> T create(Class<? extends T> classOfT) {
		return create(null, classOfT);
	}
}