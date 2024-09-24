package com.gentics.mesh.core;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

import io.reactivex.Completable;

/**
 * Uniqueness test for tagfamily names (per project)
 */
@MeshTestSetting(testSize = TestSize.PROJECT, startServer = true)
public class TagFamilyNameUniquenessTest extends AbstractNameUniquenessTest {
	/**
	 * Name of the created tagfamily
	 */
	public final static String TAGFAMILY_NAME = "testtagfamily";

	@Override
	protected Completable createEntity(Optional<String> optParent) {
		return client().createTagFamily(optParent.get(), new TagFamilyCreateRequest().setName(TAGFAMILY_NAME)).toCompletable();
	}

	@Override
	protected Optional<List<String>> parentEntities() {
		List<String> projectNames = Arrays.asList("first_project", "second_project", "third_project");
		for (String projectName : projectNames) {
			client().createProject(new ProjectCreateRequest().setName(projectName).setSchemaRef("folder")).blockingAwait();
		}

		return Optional.of(projectNames);
	}
}
