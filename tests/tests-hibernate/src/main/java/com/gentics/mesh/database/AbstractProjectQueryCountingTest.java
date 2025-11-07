package com.gentics.mesh.database;

import static com.gentics.mesh.test.ClientHelper.call;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;

import com.gentics.mesh.core.rest.lang.LanguageListResponse;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.parameter.client.PagingParametersImpl;

public abstract class AbstractProjectQueryCountingTest extends AbstractCountingTest {
	public final static int NUM_PROJECTS = 53;

	public final static int NUM_LANGUAGES_PER_PROJECT = 8;

	protected static int totalNumProjects = NUM_PROJECTS;

	protected static Set<String> initialProjectUuids = new HashSet<>();

	@Before
	public void setup() {
		if (getTestContext().needsSetup()) {
			ProjectListResponse initialProjects = call(() -> client().findProjects());
			initialProjectUuids.addAll(initialProjects.getData().stream().map(ProjectResponse::getUuid).toList());
			totalNumProjects += initialProjects.getMetainfo().getTotalCount();

			LanguageListResponse languages = call(() -> client().findLanguages(new PagingParametersImpl().setPerPage((long)NUM_LANGUAGES_PER_PROJECT)));
			List<String> languageUuids = languages.getData().stream().map(LanguageResponse::getUuid).collect(Collectors.toList());

			// create projects
			for (int i = 0; i < NUM_PROJECTS; i++) {
				ProjectResponse project = createProject("project_%d".formatted(i));
	
				// assign languages
				for (String languageUuid : languageUuids) {
					call(() -> client().assignLanguageToProject(project.getName(), languageUuid));
				}
			}
		}

		// clear the cache
		adminCall(() -> client().clearCache());
	}
}
