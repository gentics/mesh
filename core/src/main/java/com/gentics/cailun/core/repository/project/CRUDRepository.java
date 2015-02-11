package com.gentics.cailun.core.repository.project;


public interface CRUDRepository<T> {

	/**
	 * Loads a entity which is identified by its uuid.
	 * 
	 * @param uuid
	 * @return the loaded entity
	 */
	T findByUUID(String uuid);

	/**
	 * Deletes a given entity.
	 * 
	 * @param entity
	 * @throws IllegalArgumentException
	 *             in case the given entity is (@literal null}.
	 */
	void delete(T entity);

	/**
	 * Deletes a given entity which is identified by its uuid.
	 * 
	 * @param uuid
	 * @throws IllegalArgumentException
	 *             in case the given entity is (@literal null}.
	 */
	void delete(String uuid);

	/**
	 * Saves a given entity. Use the returned instance for further operations as the save operation might have changed the entity instance completely.
	 * 
	 * 
	 * @param entity
	 * @return the saved entity
	 */
	T save(T entity);

	/**
	 * Returns the number of entities available.
	 * 
	 * @return the number of entities
	 */
	long count();

}
