package com.gentics.mesh.database;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
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

import com.gentics.mesh.core.rest.branch.BranchListResponse;
import com.gentics.mesh.core.rest.branch.BranchResponse;
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
public class BranchQueryCountingTest extends AbstractBranchQueryCountingTest {

	/**
	 * Test cases with asserters
	 */
	protected final static Map<String, Consumer<BranchResponse>> fieldAsserters = Map.of(
		"name", branch -> {
			assertThat(branch.getName()).as("Branch name").isNotEmpty();
		},
		"hostname", branch -> {
			assertThat(branch.getHostname()).as("Branch hostname").isNotEmpty();
		},
		"tags", branch -> {
			assertThat(branch.getTags()).as("Branch tags").isNotNull().hasSize(NUM_TAGS_PER_BRANCH);
		},
		"editor", branch -> {
			assertThat(branch.getEditor()).as("Branch editor").isNotNull();
		},
		"edited", branch -> {
			assertThat(branch.getEdited()).as("Branch edited").isNotEmpty();
		},
		"creator", branch -> {
			assertThat(branch.getCreator()).as("Branch creator").isNotNull();
		},
		"created", branch -> {
			assertThat(branch.getCreated()).as("Branch created").isNotEmpty();
		},
		"perms", branch -> {
			assertThat(branch.getPermissions()).as("Branch permissions").isNotNull();
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
		BranchListResponse branchList = doTest(() -> client().findBranches(PROJECT_NAME,
				new GenericParametersImpl().setETag(etag).setFields("uuid", field)), 9, 1);

		assertThat(branchList.getData()).as("List of fetched branches").hasSize(totalNumBranches);

		for (BranchResponse branch : branchList.getData()) {
			if (!initialBranchUuids.contains(branch.getUuid())) {
				fieldAsserters.get(field).accept(branch);
			}
		}
	}

	@Test
	public void testGetPage() {
		BranchListResponse branchList = doTest(() -> client().findBranches(PROJECT_NAME,
				new GenericParametersImpl().setETag(etag).setFields("uuid", field),
				new PagingParametersImpl().setPerPage(10L)), 10, 1);

		assertThat(branchList.getData()).as("List of fetched branches").hasSize(10);

		for (BranchResponse branch : branchList.getData()) {
			if (!initialBranchUuids.contains(branch.getUuid())) {
				fieldAsserters.get(field).accept(branch);
			}
		}
	}
}
