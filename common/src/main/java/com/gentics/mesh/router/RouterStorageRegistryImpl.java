package com.gentics.mesh.router;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.InvalidNameException;

/**
 * @see RouterStorageRegistry
 */
@Singleton
public class RouterStorageRegistryImpl implements RouterStorageRegistry {

	private Set<RouterStorage> instances = new ConcurrentSkipListSet<>();

	@Inject
	public RouterStorageRegistryImpl() {
	}

	/**
	 * Register the eventbus handlers of all stored router storages.
	 */
	public synchronized void registerEventbus() {
		for (RouterStorage rs : instances) {
			rs.registerEventbusHandlers();
		}
	}

	/**
	 * Iterate over all created router storages and assert that no project/api route causes a conflict with the given name
	 * 
	 * @param name
	 */
	public synchronized void assertProjectName(String name) {
		for (RouterStorage rs : instances) {
			rs.root().apiRouter().projectsRouter().assertProjectNameValid(name);
		}
	}

	@Override
	public synchronized void addProject(String name) throws InvalidNameException {
		for (RouterStorage rs : instances) {
			rs.root().apiRouter().projectsRouter().addProjectRouter(name);
		}
	}

	@Override
	public synchronized boolean hasProject(String projectName) {
		for (RouterStorage rs : instances) {
			if (rs.root().apiRouter().projectsRouter().hasProjectRouter(projectName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<RouterStorage> getInstances() {
		return instances;
	}

}
