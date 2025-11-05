package com.gentics.mesh.database;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.lang.LanguageListResponse;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.hibernate.util.QueryCounter;
import com.gentics.mesh.parameter.client.GenericParametersImpl;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.ProjectLoadParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.ResetTestDb;
import com.gentics.mesh.test.TestSize;

/**
 * Test cases which count the number of executed REST call queries.
 */
@MeshTestSetting(testSize = TestSize.PROJECT, monitoring = true, startServer = true, customOptionChanger = QueryCounter.EnableHibernateStatistics.class, resetBetweenTests = ResetTestDb.NEVER)
@RunWith(Parameterized.class)
public class ProjectQueryCountingTest extends AbstractCountingTest {
	public final static int NUM_PROJECTS = 53;

	public final static int NUM_LANGUAGES_PER_PROJECT = 8;

	protected static int totalNumProjects = NUM_PROJECTS;

	protected static Set<String> initialProjectUuids = new HashSet<>();

	protected final static Map<String, Consumer<ProjectResponse>> fieldAsserters = Map.of(
		"name", project -> {
			assertThat(project.getName()).as("Project name").isNotEmpty();
		},
//		"rootNode", project -> {
//			assertThat(project.getRootNode()).as("Project root node").isNotNull();
//		},
		"langs", project -> {
			assertThat(project.getLanguages()).as("Project languages").hasSize(NUM_LANGUAGES_PER_PROJECT);
		},
		"editor", project -> {
			assertThat(project.getEditor()).as("Project editor").isNotNull();
		},
		"edited", project -> {
			assertThat(project.getEdited()).as("Project edited").isNotEmpty();
		},
		"creator", project -> {
			assertThat(project.getCreator()).as("Project creator").isNotNull();
		},
		"created", project -> {
			assertThat(project.getCreated()).as("Project created").isNotEmpty();
		},
		"perms", project -> {
			assertThat(project.getPermissions()).as("Project permissions").isNotNull();
		}
	);

	@Parameters(name = "{index}: field {0}, etag {1}")
	public static Collection<Object[]> parameters() throws Exception {
		Collection<Object[]> data = new ArrayList<>();
		for (String field : fieldAsserters.keySet()) {
			for (Boolean etag : Arrays.asList(true, false)) {
				data.add(new Object[] {field, etag});
			}
		}
		return data;
	}

	@Parameter(0)
	public String field;

	@Parameter(1)
	public boolean etag;

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

	@Test
	public void testGetAll() {
		ProjectListResponse projectList = doTest(
				() -> client().findProjects(new GenericParametersImpl().setETag(etag).setFields("uuid", field),
						new ProjectLoadParametersImpl().setLangs("langs".equals(field))),
				7, 1);
		assertThat(projectList.getData()).as("List of fetched projects").hasSize(totalNumProjects);

		for (ProjectResponse project : projectList.getData()) {
			if (!initialProjectUuids.contains(project.getUuid())) {
				fieldAsserters.get(field).accept(project);
			}
		}
	}

	@Test
	public void testGetPage() {
		ProjectListResponse projectList = doTest(
				() -> client().findProjects(new GenericParametersImpl().setETag(etag).setFields("uuid", field),
						new PagingParametersImpl().setPerPage(10L),
						new ProjectLoadParametersImpl().setLangs("langs".equals(field))),
				7, 1);
		assertThat(projectList.getData()).as("List of fetched projects").hasSize(10);

		for (ProjectResponse project : projectList.getData()) {
			if (!initialProjectUuids.contains(project.getUuid())) {
				fieldAsserters.get(field).accept(project);
			}
		}
	}
}
