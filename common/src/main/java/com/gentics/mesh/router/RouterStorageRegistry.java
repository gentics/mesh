package com.gentics.mesh.router;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.InvalidNameException;

@Singleton
public class RouterStorageRegistry {

	private Set<RouterStorage> instances = new HashSet<>();

	@Inject
	public RouterStorageRegistry() {
	}

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

	public synchronized void addProject(String name) throws InvalidNameException {
		for (RouterStorage rs : instances) {
			rs.root().apiRouter().projectsRouter().addProjectRouter(name);
		}
	}

	public synchronized boolean hasProject(String projectName) {
		for (RouterStorage rs : instances) {
			if (rs.root().apiRouter().projectsRouter().hasProjectRouter(projectName)) {
				return true;
			}
		}
		return false;
	}

	public Set<RouterStorage> getInstances() {
		return instances;
	}

}
