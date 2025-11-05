package com.gentics.mesh.database;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchListResponse;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
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
public class BranchQueryCountingTest extends AbstractCountingTest {
	public final static String PROJECT_NAME = "testproject";

	public final static String TAG_FAMILY_NAME = "tagfamily";

	public final static int NUM_BRANCHES = 53;

	public final static int NUM_TAGS_PER_BRANCH = 10;

	protected static int totalNumBranches = NUM_BRANCHES;

	protected static Set<String> initialBranchUuids = new HashSet<>();

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

	@Before
	public void setup() {
		if (getTestContext().needsSetup()) {
			// create project
			ProjectCreateRequest projectCreateRequest = new ProjectCreateRequest();
			projectCreateRequest.setName(PROJECT_NAME);
			projectCreateRequest.setHostname(PROJECT_NAME);
			projectCreateRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
			call(() -> client().createProject(projectCreateRequest));
			BranchListResponse initialBranches = call(() -> client().findBranches(PROJECT_NAME));
			initialBranchUuids.addAll(initialBranches.getData().stream().map(BranchResponse::getUuid).toList());
			totalNumBranches += initialBranches.getMetainfo().getTotalCount();

			// create tag family
			TagFamilyResponse tagFamily = createTagFamily(PROJECT_NAME, TAG_FAMILY_NAME);

			// create tags
			Set<String> tagUuids = new HashSet<>();
			for (int i = 0; i < NUM_TAGS_PER_BRANCH; i++) {
				TagResponse tag = createTag(PROJECT_NAME, tagFamily.getUuid(), "tag_%d".formatted(i));
				tagUuids.add(tag.getUuid());
			}

			// create branches
			for (int i = 0; i < NUM_BRANCHES; i++) {
				createBranch("");
				BranchCreateRequest request = new BranchCreateRequest();
				request.setName("branch_%d".formatted(i));
				request.setHostname("branch_%d".formatted(i));
				request.setLatest(true);
				BranchResponse branch = call(() -> client().createBranch(PROJECT_NAME, request));

				for (String tagUuid : tagUuids) {
					call(() -> client().addTagToBranch(PROJECT_NAME, branch.getUuid(), tagUuid));
				}
			}
		}

		// clear the cache
		adminCall(() -> client().clearCache());
	}

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
				new PagingParametersImpl().setPerPage(10L)), 9, 1);

		assertThat(branchList.getData()).as("List of fetched branches").hasSize(10);

		for (BranchResponse branch : branchList.getData()) {
			if (!initialBranchUuids.contains(branch.getUuid())) {
				fieldAsserters.get(field).accept(branch);
			}
		}
	}
}
