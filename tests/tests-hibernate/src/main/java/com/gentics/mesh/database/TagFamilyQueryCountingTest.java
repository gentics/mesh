package com.gentics.mesh.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
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
public class TagFamilyQueryCountingTest extends AbstractTagFamilyQueryCountingTest {

	protected final static Map<String, Consumer<TagFamilyResponse>> fieldAsserters = Map.of(
		"name", tagFamily -> {
			assertThat(tagFamily.getName()).as("TagFamily name").isNotEmpty();
		},
		"editor", tagFamily -> {
			assertThat(tagFamily.getEditor()).as("TagFamily editor").isNotNull();
		},
		"edited", tagFamily -> {
			assertThat(tagFamily.getEdited()).as("TagFamily edited").isNotEmpty();
		},
		"creator", tagFamily -> {
			assertThat(tagFamily.getCreator()).as("TagFamily creator").isNotNull();
		},
		"created", tagFamily -> {
			assertThat(tagFamily.getCreated()).as("TagFamily created").isNotEmpty();
		},
		"perms", tagFamily -> {
			assertThat(tagFamily.getPermissions()).as("TagFamily permissions").isNotNull();
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

	@Test
	public void testGetAll() {
		TagFamilyListResponse tagFamilyList = doTest(() -> client().findTagFamilies(PROJECT_NAME,
				new GenericParametersImpl().setETag(etag).setFields("uuid", field)), 9, 1);

		assertThat(tagFamilyList.getData()).as("List of fetched tag families").hasSize(totalNumTagFamilies);

		for (TagFamilyResponse tagFamily : tagFamilyList.getData()) {
			fieldAsserters.get(field).accept(tagFamily);
		}
	}

	@Test
	public void testGetPage() {
		TagFamilyListResponse tagFamilyList = doTest(() -> client().findTagFamilies(PROJECT_NAME,
				new GenericParametersImpl().setETag(etag).setFields("uuid", field),
				new PagingParametersImpl().setPerPage(10L)), 9, 1);

		assertThat(tagFamilyList.getData()).as("List of fetched tag families").hasSize(10);

		for (TagFamilyResponse tagFamily : tagFamilyList.getData()) {
			fieldAsserters.get(field).accept(tagFamily);
		}
	}
}
