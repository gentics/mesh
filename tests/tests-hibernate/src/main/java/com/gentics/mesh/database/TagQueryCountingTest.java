package com.gentics.mesh.database;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.hibernate.util.QueryCounter;
import com.gentics.mesh.parameter.client.GenericParametersImpl;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.ResetTestDb;
import com.gentics.mesh.test.TestSize;

/**
 * Test cases which count the number of executed REST call queries.
 */
@MeshTestSetting(testSize = TestSize.PROJECT, monitoring = true, startServer = true, customOptionChanger = QueryCounter.EnableHibernateStatistics.class, resetBetweenTests = ResetTestDb.NEVER)
@RunWith(Parameterized.class)
public class TagQueryCountingTest extends AbstractCountingTest {
	public final static String PROJECT_NAME = "testproject";

	public final static String TAGFAMILY_NAME = "tagfamily";

	public final static int NUM_TAGS = 53;

	protected static int totalNumTags = NUM_TAGS;

	protected static String tagFamilyUuid;

	protected final static Map<String, Consumer<TagResponse>> fieldAsserters = Map.of(
		"name", tag -> {
			assertThat(tag.getName()).as("Tag name").isNotEmpty();
		},
		"tagFamily", tag -> {
			assertThat(tag.getTagFamily()).as("Tag TagFamily").isNotNull();
		},
		"editor", tag -> {
			assertThat(tag.getEditor()).as("Tag editor").isNotNull();
		},
		"edited", tag -> {
			assertThat(tag.getEdited()).as("Tag edited").isNotEmpty();
		},
		"creator", tag -> {
			assertThat(tag.getCreator()).as("Tag creator").isNotNull();
		},
		"created", tag -> {
			assertThat(tag.getCreated()).as("Tag created").isNotEmpty();
		},
		"perms", tag -> {
			assertThat(tag.getPermissions()).as("Tag permissions").isNotNull();
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
			// create project
			ProjectCreateRequest projectCreateRequest = new ProjectCreateRequest();
			projectCreateRequest.setName(PROJECT_NAME);
			projectCreateRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
			call(() -> client().createProject(projectCreateRequest));

			TagFamilyResponse tagFamily = createTagFamily(PROJECT_NAME, TAGFAMILY_NAME);
			tagFamilyUuid = tagFamily.getUuid();

			// create tags
			for (int i = 0; i < NUM_TAGS; i++) {
				createTag(PROJECT_NAME, tagFamilyUuid, "tag_%d".formatted(i));
			}
		}

		// clear the cache
		adminCall(() -> client().clearCache());
	}

	@Test
	public void testGetAll() {
		TagListResponse tagList = doTest(() -> client().findTags(PROJECT_NAME, tagFamilyUuid,
				new GenericParametersImpl().setETag(etag).setFields("uuid", field)), 7, 1);

		assertThat(tagList.getData()).as("List of fetched tags").hasSize(totalNumTags);

		for (TagResponse tag : tagList.getData()) {
			fieldAsserters.get(field).accept(tag);
		}
	}

	@Test
	public void testGetPage() {
		TagListResponse tagList = doTest(() -> client().findTags(PROJECT_NAME, tagFamilyUuid,
				new GenericParametersImpl().setETag(etag).setFields("uuid", field),
				new PagingParametersImpl().setPerPage(10L)), 7, 1);

		assertThat(tagList.getData()).as("List of fetched tags").hasSize(10);

		for (TagResponse tag : tagList.getData()) {
			fieldAsserters.get(field).accept(tag);
		}
	}
}
