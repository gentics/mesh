package com.gentics.mesh.cache;

import java.util.function.Function;

import com.gentics.mesh.core.data.Project;

public interface ProjectNameCache extends MeshCache {

	Project get(String name, Function<String, Project> mappingFunction);

}
