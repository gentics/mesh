package com.gentics.mesh.router;

import java.util.Set;

import javax.naming.InvalidNameException;

public interface RouterStorageRegistry {

	void assertProjectName(String name);

	void addProject(String projectName) throws InvalidNameException;

	boolean hasProject(String newName);

	Set<RouterStorage> getInstances();

}
