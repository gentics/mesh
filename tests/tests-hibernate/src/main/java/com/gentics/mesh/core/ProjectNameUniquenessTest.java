package com.gentics.mesh.core;

import java.util.List;
import java.util.Optional;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

import io.reactivex.Completable;

/**
 * Uniqueness test for project names
 */
@MeshTestSetting(testSize = TestSize.PROJECT, startServer = true)
public class ProjectNameUniquenessTest extends AbstractNameUniquenessTest {
	/**
	 * Name of the created project
	 */
	public final static String PROJECT_NAME = "testproject";

	@Override
	protected Completable createEntity(Optional<String> optParent) {
		return client().createProject(new ProjectCreateRequest().setName(PROJECT_NAME).setSchemaRef("folder")).toCompletable();
	}

	@Override
	protected Optional<List<String>> parentEntities() {
		return Optional.empty();
	}

}
