package com.gentics.mesh.router;

import java.util.Set;

import javax.naming.InvalidNameException;

/**
 * Storage for routes keeps track of all project specific routes across all REST verticles.
 */
public interface RouterStorageRegistry {

	/**
	 * Validate the project name.
	 * 
	 * @param name
	 */
	void assertProjectName(String name);

	/**
	 * Register the project name in the storage.
	 * 
	 * @param projectName
	 * @throws InvalidNameException
	 */
	void addProject(String projectName) throws InvalidNameException;

	/**
	 * Check whether the storage contains the project.
	 * 
	 * @param newName
	 * @return
	 */
	boolean hasProject(String newName);

	/**
	 * Return all router storage instances (each REST verticle has a dedicated storage since routes must not be shared across verticles / threads)
	 * 
	 * @return
	 */
	Set<RouterStorage> getInstances();

}
