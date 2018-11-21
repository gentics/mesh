package com.gentics.diktyo.db;

/**
 * Manager for graph databases.
 */
public interface DatabaseManager {

	/**
	 * Open the database with the given name.
	 * 
	 * @param name
	 * @param type
	 * @return opened database
	 * 
	 */
	Database open(String name, DatabaseType type);

	/**
	 * Create a new database with the given name.
	 * 
	 * @param name
	 * @param type
	 */
	void create(String name, DatabaseType type);

	/**
	 * Delete the database with the given name.
	 * 
	 * @param name
	 */
	void delete(String name);

	/**
	 * Check whether the given database exists.
	 * 
	 * @param name
	 * @return
	 */
	boolean exists(String name);

}
