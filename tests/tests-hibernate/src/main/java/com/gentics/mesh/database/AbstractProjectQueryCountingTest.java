package com.gentics.mesh.database;

import static com.gentics.mesh.test.ClientHelper.call;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Strings;
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
			// when the list of the languages does not contain english, we remove one language from the list (because english is automatically assigned to the project)
			boolean containsEnglish = languages.getData().stream().filter(lang -> Strings.CI.equals(lang.getLanguageTag(), "en")).findFirst().isPresent();

			List<String> languageUuids = new ArrayList<>(languages.getData().stream().map(LanguageResponse::getUuid).collect(Collectors.toList()));
			if (!containsEnglish) {
				languageUuids.remove(languageUuids.size() - 1);
			}

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
