package com.gentics.mesh.database;

import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;

import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchListResponse;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;

public abstract class AbstractBranchQueryCountingTest extends AbstractCountingTest {
	public final static String PROJECT_NAME = "testproject";

	public final static String TAG_FAMILY_NAME = "tagfamily";

	public final static int NUM_BRANCHES = 53;

	public final static int NUM_TAGS_PER_BRANCH = 10;

	protected static int totalNumBranches = NUM_BRANCHES;

	protected static Set<String> initialBranchUuids = new HashSet<>();

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
				BranchCreateRequest request = new BranchCreateRequest();
				request.setName("branch_%d".formatted(i));
				request.setHostname("branch_%d".formatted(i));
				request.setLatest(true);

				AtomicReference<String> branchUuid = new AtomicReference<>();
				waitForJobs(() -> {
					BranchResponse branch = call(() -> client().createBranch(PROJECT_NAME, request));
					branchUuid.set(branch.getUuid());
				}, COMPLETED, 1);

				for (String tagUuid : tagUuids) {
					call(() -> client().addTagToBranch(PROJECT_NAME, branchUuid.get(), tagUuid));
				}
			}
		}

		// clear the cache
		adminCall(() -> client().clearCache());
	}

}
