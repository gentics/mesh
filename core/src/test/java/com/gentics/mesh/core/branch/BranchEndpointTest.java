package com.gentics.mesh.core.branch;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.COMPLETED;
import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.INITIAL_RELEASE_NAME;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.test.util.MeshAssert.failingLatch;
import static com.gentics.mesh.test.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchListResponse;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.branch.BranchUpdateRequest;
import com.gentics.mesh.core.rest.branch.info.BranchInfoMicroschemaList;
import com.gentics.mesh.core.rest.branch.info.BranchInfoSchemaList;
import com.gentics.mesh.core.rest.branch.info.BranchMicroschemaInfo;
import com.gentics.mesh.core.rest.branch.info.BranchSchemaInfo;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.parameter.impl.SchemaUpdateParametersImpl;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicRestTestcases;
import com.gentics.mesh.test.util.TestUtils;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class BranchEndpointTest extends AbstractMeshTest implements BasicRestTestcases {

	@Before
	public void addAdminPerms() {
		// Grant admin perms. Otherwise we can't check the jobs
		tx(() -> group().addRole(roles().get("admin")));
	}

	@Override
	public void testUpdateMultithreaded() throws Exception {
		// TODO Auto-generated method stub
	}

	@Test
	@Override
	public void testReadByUuidMultithreaded() throws Exception {
		int nJobs = 200;
		try (Tx tx = tx()) {
			String projectName = PROJECT_NAME;
			String uuid = initialBranchUuid();

			Set<MeshResponse<?>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(client().findBranchByUuid(projectName, uuid).invoke());
			}

			for (MeshResponse<?> future : set) {
				latchFor(future);
				assertSuccess(future);
			}
		}
	}

	@Override
	public void testDeleteByUUIDMultithreaded() throws Exception {
		// Branches can't be deleted
	}

	@Test
	@Override
	@Ignore
	public void testCreateMultithreaded() throws Exception {
		String branchName = "Branch V";
		try (Tx tx = tx()) {
			Project project = project();
			int nJobs = 100;

			Set<MeshResponse<BranchResponse>> responseFutures = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				BranchCreateRequest request = new BranchCreateRequest();
				request.setName(branchName + i);
				MeshResponse<BranchResponse> future = client().createBranch(PROJECT_NAME, request).invoke();
				responseFutures.add(future);
			}

			Set<String> uuids = new HashSet<>();
			uuids.add(initialBranchUuid());
			for (MeshResponse<BranchResponse> future : responseFutures) {
				latchFor(future);
				assertSuccess(future);

				assertThat(future.result()).as("Response").isNotNull();
				assertThat(uuids).as("Existing uuids").doesNotContain(future.result().getUuid());
				uuids.add(future.result().getUuid());
			}

			// All branches must form a chain
			Set<String> foundBranches = new HashSet<>();
			Branch previousBranch = null;
			Branch branch = project.getInitialBranch();

			do {
				assertThat(branch).as("Branch").isNotNull().hasPrevious(previousBranch);
				assertThat(foundBranches).as("Existing uuids").doesNotContain(branch.getUuid());
				foundBranches.add(branch.getUuid());
				previousBranch = branch;
				branch = branch.getNextBranch();
			} while (branch != null);

			assertThat(previousBranch).as("Latest Branch").matches(project.getLatestBranch());
			assertThat(foundBranches).as("Found Branchs").containsOnlyElementsOf(uuids);
		}
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		int nJobs = 200;
		try (Tx tx = tx()) {
			Set<MeshResponse<BranchResponse>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(client().findBranchByUuid(PROJECT_NAME, initialBranchUuid()).invoke());
			}
			for (MeshResponse<BranchResponse> future : set) {
				latchFor(future);
				assertSuccess(future);
			}
		}
	}

	@Test
	@Override
	public void testCreate() throws Exception {
		String branchName = "Branch V1";

		BranchCreateRequest request = new BranchCreateRequest();
		request.setName(branchName);

		waitForJobs(() -> {
			BranchResponse response = call(() -> client().createBranch(PROJECT_NAME, request));
			assertThat(response).as("Branch Response").isNotNull().hasName(branchName).isActive().isNotMigrated();
		}, COMPLETED, 1);

		BranchListResponse branches = call(() -> client().findBranches(PROJECT_NAME, new PagingParametersImpl().setPerPage(Integer.MAX_VALUE)));
		branches.getData().forEach(branch -> assertThat(branch).as("Branch " + branch.getName()).isMigrated());
	}

	@Override
	public void testCreateWithNoPerm() throws Exception {
		String branchName = "Branch V1";
		try (Tx tx = tx()) {
			Project project = project();
			role().grantPermissions(project, READ_PERM);
			role().revokePermissions(project, UPDATE_PERM);
			tx.success();
		}
		BranchCreateRequest request = new BranchCreateRequest();
		request.setName(branchName);
		call(() -> client().createBranch(PROJECT_NAME, request), FORBIDDEN, "error_missing_perm", projectUuid() + "/" + PROJECT_NAME);
	}

	@Test
	@Override
	public void testCreateWithUuid() throws Exception {
		String branchName = "Branch V1";
		String uuid = UUIDUtil.randomUUID();

		BranchCreateRequest request = new BranchCreateRequest();
		request.setName(branchName);

		waitForJobs(() -> {
			BranchResponse response = call(() -> client().createBranch(PROJECT_NAME, uuid, request));
			assertThat(response).as("Branch Response").isNotNull().hasName(branchName).isActive().isNotMigrated().hasUuid(uuid);
		}, COMPLETED, 1);

		BranchListResponse branchs = call(() -> client().findBranches(PROJECT_NAME, new PagingParametersImpl().setPerPage(Integer.MAX_VALUE)));
		branchs.getData().forEach(branch -> assertThat(branch).as("Branch " + branch.getName()).isMigrated());
	}

	@Test
	@Override
	public void testCreateWithDuplicateUuid() throws Exception {
		String branchName = "Branch V1";
		try (Tx tx = db().tx()) {
			Project project = project();
			String uuid = user().getUuid();

			BranchUpdateRequest request = new BranchUpdateRequest();
			request.setName(branchName);

			call(() -> client().updateBranch(project.getName(), uuid, request), INTERNAL_SERVER_ERROR, "error_internal");
		}
	}

	@Test
	public void testCreateWithoutName() throws Exception {
		call(() -> client().createBranch(PROJECT_NAME, new BranchCreateRequest()), BAD_REQUEST, "branch_missing_name");
	}

	@Test
	public void testCreateWithConflictingName1() throws Exception {
		BranchCreateRequest request = new BranchCreateRequest();
		request.setName(PROJECT_NAME);
		call(() -> client().createBranch(PROJECT_NAME, request), CONFLICT, "branch_conflicting_name", PROJECT_NAME);
	}

	@Test
	public void testCreateWithConflictingName2() throws Exception {
		BranchCreateRequest request = new BranchCreateRequest();
		String branchName = "New Branch";
		request.setName(branchName);
		CountDownLatch latch = TestUtils.latchForMigrationCompleted(client());
		waitForJobs(() -> {
			call(() -> client().createBranch(PROJECT_NAME, request));
		}, COMPLETED, 1);
		failingLatch(latch);
		call(() -> client().createBranch(PROJECT_NAME, request), CONFLICT, "branch_conflicting_name", branchName);
	}

	@Test
	public void testCreateWithConflictingName3() throws Exception {
		String branchName = "New Branch";
		String newProjectName = "otherproject";

		// 1. Create a new branch
		CountDownLatch latch = TestUtils.latchForMigrationCompleted(client());
		BranchCreateRequest request = new BranchCreateRequest();
		request.setName(branchName);
		waitForJobs(() -> {
			call(() -> client().createBranch(PROJECT_NAME, request));
		}, COMPLETED, 1);
		failingLatch(latch);

		// 2. Create a new project and branch and use the same name. This is allowed and should not raise any error.
		latch = TestUtils.latchForMigrationCompleted(client());
		ProjectCreateRequest createProject = new ProjectCreateRequest();
		createProject.setName(newProjectName);
		createProject.setSchema(new SchemaReferenceImpl().setName("folder"));
		call(() -> client().createProject(createProject));
		waitForJobs(() -> {
			call(() -> client().createBranch(newProjectName, request));
		}, COMPLETED, 1);
		failingLatch(latch);
	}

	@Override
	public void testCreateReadDelete() throws Exception {
		// TODO Auto-generated method stub

	}

	@Test
	public void testCreateWithHostname() {
		String branchName = "MyBranch";
		String hostname = "my.host";
		BranchCreateRequest request = new BranchCreateRequest();
		request.setName(branchName);
		request.setHostname(hostname);
		request.setSsl(true);
		waitForJobs(() -> {
			BranchResponse response = call(() -> client().createBranch(PROJECT_NAME, request));
			assertThat(response).as("Created branch").hasName(branchName).hasHostname(hostname).hasSSL(true);
		}, COMPLETED, 1);
	}

	@Test
	public void testCreateWithSsl() {
		String branchName = "MyBranch";
		Boolean ssl = true;

		BranchCreateRequest request = new BranchCreateRequest();
		request.setName(branchName);
		request.setSsl(ssl);

		waitForJobs(() -> {
			BranchResponse response = call(() -> client().createBranch(PROJECT_NAME, request));
			assertThat(response).as("Created branch").hasName(branchName).hasSsl(ssl);
		}, COMPLETED, 1);
	}

	@Test
	@Override
	public void testReadByUUID() throws Exception {

		List<Pair<String, String>> branchInfo = new ArrayList<>();

		try (Tx tx = tx()) {
			Project project = project();
			Branch initialBranch = project.getInitialBranch();
			branchInfo.add(Pair.of(initialBranch.getUuid(), initialBranch.getName()));

			Branch firstBranch = project.getBranchRoot().create("One", user());
			branchInfo.add(Pair.of(firstBranch.getUuid(), firstBranch.getName()));

			Branch secondBranch = project.getBranchRoot().create("Two", user());
			branchInfo.add(Pair.of(secondBranch.getUuid(), secondBranch.getName()));

			Branch thirdBranch = project.getBranchRoot().create("Three", user());
			branchInfo.add(Pair.of(thirdBranch.getUuid(), thirdBranch.getName()));

			tx.success();
		}

		for (Pair<String, String> info : branchInfo) {
			BranchResponse response = call(() -> client().findBranchByUuid(PROJECT_NAME, info.getKey()));
			assertThat(response).isNotNull().hasName(info.getValue()).hasUuid(info.getKey()).isActive();
		}

	}

	@Test
	public void testReadByBogusUUID() throws Exception {
		call(() -> client().findBranchByUuid(PROJECT_NAME, "bogus"), NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	@Override
	public void testReadByUuidWithRolePerms() {
		String roleUuid = db().tx(() -> role().getUuid());
		BranchResponse response = call(() -> client().findBranchByUuid(PROJECT_NAME, initialBranchUuid(), new RolePermissionParametersImpl()
			.setRoleUuid(roleUuid)));
		assertThat(response.getRolePerms()).hasPerm(READ, CREATE, UPDATE, DELETE);
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		try (Tx tx = tx()) {
			role().revokePermissions(project().getInitialBranch(), READ_PERM);
			tx.success();
		}
		call(() -> client().findBranchByUuid(PROJECT_NAME, initialBranchUuid()), FORBIDDEN, "error_missing_perm", initialBranchUuid());
	}

	@Test
	@Override
	public void testReadMultiple() throws Exception {
		Branch initialBranch;
		Branch firstBranch;
		Branch thirdBranch;
		Branch secondBranch;

		try (Tx tx = tx()) {
			Project project = project();
			initialBranch = project.getInitialBranch();
			firstBranch = project.getBranchRoot().create("One", user());
			secondBranch = project.getBranchRoot().create("Two", user());
			thirdBranch = project.getBranchRoot().create("Three", user());
			tx.success();
		}

		try (Tx tx = tx()) {
			ListResponse<BranchResponse> responseList = call(() -> client().findBranches(PROJECT_NAME));
			InternalActionContext ac = mockActionContext();

			assertThat(responseList).isNotNull();
			assertThat(responseList.getData()).usingElementComparatorOnFields("uuid", "name").containsOnly(initialBranch.transformToRestSync(ac, 0),
				firstBranch.transformToRestSync(ac, 0), secondBranch.transformToRestSync(ac, 0), thirdBranch.transformToRestSync(ac, 0));
		}
	}

	@Test
	public void testReadMultipleWithRestrictedPermissions() throws Exception {
		Project project = project();
		Branch initialBranch = tx(() -> initialBranch());

		Branch firstBranch;
		Branch secondBranch;
		Branch thirdBranch;

		try (Tx tx = tx()) {
			firstBranch = project.getBranchRoot().create("One", user());
			secondBranch = project.getBranchRoot().create("Two", user());
			thirdBranch = project.getBranchRoot().create("Three", user());
			tx.success();
		}
		try (Tx tx = tx()) {
			role().revokePermissions(firstBranch, READ_PERM);
			role().revokePermissions(thirdBranch, READ_PERM);
			tx.success();
		}

		ListResponse<BranchResponse> responseList = call(() -> client().findBranches(PROJECT_NAME));

		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			assertThat(responseList).isNotNull();
			assertThat(responseList.getData()).usingElementComparatorOnFields("uuid", "name").containsOnly(initialBranch.transformToRestSync(ac, 0),
				secondBranch.transformToRestSync(ac, 0));
		}
	}

	@Test
	@Override
	public void testUpdate() throws Exception {
		String newName = "New Branch Name";
		String anotherNewName = "Another New Branch Name";
		try (Tx tx = tx()) {

			// change name
			BranchUpdateRequest request1 = new BranchUpdateRequest();
			request1.setName(newName);
			BranchResponse response = call(() -> client().updateBranch(PROJECT_NAME, initialBranchUuid(), request1));
			assertThat(response).as("Updated Branch").isNotNull().hasName(newName).isActive();

			// change active
			BranchUpdateRequest request2 = new BranchUpdateRequest();
			// request2.setActive(false);
			response = call(() -> client().updateBranch(PROJECT_NAME, initialBranchUuid(), request2));
			assertThat(response).as("Updated Branch").isNotNull().hasName(newName).isInactive();

			// change active and name
			BranchUpdateRequest request3 = new BranchUpdateRequest();
			// request3.setActive(true);
			request3.setName(anotherNewName);
			response = call(() -> client().updateBranch(PROJECT_NAME, initialBranchUuid(), request3));
			assertThat(response).as("Updated Branch").isNotNull().hasName(anotherNewName).isActive();
		}
	}

	@Test
	public void testUpdateWithNameConflict() throws Exception {
		String newName = "New Branch Name";
		try (Tx tx = tx()) {
			project().getBranchRoot().create(newName, user());
			tx.success();
		}
		BranchUpdateRequest request = new BranchUpdateRequest();
		request.setName(newName);
		call(() -> client().updateBranch(PROJECT_NAME, initialBranchUuid(), request), CONFLICT, "branch_conflicting_name", newName);

	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		try (Tx tx = tx()) {
			role().revokePermissions(project().getInitialBranch(), UPDATE_PERM);
			tx.success();
		}
		BranchUpdateRequest request = new BranchUpdateRequest();
		// request.setActive(false);
		call(() -> client().updateBranch(PROJECT_NAME, initialBranchUuid(), request), FORBIDDEN, "error_missing_perm", initialBranchUuid());
	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {
		BranchUpdateRequest request = new BranchUpdateRequest();
		// request.setActive(false);
		call(() -> client().createBranch(PROJECT_NAME, "bogus", request), BAD_REQUEST, "error_illegal_uuid", "bogus");
	}

	@Test
	public void testUpdateHostname() {
		String hostname = "new.hostname";

		BranchUpdateRequest request = new BranchUpdateRequest();
		request.setHostname(hostname);
		BranchResponse response = call(() -> client().updateBranch(PROJECT_NAME, initialBranchUuid(), request));
		assertThat(response).as("Updated branch").hasHostname(hostname);
	}

	@Test
	public void testUpdateSsl() {
		Boolean ssl = true;

		BranchUpdateRequest request = new BranchUpdateRequest();
		request.setSsl(ssl);
		BranchResponse response = call(() -> client().updateBranch(PROJECT_NAME, initialBranchUuid(), request));
		assertThat(response).as("Updated branch").hasSsl(ssl);
	}

	@Override
	public void testDeleteByUUID() throws Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		// TODO Auto-generated method stub
	}

	// Tests for assignment of schema versions to branchs

	@Test
	public void testReadSchemaVersions() throws Exception {
		try (Tx tx = tx()) {
			BranchInfoSchemaList list = call(() -> client().getBranchSchemaVersions(PROJECT_NAME, initialBranchUuid()));
			BranchSchemaInfo content = new BranchSchemaInfo(schemaContainer("content").getLatestVersion().transformToReference());
			BranchSchemaInfo folder = new BranchSchemaInfo(schemaContainer("folder").getLatestVersion().transformToReference());
			BranchSchemaInfo binaryContent = new BranchSchemaInfo(schemaContainer("binary_content").getLatestVersion().transformToReference());

			assertThat(list.getSchemas()).as("branch schema versions").usingElementComparatorOnFields("name", "uuid", "version").containsOnly(
				content, folder, binaryContent);
		}
	}

	@Test
	public void testAssignSchemaVersionViaSchemaUpdate() throws Exception {
		try (Tx tx = tx()) {
			// create version 1 of a schema
			SchemaResponse schema = createSchema("schemaname");

			// Assign schema to project
			call(() -> client().assignSchemaToProject(PROJECT_NAME, schema.getUuid()));

			// Generate version 2
			waitForJobs(() -> {
				updateSchema(schema.getUuid(), "newschemaname");
			}, COMPLETED, 1);

			// Assert that version 2 is assigned to branch
			BranchInfoSchemaList infoList = call(() -> client().getBranchSchemaVersions(PROJECT_NAME, initialBranchUuid()));
			assertThat(infoList.getSchemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new BranchSchemaInfo().setName("newschemaname").setUuid(schema.getUuid()).setVersion("2.0"));

			// Generate version 3
			updateSchema(schema.getUuid(), "anothernewschemaname", new SchemaUpdateParametersImpl().setUpdateAssignedBranches(false));

			// Assert that version 2 is still assigned to branch
			infoList = call(() -> client().getBranchSchemaVersions(PROJECT_NAME, initialBranchUuid()));
			assertThat(infoList.getSchemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new BranchSchemaInfo().setName("newschemaname").setUuid(schema.getUuid()).setVersion("2.0"));

			// Generate version 3 which should not be auto assigned to the project branch
			updateSchema(schema.getUuid(), "anothernewschemaname", new SchemaUpdateParametersImpl().setUpdateAssignedBranches(false).setBranchNames(
				INITIAL_RELEASE_NAME));

			// Assert that version 2 is still assigned to the branch
			infoList = call(() -> client().getBranchSchemaVersions(PROJECT_NAME, initialBranchUuid()));
			assertThat(infoList.getSchemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new BranchSchemaInfo().setName("newschemaname").setUuid(schema.getUuid()).setVersion("2.0"));

			// Generate version 4
			waitForJobs(() -> {
				updateSchema(schema.getUuid(), "anothernewschemaname2", new SchemaUpdateParametersImpl().setUpdateAssignedBranches(true));
			}, COMPLETED, 1);

			// Assert that version 4 is assigned to the branch
			infoList = call(() -> client().getBranchSchemaVersions(PROJECT_NAME, initialBranchUuid()));

			assertThat(infoList.getSchemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new BranchSchemaInfo().setName("anothernewschemaname2").setUuid(schema.getUuid()).setVersion("4.0"));

			// Generate version 5
			updateSchema(schema.getUuid(), "anothernewschemaname3", new SchemaUpdateParametersImpl().setUpdateAssignedBranches(true).setBranchNames(
				"bla", "bogus", "moped"));

			// Assert that version 4 is still assigned to the branch since non of the names matches the project branch
			infoList = call(() -> client().getBranchSchemaVersions(PROJECT_NAME, initialBranchUuid()));
			assertThat(infoList.getSchemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new BranchSchemaInfo().setName("anothernewschemaname2").setUuid(schema.getUuid()).setVersion("4.0"));

		}
	}

	@Test
	public void testAssignSchemaVersion() throws Exception {
		// Grant admin perm
		tx(() -> group().addRole(roles().get("admin")));

		// create version 1 of a schema
		SchemaResponse schema = createSchema("schemaname");

		// assign schema to project
		call(() -> client().assignSchemaToProject(PROJECT_NAME, schema.getUuid()));

		// generate version 2
		updateSchema(schema.getUuid(), "newschemaname", new SchemaUpdateParametersImpl().setUpdateAssignedBranches(false));

		// generate version 3
		updateSchema(schema.getUuid(), "anothernewschemaname", new SchemaUpdateParametersImpl().setUpdateAssignedBranches(false));

		// check that version 1 is assigned to branch
		BranchInfoSchemaList list = call(() -> client().getBranchSchemaVersions(PROJECT_NAME, initialBranchUuid()));
		assertThat(list.getSchemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
			new BranchSchemaInfo().setName("schemaname").setUuid(schema.getUuid()).setVersion("1.0"));

		// assign version 2 to the branch
		BranchInfoSchemaList info = new BranchInfoSchemaList();
		info.getSchemas().add(new BranchSchemaInfo().setUuid(schema.getUuid()).setVersion("2.0"));
		waitForJobs(() -> {
			call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, initialBranchUuid(), info));
		}, COMPLETED, 1);

		JobListResponse jobList = call(() -> client().findJobs());
		JobResponse job = jobList.getData().stream().filter(j -> j.getProperties().get("schemaUuid").equals(schema.getUuid())).findAny().get();

		BranchInfoSchemaList schemaList = call(() -> client().getBranchSchemaVersions(PROJECT_NAME, initialBranchUuid()));
		BranchSchemaInfo schemaInfo = schemaList.getSchemas().stream().filter(s -> s.getUuid().equals(schema.getUuid())).findFirst().get();
		assertEquals(COMPLETED, schemaInfo.getMigrationStatus());
		assertEquals(job.getUuid(), schemaInfo.getJobUuid());

		list = call(() -> client().getBranchSchemaVersions(PROJECT_NAME, initialBranchUuid()));
		assertThat(list.getSchemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
			new BranchSchemaInfo().setName("newschemaname").setUuid(schema.getUuid()).setVersion("2.0"));
	}

	@Test
	public void testAssignBogusSchemaVersion() throws Exception {
		call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, initialBranchUuid(), new SchemaReferenceImpl().setName("content").setVersion(
			"4711.0")), BAD_REQUEST, "error_schema_reference_not_found", "content", "-", "4711.0");
	}

	@Test
	public void testAssignBogusSchemaUuid() throws Exception {
		call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, initialBranchUuid(), new SchemaReferenceImpl().setUuid("bogusuuid").setVersion(
			"1.0")), BAD_REQUEST, "error_schema_reference_not_found", "-", "bogusuuid", "1.0");
	}

	@Test
	public void testAssignBogusSchemaName() throws Exception {
		call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, initialBranchUuid(), new SchemaReferenceImpl().setName("bogusname").setVersion(
			"1.0")), BAD_REQUEST, "error_schema_reference_not_found", "bogusname", "-", "1.0");
	}

	@Test
	public void testAssignUnassignedSchemaVersion() throws Exception {
		final String schemaName = "schemaname";
		try (Tx tx = tx()) {
			createSchema(schemaName);
			tx.success();
		}

		call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, initialBranchUuid(), new SchemaReferenceImpl().setName(schemaName).setVersion(
			"1.0")), BAD_REQUEST, "error_schema_reference_not_found", schemaName, "-", "1.0");
	}

	@Test
	public void testAssignOlderSchemaVersion() throws Exception {
		String schemaUuid;
		try (Tx tx = tx()) {
			// create version 1 of a schema
			SchemaResponse schema = createSchema("schemaname");
			schemaUuid = schema.getUuid();
			// generate version 2
			updateSchema(schema.getUuid(), "newschemaname");

			// assign schema to project
			call(() -> client().assignSchemaToProject(PROJECT_NAME, schema.getUuid()));
			tx.success();
		}
		// try to downgrade schema version
		call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, initialBranchUuid(), new SchemaReferenceImpl().setUuid(schemaUuid).setVersion(
			"1.0")), BAD_REQUEST, "branch_error_downgrade_schema_version", "schemaname", "2.0", "1.0");

	}

	@Test
	public void testAssignSchemaVersionNoPermission() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();
			role().revokePermissions(project.getInitialBranch(), UPDATE_PERM);
			tx.success();
		}

		call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, initialBranchUuid(), new SchemaReferenceImpl().setName("content").setVersion(
			"1.0")), FORBIDDEN, "error_missing_perm", initialBranchUuid());
	}

	@Test
	public void testAssignLatestSchemaVersion() throws Exception {
		String schemaUuid;
		try (Tx tx = tx()) {
			// create version 1 of a schema
			SchemaResponse schema = createSchema("schemaname");
			schemaUuid = schema.getUuid();

			// assign schema to project
			call(() -> client().assignSchemaToProject(PROJECT_NAME, schema.getUuid()));

			// generate version 2
			updateSchema(schema.getUuid(), "newschemaname", new SchemaUpdateParametersImpl().setUpdateAssignedBranches(false));

			// generate version 3
			updateSchema(schema.getUuid(), "anothernewschemaname", new SchemaUpdateParametersImpl().setUpdateAssignedBranches(false));
			tx.success();
		}

		// check that version 1 is assigned to branch
		BranchInfoSchemaList list = call(() -> client().getBranchSchemaVersions(PROJECT_NAME, initialBranchUuid()));
		assertThat(list.getSchemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
			new BranchSchemaInfo().setName("schemaname").setUuid(schemaUuid).setVersion("1.0"));

		// assign latest version to the branch
		BranchInfoSchemaList info = new BranchInfoSchemaList();
		info.getSchemas().add(new BranchSchemaInfo().setUuid(schemaUuid));
		call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, initialBranchUuid(), info));

		// assert
		list = call(() -> client().getBranchSchemaVersions(PROJECT_NAME, initialBranchUuid()));
		assertThat(list.getSchemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
			new BranchSchemaInfo().setName("anothernewschemaname").setUuid(schemaUuid).setVersion("3.0"));

	}

	// Tests for assignment of microschema versions to branchs

	@Test
	public void testReadMicroschemaVersions() throws Exception {
		BranchMicroschemaInfo vcard = new BranchMicroschemaInfo(db().tx(() -> microschemaContainer("vcard").getLatestVersion()
			.transformToReference()));
		BranchMicroschemaInfo captionedImage = new BranchMicroschemaInfo(db().tx(() -> microschemaContainer("captionedImage").getLatestVersion()
			.transformToReference()));
		BranchInfoMicroschemaList list = call(() -> client().getBranchMicroschemaVersions(PROJECT_NAME, initialBranchUuid()));
		assertThat(list.getMicroschemas()).as("branch microschema versions").usingElementComparatorOnFields("name", "uuid", "version").containsOnly(
			vcard, captionedImage);
	}

	@Test
	public void testAssignMicroschemaVersion() throws Exception {
		try (Tx tx = tx()) {
			// create version 1 of a microschema
			MicroschemaResponse microschema = createMicroschema("microschemaname");

			// assign microschema to project
			call(() -> client().assignMicroschemaToProject(PROJECT_NAME, microschema.getUuid()));

			// generate version 2
			updateMicroschema(microschema.getUuid(), "newmicroschemaname", new SchemaUpdateParametersImpl().setUpdateAssignedBranches(false));

			// generate version 3
			updateMicroschema(microschema.getUuid(), "anothernewmicroschemaname", new SchemaUpdateParametersImpl().setUpdateAssignedBranches(false));

			// check that version 1 is assigned to branch
			BranchInfoMicroschemaList list = call(() -> client().getBranchMicroschemaVersions(PROJECT_NAME, initialBranchUuid()));
			assertThat(list.getMicroschemas()).as("Initial microschema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new BranchMicroschemaInfo(new MicroschemaReferenceImpl().setName("microschemaname").setUuid(microschema.getUuid()).setVersion(
					"1.0")));

			BranchInfoMicroschemaList info = new BranchInfoMicroschemaList();
			info.add(new MicroschemaReferenceImpl().setUuid(microschema.getUuid()).setVersion("2.0"));

			// assign version 2 to the branch
			waitForJobs(() -> {
				call(() -> client().assignBranchMicroschemaVersions(PROJECT_NAME, initialBranchUuid(), info));
			}, COMPLETED, 1);

			// assert
			list = call(() -> client().getBranchMicroschemaVersions(PROJECT_NAME, initialBranchUuid()));
			assertThat(list.getMicroschemas()).as("Initial microschema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new BranchMicroschemaInfo(new MicroschemaReferenceImpl().setName("newmicroschemaname").setUuid(microschema.getUuid()).setVersion(
					"2.0")));
		}
	}

	@Test
	public void testAssignMicroschemaVersionViaMicroschemaUpdate() throws Exception {
		try (Tx tx = tx()) {
			// create version 1 of a microschema
			MicroschemaResponse microschema = createMicroschema("microschemaname");

			// assign microschema to project
			call(() -> client().assignMicroschemaToProject(PROJECT_NAME, microschema.getUuid()));

			// generate version 2
			waitForJobs(() -> {
				updateMicroschema(microschema.getUuid(), "newmicroschemaname", new SchemaUpdateParametersImpl().setUpdateAssignedBranches(true));
			}, COMPLETED, 1);

			// generate version 3
			waitForJobs(() -> {
				updateMicroschema(microschema.getUuid(), "anothernewmicroschemaname");
			}, COMPLETED, 1);

			// check that version 3 is assigned to branch
			BranchInfoMicroschemaList list = call(() -> client().getBranchMicroschemaVersions(PROJECT_NAME, initialBranchUuid()));
			assertThat(list.getMicroschemas()).as("Initial microschema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new BranchMicroschemaInfo(new MicroschemaReferenceImpl().setName("anothernewmicroschemaname").setUuid(microschema.getUuid())
					.setVersion("3.0")));

			// assign version 2 to the branch
			// call(() -> getClient().assignBranchMicroschemaVersions(PROJECT_NAME, branchUuid(),
			// new MicroschemaReferenceList(Arrays.asList(new MicroschemaReference().setUuid(microschema.getUuid()).setVersion(2)))));

			// assert
			// list = call(() -> getClient().getBranchMicroschemaVersions(PROJECT_NAME, branchUuid()));
			// assertThat(list).as("Initial microschema versions").usingElementComparatorOnFields("name", "uuid", "version")
			// .contains(new MicroschemaReference().setName("newmicroschemaname").setUuid(microschema.getUuid()).setVersion(2));
		}
	}

	@Test
	public void testAssignBogusMicroschemaVersion() throws Exception {
		call(() -> client().assignBranchMicroschemaVersions(PROJECT_NAME, initialBranchUuid(), new MicroschemaReferenceImpl().setName("vcard")
			.setVersion("4711")), BAD_REQUEST, "error_microschema_reference_not_found", "vcard", "-", "4711");
	}

	@Test
	public void testAssignBogusMicroschemaUuid() throws Exception {
		call(() -> client().assignBranchMicroschemaVersions(PROJECT_NAME, initialBranchUuid(), new MicroschemaReferenceImpl().setUuid("bogusuuid")
			.setVersion("1.0")), BAD_REQUEST, "error_microschema_reference_not_found", "-", "bogusuuid", "1.0");
	}

	@Test
	public void testAssignBogusMicroschemaName() throws Exception {
		call(() -> client().assignBranchMicroschemaVersions(PROJECT_NAME, initialBranchUuid(), new MicroschemaReferenceImpl().setName("bogusname")
			.setVersion("1.0")), BAD_REQUEST, "error_microschema_reference_not_found", "bogusname", "-", "1.0");
	}

	@Test
	public void testAssignUnassignedMicroschemaVersion() throws Exception {
		try (Tx tx = tx()) {
			SchemaModel schema = createSchema("microschemaname");

			call(() -> client().assignBranchMicroschemaVersions(PROJECT_NAME, initialBranchUuid(), new MicroschemaReferenceImpl().setName(schema
				.getName()).setVersion(schema.getVersion())), BAD_REQUEST, "error_microschema_reference_not_found", schema.getName(), "-", schema
					.getVersion());
		}
	}

	@Test
	public void testAssignOlderMicroschemaVersion() throws Exception {

		// create version 1 of a microschema
		MicroschemaResponse microschema = createMicroschema("microschemaname");

		// generate version 2
		updateMicroschema(microschema.getUuid(), "newmicroschemaname");

		// assign microschema to project
		call(() -> client().assignMicroschemaToProject(PROJECT_NAME, microschema.getUuid()));

		// try to downgrade microschema version
		call(() -> client().assignBranchMicroschemaVersions(PROJECT_NAME, initialBranchUuid(), new MicroschemaReferenceImpl().setUuid(microschema
			.getUuid()).setVersion("1.0")), BAD_REQUEST, "branch_error_downgrade_microschema_version", "microschemaname", "2.0", "1.0");

	}

	@Test
	public void testAssignMicroschemaVersionNoPermission() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();
			role().revokePermissions(project.getInitialBranch(), UPDATE_PERM);
			tx.success();
		}
		call(() -> client().assignBranchMicroschemaVersions(PROJECT_NAME, initialBranchUuid(), new MicroschemaReferenceImpl().setName("vcard")
			.setVersion("1.0")), FORBIDDEN, "error_missing_perm", initialBranchUuid());
	}

	@Test
	public void testMigrateBranchSchemas() {
		// TODO Assign schema versions to the branch and delay the actual migration
		// https://github.com/gentics/mesh/issues/374
		call(() -> client().migrateBranchSchemas(PROJECT_NAME, initialBranchUuid()));
	}

	@Test
	public void testBranchMicroschemas() {
		// TODO Assign schema versions to the branch and delay the actual migration
		// https://github.com/gentics/mesh/issues/374
		call(() -> client().migrateBranchMicroschemas(PROJECT_NAME, initialBranchUuid()));
	}

	@Test
	public void testAssignLatestMicroschemaVersion() throws Exception {
		try (Tx tx = tx()) {
			// create version 1 of a microschema
			MicroschemaResponse microschema = createMicroschema("microschemaname");

			// Assign microschema to project
			call(() -> client().assignMicroschemaToProject(PROJECT_NAME, microschema.getUuid()));

			// Generate version 2
			waitForJobs(() -> {
				updateMicroschema(microschema.getUuid(), "newmicroschemaname");
			}, COMPLETED, 1);

			// Assert that version 2 is assigned to branch
			BranchInfoMicroschemaList list = call(() -> client().getBranchMicroschemaVersions(PROJECT_NAME, initialBranchUuid()));
			assertThat(list.getMicroschemas()).as("Initial microschema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new BranchMicroschemaInfo(new MicroschemaReferenceImpl().setName("newmicroschemaname").setUuid(microschema.getUuid()).setVersion(
					"2.0")));

			// Generate version 3 which should not be auto assigned to the project branch
			updateMicroschema(microschema.getUuid(), "anothernewschemaname", new SchemaUpdateParametersImpl().setUpdateAssignedBranches(false));

			// Assert that version 2 is still assigned to branch
			list = call(() -> client().getBranchMicroschemaVersions(PROJECT_NAME, initialBranchUuid()));
			assertThat(list.getMicroschemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new BranchMicroschemaInfo(new MicroschemaReferenceImpl().setName("newmicroschemaname").setUuid(microschema.getUuid()).setVersion(
					"2.0")));

			// Generate version 4
			updateMicroschema(microschema.getUuid(), "anothernewschemaname1", new SchemaUpdateParametersImpl().setUpdateAssignedBranches(true)
				.setBranchNames(INITIAL_RELEASE_NAME));

			// Assert that version 4 is assigned to the branch
			list = call(() -> client().getBranchMicroschemaVersions(PROJECT_NAME, initialBranchUuid()));
			assertThat(list.getMicroschemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new BranchMicroschemaInfo(new MicroschemaReferenceImpl().setName("anothernewschemaname1").setUuid(microschema.getUuid())
					.setVersion("4.0")));

			// Generate version 5
			waitForJobs(() -> {
				updateMicroschema(microschema.getUuid(), "anothernewschemaname2", new SchemaUpdateParametersImpl().setUpdateAssignedBranches(true));
			}, COMPLETED, 1);

			// Assert that version 5
			list = call(() -> client().getBranchMicroschemaVersions(PROJECT_NAME, initialBranchUuid()));
			assertThat(list.getMicroschemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new BranchMicroschemaInfo(new MicroschemaReferenceImpl().setName("anothernewschemaname2").setUuid(microschema.getUuid())
					.setVersion("5.0")));

			// Generate version 6
			updateMicroschema(microschema.getUuid(), "anothernewschemaname3", new SchemaUpdateParametersImpl().setUpdateAssignedBranches(true)
				.setBranchNames("bla", "bogus", "moped"));

			// Assert that version 4 is still assigned to the branch since non of the names matches the project branch
			list = call(() -> client().getBranchMicroschemaVersions(PROJECT_NAME, initialBranchUuid()));
			assertThat(list.getMicroschemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new BranchMicroschemaInfo(new MicroschemaReferenceImpl().setName("anothernewschemaname2").setUuid(microschema.getUuid())
					.setVersion("5.0")));
		}
	}
}
