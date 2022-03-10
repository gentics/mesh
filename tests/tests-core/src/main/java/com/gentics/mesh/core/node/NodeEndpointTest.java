package com.gentics.mesh.core.node;

import static com.gentics.mesh.FieldUtil.createStringField;
import static com.gentics.mesh.MeshVersion.CURRENT_API_BASE_PATH;
import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UPDATED;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.rest.client.MeshRestClientUtil.onErrorCodeResumeNext;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.validateDeletion;
import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestDataProvider.INITIAL_BRANCH_NAME;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.awaitConcurrentRequests;
import static com.gentics.mesh.test.context.MeshTestHelper.validateCreation;
import static com.gentics.mesh.test.util.MeshAssert.assertElement;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeUpsertRequest;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.demo.UserInfo;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.client.GenericParametersImpl;
import com.gentics.mesh.parameter.impl.DeleteParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.definition.BasicRestTestcases;
import com.gentics.mesh.util.UUIDUtil;
import com.gentics.mesh.util.VersionNumber;

import io.reactivex.Observable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class NodeEndpointTest extends AbstractMeshTest implements BasicRestTestcases {

	@Test
	public void testCreateNodeWithNoLanguageCode() {
		try (Tx tx = tx()) {
			NodeCreateRequest request = new NodeCreateRequest();
			SchemaReferenceImpl schemaReference = new SchemaReferenceImpl();
			schemaReference.setName("content");
			schemaReference.setUuid(schemaContainer("content").getUuid());
			// No language code set
			request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setSchema(schemaReference);
			request.setParentNodeUuid(folder("news").getUuid());

			assertThat(trackingSearchProvider()).recordedStoreEvents(0);
			call(() -> client().createNode(PROJECT_NAME, request), BAD_REQUEST, "node_no_languagecode_specified");
			assertThat(trackingSearchProvider()).recordedStoreEvents(0);
		}
	}

	@Test
	public void testCreateNodeWithBogusLanguageCode() throws GenericRestException, Exception {
		String schemaUuid = tx(() -> schemaContainer("content").getUuid());
		String folderUuid = tx(() -> folder("news").getUuid());

		NodeCreateRequest request = new NodeCreateRequest();
		SchemaReferenceImpl schemaReference = new SchemaReferenceImpl();
		schemaReference.setName("content");
		schemaReference.setUuid(schemaUuid);
		schemaReference.setVersion("1.0");
		request.setLanguage("BOGUS");
		request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.setSchema(schemaReference);
		request.setParentNodeUuid(folderUuid);

		assertThat(trackingSearchProvider()).recordedStoreEvents(0);
		call(() -> client().createNode(PROJECT_NAME, request), BAD_REQUEST, "language_not_found", "BOGUS");
		assertThat(trackingSearchProvider()).recordedStoreEvents(0);
	}

	@Test
	public void testCreateNodeInBaseNode() {
		String schemaUuid = tx(() -> schemaContainer("content").getUuid());
		String folderUuid = tx(() -> project().getBaseNode().getUuid());

		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchema(new SchemaReferenceImpl().setVersion("1.0").setName("content").setUuid(schemaUuid));
		request.setLanguage("en");
		request.getFields().put("title", FieldUtil.createStringField("some title"));
		request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		request.getFields().put("slug", FieldUtil.createStringField("new-page"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.setParentNodeUuid(folderUuid);

		waitForSearchIdleEvent();
		assertThat(trackingSearchProvider()).recordedStoreEvents(0);
		NodeResponse restNode = call(() -> client().createNode(PROJECT_NAME, request));
		assertThat(restNode).matches(request);
		waitForSearchIdleEvent();
		assertThat(trackingSearchProvider()).recordedStoreEvents(1);
	}

	@Test
	public void testCreateFolder() {
		String schemaUuid = tx(() -> schemaContainer("folder").getUuid());
		String folderUuid = tx(() -> folder("news").getUuid());

		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchema(new SchemaReferenceImpl().setName("folder").setVersion("1.0").setUuid(schemaUuid));
		request.setLanguage("en");
		request.getFields().put("slug", FieldUtil.createStringField("some slug"));
		request.setParentNodeUuid(folderUuid);

		assertThat(trackingSearchProvider()).recordedStoreEvents(0);
		NodeResponse restNode = call(() -> client().createNode(PROJECT_NAME, request));
		assertThat(restNode).matches(request);
		waitForSearchIdleEvent();

		assertThat(trackingSearchProvider()).recordedStoreEvents(1);
	}

	@Test
	public void testCreateMultiple() {
		// TODO migrate test to performance tests
		try (Tx tx = tx()) {
			HibNode parentNode = folder("news");
			String uuid = parentNode.getUuid();
			assertNotNull(parentNode);
			assertNotNull(parentNode.getUuid());

			long start = System.currentTimeMillis();
			for (int i = 1; i < 100; i++) {
				trackingSearchProvider().reset();
				NodeCreateRequest request = new NodeCreateRequest();
				request.setSchema(new SchemaReferenceImpl().setName("content").setUuid(schemaContainer("content").getUuid()));
				request.setLanguage("en");
				request.getFields().put("title", createStringField("some title " + i));
				request.getFields().put("teaser", createStringField("some teaser " + i));
				request.getFields().put("slug", createStringField("new-page_" + i + ".html"));
				request.getFields().put("content", createStringField("Blessed mealtime again!"));
				request.setParentNodeUuid(uuid);

				call(() -> client().createNode(PROJECT_NAME, request));
				// long duration = currentTimeMillis() - start;
				// out.println("Duration:" + i + " " + (duration / i));
			}
		}
	}

	@Test
	public void testCreateWithoutSegment() {
		String baseNodeUuid = tx(() -> project().getBaseNode().getUuid());

		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("dummyData");
		request.addField(FieldUtil.createStringFieldSchema("test"));
		SchemaResponse response = call(() -> client().createSchema(request));
		String schemaUuid = response.getUuid();

		call(() -> client().assignSchemaToProject(PROJECT_NAME, schemaUuid));

		for (int i = 0; i < 10; i++) {
			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
			nodeCreateRequest.setSchemaName("dummyData");
			nodeCreateRequest.setLanguage("en");
			nodeCreateRequest.setParentNodeUuid(baseNodeUuid);

			NodeResponse node = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
			call(() -> client().publishNode(PROJECT_NAME, node.getUuid()));
		}
	}

	@Test
	@Override
	public void testCreate() throws Exception {

		String schemaUuid = tx(() -> schemaContainer("content").getUuid());
		String parentNodeUuid = tx(() -> folder("news").getUuid());
		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchemaName("content");
		request.setLanguage("en");
		request.getFields().put("title", FieldUtil.createStringField("some title"));
		request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.setParentNodeUuid(parentNodeUuid);

		assertThat(trackingSearchProvider()).recordedStoreEvents(0);

		expect(NODE_CONTENT_CREATED).match(1, NodeMeshEventModel.class, event -> {
			assertThat(event)
				.uuidNotNull()
				.hasBranchUuid(initialBranchUuid())
				.hasSchemaName("content")
				.hasSchemaUuid(schemaUuid)
				.hasLanguage("en");
		});

		NodeResponse restNode = call(() -> client().createNode(PROJECT_NAME, request));
		awaitEvents();
		waitForSearchIdleEvent();

		assertThat(restNode).matches(request);
		assertThat(trackingSearchProvider()).recordedStoreEvents(1);
	}

	@Test
	public void testUpsert() {
		String uuid = UUIDUtil.randomUUID();
		String parentNodeUuid = tx(() -> folder("news").getUuid());
		NodeUpsertRequest request = new NodeUpsertRequest();
		request.setSchemaName("content");
		request.setLanguage("en");
		request.getFields().put("title", FieldUtil.createStringField("some title"));
		request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.setParentNodeUuid(parentNodeUuid);

		for (int i = 0; i < 10; i++) {
			NodeResponse response = call(() -> client().upsertNode(PROJECT_NAME, uuid, request));
			assertEquals("No additional update should alter the version of the node.", "0.1", response.getVersion());
		}

		request.getFields().put("slug", FieldUtil.createStringField("new-page2.html"));
		NodeResponse response = call(() -> client().upsertNode(PROJECT_NAME, uuid, request));
		assertEquals("0.2", response.getVersion());
	}

	@Test
	@Override
	public void testCreateWithNoPerm() throws Exception {

		String parentNodeUuid = tx(() -> folder("news").getUuid());

		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchema(new SchemaReferenceImpl().setName("content"));
		request.setLanguage("en");
		request.getFields().put("title", FieldUtil.createStringField("some title"));
		request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.setParentNodeUuid(parentNodeUuid);

		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			roleDao.revokePermissions(role(), folder("news"), CREATE_PERM);
			tx.success();
		}

		call(() -> client().createNode(PROJECT_NAME, request), FORBIDDEN, "error_missing_perm", parentNodeUuid, CREATE_PERM.getRestPerm().getName());

	}

	@Test
	@Override
	public void testCreateWithUuid() throws Exception {
		String nodeUuid = UUIDUtil.randomUUID();
		String parentNodeUuid = tx(() -> folder("news").getUuid());
		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchema(new SchemaReferenceImpl().setName("content"));
		request.setLanguage("en");
		request.getFields().put("title", FieldUtil.createStringField("some title"));
		request.getFields().put("teaser", FieldUtil.createStringField("some name"));
		request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.setParentNodeUuid(parentNodeUuid);

		try (Tx tx = tx()) {
			assertThat(trackingSearchProvider()).recordedStoreEvents(0);
			NodeResponse restNode = call(() -> client().createNode(nodeUuid, PROJECT_NAME, request));
			assertThat(restNode).matches(request).hasUuid(nodeUuid);
			waitForSearchIdleEvent();
			assertThat(trackingSearchProvider()).recordedStoreEvents(1);
		}
	}

	@Test
	public void testCreateDeleteCreateWithUuid() throws Exception {
		String nodeUuid = UUIDUtil.randomUUID();
		String nodeUuid2 = UUIDUtil.randomUUID();
		String parentNodeUuid = tx(() -> folder("news").getUuid());
		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchema(new SchemaReferenceImpl().setName("content"));
		request.setLanguage("en");
		request.getFields().put("title", FieldUtil.createStringField("some title"));
		request.getFields().put("teaser", FieldUtil.createStringField("some name"));
		request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.setParentNodeUuid(parentNodeUuid);

		for (int i = 0; i < 10; i++) {
			// 1. Create
			request.setParentNodeUuid(parentNodeUuid);
			call(() -> client().createNode(nodeUuid, PROJECT_NAME, request));
			request.setParentNodeUuid(nodeUuid);
			call(() -> client().createNode(nodeUuid2, PROJECT_NAME, request));

			// 2. Delete
			call(() -> client().deleteNode(PROJECT_NAME, nodeUuid, new DeleteParametersImpl().setRecursive(true)));
		}

	}

	@Test
	@Override
	public void testCreateWithDuplicateUuid() throws Exception {
		String nodeUuid = tx(() -> project().getBaseNode().getUuid());
		String parentNodeUuid = tx(() -> folder("news").getUuid());
		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchema(new SchemaReferenceImpl().setName("content"));
		request.setLanguage("en");
		request.getFields().put("title", FieldUtil.createStringField("some title"));
		request.getFields().put("teaser", FieldUtil.createStringField("some name"));
		request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.setParentNodeUuid(parentNodeUuid);

		try (Tx tx = tx()) {
			assertThat(trackingSearchProvider()).recordedStoreEvents(0);
			call(() -> client().createNode(nodeUuid, PROJECT_NAME, request), BAD_REQUEST);
		}
	}

	@Test
	public void testCreateForBranchByName() {
		String initialBranchUuid = tx(() -> initialBranch().getUuid());
		String newBranchUuid = tx(() -> createBranch("newbranch").getUuid());
		String nodeUuid = tx(() -> folder("news").getUuid());

		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchemaName("content");
		request.setLanguage("en");
		request.getFields().put("title", FieldUtil.createStringField("some title"));
		request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.setParentNodeUuid(nodeUuid);

		NodeResponse nodeResponse = call(
			() -> client().createNode(PROJECT_NAME, request, new VersioningParametersImpl().setBranch(INITIAL_BRANCH_NAME)));

		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNode newNode = boot().nodeDao().findByUuid(project(), nodeResponse.getUuid());
			for (ContainerType type : Arrays.asList(ContainerType.INITIAL, ContainerType.DRAFT)) {
				assertThat(contentDao.getFieldContainer(newNode, "en", initialBranchUuid, type)).as(type + " Field container for initial branch")
					.isNotNull().hasVersion("0.1");
				assertThat(contentDao.getFieldContainer(newNode, "en", newBranchUuid, type)).as(type + " Field Container for new branch")
					.isNull();
			}
		}
	}

	@Test
	public void testCreateForBranchByUuid() {
		String nodeUuid = tx(() -> folder("news").getUuid());
		String initialBranchUuid = tx(() -> initialBranch().getUuid());
		HibBranch newBranch = tx(() -> createBranch("newbranch"));

		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchemaName("content");
		request.setLanguage("en");
		request.getFields().put("title", FieldUtil.createStringField("some title"));
		request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.setParentNodeUuid(nodeUuid);

		NodeResponse nodeResponse = call(() -> client().createNode(PROJECT_NAME, request, new VersioningParametersImpl().setBranch(
			initialBranchUuid)));

		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNode newNode = boot().nodeDao().findByUuid(project(), nodeResponse.getUuid());
			for (ContainerType type : Arrays.asList(ContainerType.INITIAL, ContainerType.DRAFT)) {
				assertThat(contentDao.getFieldContainer(newNode, "en", initialBranchUuid, type)).as(type + " Field container for initial branch")
					.isNotNull().hasVersion("0.1");
				assertThat(contentDao.getFieldContainer(newNode, "en", newBranch.getUuid(), type)).as(type + " Field Container for new branch")
					.isNull();
			}
		}

	}

	@Test
	public void testCreateForLatestBranch() {
		HibBranch newBranch = createBranch("newbranch", true);

		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNode parentNode = folder("news");
			String uuid = parentNode.getUuid();
			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchemaName("content");
			request.setLanguage("en");
			request.getFields().put("title", FieldUtil.createStringField("some title"));
			request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setParentNodeUuid(uuid);

			NodeResponse nodeResponse = call(() -> client().createNode(PROJECT_NAME, request));

			HibNode newNode = boot().nodeDao().findByUuid(project(), nodeResponse.getUuid());

			for (ContainerType type : Arrays.asList(ContainerType.INITIAL, ContainerType.DRAFT)) {
				assertThat(contentDao.getFieldContainer(newNode, "en", initialBranchUuid(), type))
					.as(type + " Field container for initial branch")
					.isNull();
				assertThat(contentDao.getFieldContainer(newNode, "en", newBranch.getUuid(), type)).as(type + " Field Container for new branch")
					.isNotNull()
					.hasVersion("0.1");
			}
		}
	}

	@Test
	public void testCreateForBogusBranch() {
		String uuid = tx(() -> {
			createBranch("newbranch");

			HibNode parentNode = folder("news");
			return parentNode.getUuid();
		});
		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchemaName("content");
		request.setLanguage("en");
		request.getFields().put("title", FieldUtil.createStringField("some title"));
		request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.setParentNodeUuid(uuid);
		call(() -> client().createNode(PROJECT_NAME, request, new VersioningParametersImpl().setBranch("bogusbranch")), BAD_REQUEST,
			"branch_error_not_found", "bogusbranch");

	}

	@Test
	@Ignore
	public void testCreateNodeAndCheckReadOnlyRole() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testCreateReadDelete() throws Exception {
		try (Tx tx = tx()) {
			NodeResponse restNode2;

			NodeCreateRequest request = new NodeCreateRequest();
			SchemaReferenceImpl schemaReference = new SchemaReferenceImpl();
			schemaReference.setName("content");
			schemaReference.setUuid(schemaContainer("content").getUuid());
			request.setSchema(schemaReference);

			request.setLanguage("de");
			request.getFields().put("title", FieldUtil.createStringField("Title"));
			request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setParentNodeUuid(folder("news").getUuid());

			assertThat(trackingSearchProvider()).recordedStoreEvents(0);
			NodeResponse restNode = call(() -> client().createNode(PROJECT_NAME, request, new NodeParametersImpl().setLanguages("de"),
				new VersioningParametersImpl().draft()));
			waitForSearchIdleEvent();
			assertThat(trackingSearchProvider()).recordedStoreEvents(1);
			assertThat(restNode).matches(request);

			HibNode node = boot().nodeDao().findByUuid(project(), restNode.getUuid());
			assertNotNull(node);
			assertThat(node).matches(request);

			// Load the node again
			restNode2 = call(() -> client().findNodeByUuid(PROJECT_NAME, restNode.getUuid(), new NodeParametersImpl().setLanguages("de"),
				new VersioningParametersImpl().draft()));

			// Delete the node
			call(() -> client().deleteNode(PROJECT_NAME, restNode2.getUuid()));

			HibNode deletedNode = boot().nodeDao().findByUuid(project(), restNode2.getUuid());
			assertNull("The node should have been deleted.", deletedNode);
		}
	}

	@Test
	public void testCreateNodeWithMissingParentNodeUuid() throws Exception {
		try (Tx tx = tx()) {
			NodeCreateRequest request = new NodeCreateRequest();
			SchemaReferenceImpl schemaReference = new SchemaReferenceImpl();
			schemaReference.setName("node");
			request.setSchema(schemaReference);
			request.getFields().put("teaser", createStringField("some teaser"));
			request.getFields().put("slug", createStringField("new-page.html"));
			request.getFields().put("content", createStringField("Blessed mealtime again!"));
			request.setSchema(new SchemaReferenceImpl().setName("content").setUuid(schemaContainer("content").getUuid()));

			call(() -> client().createNode(PROJECT_NAME, request), BAD_REQUEST, "node_missing_parentnode_field");
		}
	}

	@Test
	public void testCreateNodeWithMissingSchemaPermission() {
		HibNode node = folder("news");

		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			roleDao.revokePermissions(role(), schemaContainer("content"), READ_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			String uuid = node.getUuid();
			NodeCreateRequest request = new NodeCreateRequest();
			SchemaReference schemaReference = new SchemaReferenceImpl().setName("content").setUuid(schemaContainer("content").getUuid());
			request.setSchema(schemaReference);
			request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setSchema(new SchemaReferenceImpl().setName("content").setUuid(schemaContainer("content").getUuid()));
			request.setLanguage("en");
			request.setParentNodeUuid(uuid);
			call(() -> client().createNode(PROJECT_NAME, request), FORBIDDEN, "error_missing_perm", schemaContainer("content").getUuid(),
				READ_PERM.getRestPerm().getName());
		}
	}

	@Test
	public void testCreateNodeWithMissingPermission() throws Exception {

		// Revoke create perm
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			roleDao.revokePermissions(role(), folder("news"), CREATE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			HibNode node = folder("news");
			String uuid = node.getUuid();
			NodeCreateRequest request = new NodeCreateRequest();
			SchemaReference schemaReference = new SchemaReferenceImpl().setName("content").setUuid(schemaContainer("content").getUuid());
			request.setSchema(schemaReference);
			request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setSchema(new SchemaReferenceImpl().setName("content").setUuid(schemaContainer("content").getUuid()));
			request.setLanguage("en");
			request.setParentNodeUuid(uuid);

			assertThat(trackingSearchProvider()).recordedStoreEvents(0);
			call(() -> client().createNode(PROJECT_NAME, request), FORBIDDEN, "error_missing_perm", uuid, CREATE_PERM.getRestPerm().getName());
			assertThat(trackingSearchProvider()).recordedStoreEvents(0);
		}
	}

	// Read tests

	/**
	 * Test default paging parameters
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReadNodesDefaultPaging() throws Exception {
		NodeListResponse restResponse = call(() -> client().findNodes(PROJECT_NAME, new VersioningParametersImpl().draft()));

		assertNotNull(restResponse);
		assertNull(restResponse.getMetainfo().getPerPage());
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals(getNodeCount(), restResponse.getData().size());
	}

	@Test
	public void testReadMultipleAndAssertOrder() {
		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();
			RoleDao roleDao = tx.roleDao();
			HibNode parentNode = folder("2015");
			int nNodes = 20;
			for (int i = 0; i < nNodes; i++) {
				HibNode node = nodeDao.create(parentNode, user(), schemaContainer("content").getLatestVersion(), project());
				boot().contentDao().createFieldContainer(node, english(), initialBranch(), user());
				assertNotNull(node);
				roleDao.grantPermissions(role(), node, READ_PERM);
			}
			tx.success();
		}

		String firstUuid = null;
		for (int i = 0; i < 10; i++) {
			NodeListResponse response = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 100L), new VersioningParametersImpl()
				.draft()));
			if (firstUuid == null) {
				firstUuid = response.getData().get(0).getUuid();
			}
			assertEquals("The first element in the page should not change but it changed in run {" + i + "}", firstUuid, response.getData().get(0)
				.getUuid());
		}

	}

	@Test
	public void testReadNodeWithFieldLimit() {
		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid(), new GenericParametersImpl().setFields("uuid")));
		// not empty
		assertThat(response.getUuid()).isNotEmpty();

		// omitted
		assertThat(response.getAvailableLanguages()).isNull();
		assertThat(response.getChildrenInfo()).isNull();
		assertThat(response.getFields()).isNull();

		response = call(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid(), new GenericParametersImpl().setFields("uuid", "fields")));

		// not empty
		System.out.println(response.toJson());
		assertThat(response.getUuid()).isNotEmpty();
		assertThat(response.getFields()).isNotEmpty();

		// omitted
		assertThat(response.getAvailableLanguages()).isNull();
		assertThat(response.getChildrenInfo()).isNull();

	}

	@Test
	@Override
	public void testReadMultiple() throws Exception {
		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();
			HibNode parentNode = folder("2015");
			// Don't grant permissions to the no perm node. We want to make sure that this one will not be listed.
			HibNode noPermNode = nodeDao.create(parentNode, user(), schemaContainer("content").getLatestVersion(), project());
			String noPermNodeUUID = noPermNode.getUuid();

			// Create 20 drafts
			int nNodes = 20;
			for (int i = 0; i < nNodes; i++) {
				NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
				nodeCreateRequest.setSchema(new SchemaReferenceImpl().setName("content"));
				nodeCreateRequest.getFields().put("teaser", FieldUtil.createStringField("test"));
				nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("test" + i));
				nodeCreateRequest.setParentNodeUuid(parentNode.getUuid());
				nodeCreateRequest.setLanguage("en");
				call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
			}

			assertNotNull(noPermNode.getUuid());
			long perPage = 11;
			NodeListResponse restResponse = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(3, perPage),
				new VersioningParametersImpl().draft()));
			assertEquals(perPage, restResponse.getData().size());

			// Extra Nodes + permitted nodes
			int totalNodes = getNodeCount() + nNodes;
			int totalPages = (int) Math.ceil(totalNodes / (double) perPage);
			assertEquals("The response did not contain the correct amount of items", perPage, restResponse.getData().size());
			assertEquals(3, restResponse.getMetainfo().getCurrentPage());
			assertEquals(totalNodes, restResponse.getMetainfo().getTotalCount());
			assertEquals(totalPages, restResponse.getMetainfo().getPageCount());
			assertEquals(perPage, restResponse.getMetainfo().getPerPage().longValue());

			List<NodeResponse> allNodes = new ArrayList<>();
			for (int page = 1; page <= totalPages; page++) {
				restResponse = client().findNodes(PROJECT_NAME, new PagingParametersImpl(page, perPage),
					new VersioningParametersImpl().draft()).blockingGet();
				allNodes.addAll(restResponse.getData());
			}
			assertEquals("Somehow not all nodes were loaded when loading all pages.", totalNodes, allNodes.size());

			// Verify that the no_perm_node is not part of the response
			List<NodeResponse> filteredUserList = allNodes.parallelStream().filter(restNode -> restNode.getUuid().equals(noPermNodeUUID)).collect(
				Collectors.toList());
			assertTrue("The no perm node should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

			call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(-1, 25L)),
				BAD_REQUEST, "error_page_parameter_must_be_positive", "-1");

			call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(0, 25L)),
				BAD_REQUEST, "error_page_parameter_must_be_positive", "0");

			call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, -1L)),
				BAD_REQUEST, "error_pagesize_parameter", "-1");

			NodeListResponse list = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(4242, 25L), new VersioningParametersImpl()
				.draft()));
			assertEquals(4242, list.getMetainfo().getCurrentPage());
			assertEquals(0, list.getData().size());
			assertEquals(25L, list.getMetainfo().getPerPage().longValue());
			assertEquals(2, list.getMetainfo().getPageCount());
			assertEquals(getNodeCount() + nNodes, list.getMetainfo().getTotalCount());
		}
	}

	@Test
	public void testReadMultipleOnlyMetadata() {
		NodeListResponse listResponse = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 0L), new VersioningParametersImpl()
			.draft()));
		assertEquals(0, listResponse.getData().size());
	}

	@Test
	public void testReadNodesWithoutPermissions() throws Exception {

		// TODO add node that has no perms and check the response
		NodeListResponse restResponse = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 10L), new VersioningParametersImpl()
			.draft()));

		int nElements = restResponse.getData().size();
		assertEquals("The amount of elements in the list did not match the expected count", 10, nElements);
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals(2, restResponse.getMetainfo().getPageCount());
		assertEquals(10, restResponse.getMetainfo().getPerPage().longValue());
		assertEquals(getNodeCount(), restResponse.getMetainfo().getTotalCount());
	}

	@Test
	public void testReadNodesForBranch() {
		HibBranch newBranch = tx(() -> createBranch("newbranch"));

		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();

			NodeListResponse restResponse = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 1000L),
				new VersioningParametersImpl().draft()));
			assertThat(restResponse.getData()).as("Node List for latest branch").isEmpty();

			restResponse = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 1000L), new VersioningParametersImpl().setBranch(
				initialBranch().getName()).draft()));
			assertThat(restResponse.getData()).as("Node List for initial branch").hasSize(getNodeCount());

			// update a single node in the new branch
			HibNode node = folder("news");
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(nodeDao.getParentNode(node, project().getInitialBranch().getUuid()).getUuid());
			create.setLanguage("en");
			create.getFields().put("name", FieldUtil.createStringField("News new branch"));
			call(() -> client().createNode(node.getUuid(), PROJECT_NAME, create));

			// check whether there is one node in the new branch now
			restResponse = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 1000L), new VersioningParametersImpl().draft()));
			assertThat(restResponse.getData()).as("Node List for latest branch").hasSize(1);

			restResponse = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 1000L), new VersioningParametersImpl().draft()
				.setBranch(newBranch.getName())));
			assertThat(restResponse.getData()).as("Node List for latest branch").hasSize(1);
		}

	}

	@Test
	public void testReadPublishedNodes() {
		try (Tx tx = tx()) {
			List<HibNode> nodes = Arrays.asList(folder("products"), folder("deals"), folder("news"), folder("2015"));

			// 1. Take all nodes offline
			call(() -> client().takeNodeOffline(PROJECT_NAME, project().getBaseNode().getUuid(), new PublishParametersImpl().setRecursive(true)));

			// 2. Assert that all nodes are offline. The findNodes method should not find any node because it searches for published nodes by default.
			NodeListResponse listResponse = call(() -> client().findNodes(PROJECT_NAME, new VersioningParametersImpl().published(),
				new PagingParametersImpl(1, 1000L)));
			assertThat(listResponse.getData()).as("Published nodes list").isEmpty();

			// 3. Assert that the offline nodes are also not loadable if requests via uuid
			for (HibNode node : nodes) {
				call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().published()), NOT_FOUND,
					"node_error_published_not_found_for_uuid_branch_language", node.getUuid(), "en", latestBranch().getUuid());
			}

			// Publish a few nodes
			nodes.stream().forEach(node -> call(() -> client().publishNode(PROJECT_NAME, node.getUuid())));

			// Read each node individually
			List<NodeResponse> publishedNodes = nodes.stream().map(node -> call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid()))).collect(
				Collectors.toList());
			assertThat(publishedNodes).hasSize(nodes.size());

			// Read a bunch of nodes
			listResponse = call(() -> client().findNodes(PROJECT_NAME, new VersioningParametersImpl().published(), new PagingParametersImpl(1,
				1000L)));
			assertThat(listResponse.getData()).as("Published nodes list").usingElementComparatorOnFields("uuid").containsOnlyElementsOf(
				publishedNodes);
		}
	}

	@Test
	public void testReadPublishedNodesNoPermission() {

		String baseNodeUuid = tx(() -> project().getBaseNode().getUuid());

		// Take all nodes offline
		call(() -> client().takeNodeOffline(PROJECT_NAME, baseNodeUuid, new PublishParametersImpl().setRecursive(true)));

		// Assert that the list is now empty since all nodes are offline
		NodeListResponse listResponse = call(() -> client().findNodes(PROJECT_NAME, new VersioningParametersImpl().published(),
			new PagingParametersImpl(1, 1000L)));
		assertThat(listResponse.getData()).as("Published nodes list").isEmpty();

		// Republish all listed
		List<HibNode> nodes = tx(tx -> {
			ArrayList<HibNode> list = new ArrayList<>(Arrays.asList(folder("products"), folder("deals"), folder("news"), folder("2015")));
			list.stream().forEach(node -> call(() -> client().publishNode(PROJECT_NAME, node.getUuid())));
			return list;
		});

		// Revoke permission on one folder after the other
		while (!nodes.isEmpty()) {
			HibNode folder = nodes.remove(0);
			tx((tx) -> {
				RoleDao roleDao = tx.roleDao();
				roleDao.revokePermissions(role(), folder, READ_PUBLISHED_PERM);
				roleDao.revokePermissions(role(), folder, READ_PERM);
				tx.success();
			});

			// Load all nodes and check whether they are readable
			List<NodeResponse> publishedNodes = nodes.stream().map(node -> {
				String uuid = tx(() -> node.getUuid());
				return call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().published()));
			}).collect(Collectors.toList());
			assertThat(publishedNodes).hasSize(nodes.size());

			listResponse = call(() -> client().findNodes(PROJECT_NAME, new VersioningParametersImpl().published(), new PagingParametersImpl(1,
				1000L)));
			assertThat(listResponse.getData()).as("Published nodes list").usingElementComparatorOnFields("uuid").containsOnlyElementsOf(
				publishedNodes);
		}
	}

	@Test
	public void testReadPublishedNodesNoPermission2() {

		// Republish all listed nodes
		List<HibNode> nodes = tx(tx -> {
			ArrayList<HibNode> list = new ArrayList<>(Arrays.asList(folder("products"), folder("deals"), folder("news"), folder("2015")));
			list.stream().forEach(node -> call(() -> client().publishNode(PROJECT_NAME, node.getUuid())));
			return list;
		});

		NodeListResponse initialListResponse = call(() -> client().findNodes(PROJECT_NAME, new VersioningParametersImpl().published(),
			new PagingParametersImpl(1, 1000L)));

		int revoked = 0;
		for (HibNode node : nodes) {
			// Revoke the read perm but keep the read published perm on the node
			tx((tx) -> {
				RoleDao roleDao = tx.roleDao();
				roleDao.revokePermissions(role(), node, READ_PERM);
				roleDao.grantPermissions(role(), node, READ_PUBLISHED_PERM);
				tx.success();
			});
			revoked++;

			// Read the given node - It should still be readable
			String uuid = tx(() -> node.getUuid());
			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().published()));

			/*
			 * Extra case for https://github.com/gentics/mesh/issues/1104 // The draft version shares the same container with the published version. call(() ->
			 * client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft()));
			 * 
			 * // Now lets update the node grantAdmin(); NodeUpdateRequest request = new NodeUpdateRequest(); request.setLanguage("en");
			 * request.getFields().putString("name", "1234"); call(() -> client().updateNode(PROJECT_NAME, uuid, request)); revokeAdmin();
			 * mesh().permissionCache().clear();
			 */

			// Now the containers are different and the request should fail
			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft()), FORBIDDEN, "error_missing_perm", uuid,
				READ_PERM.getRestPerm().getName());

			// Verify also that the read nodes endpoint still finds all nodes
			NodeListResponse listResponse = call(() -> client().findNodes(PROJECT_NAME, new VersioningParametersImpl().published(),
				new PagingParametersImpl(1, 1000L)));
			assertThat(listResponse.getData()).as("Published nodes list").hasSameSizeAs(initialListResponse.getData());
			listResponse = call(() -> client().findNodes(PROJECT_NAME, new VersioningParametersImpl().draft(), new PagingParametersImpl(1, 1000L)));
			assertThat(listResponse.getData()).as("Draft nodes list").hasSize(initialListResponse.getData().size() - revoked);
		}
	}

	@Test
	@Ignore("Test covers bug https://github.com/gentics/mesh/issues/1104")
	public void testReadPublishedNodeNoPermission3() {
		String uuid = tx(() -> content().getUuid());

		// 1. Publish node
		NodeResponse draftResponse1 = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));
		call(() -> client().publishNode(PROJECT_NAME, uuid));
		NodeResponse publishedResponse = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().published()));
		assertEquals("Draft and publish versions should be the same since mesh automatically creates a new draft based on the published version.",
			draftResponse1.getVersion(), publishedResponse.getVersion());

		// 2. Update the node to create a dedicated (not shared) draft version (1.1)
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.setVersion("draft");
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("test123"));
		call(() -> client().updateNode(PROJECT_NAME, uuid, nodeUpdateRequest));
		NodeResponse draftResponse2 = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));

		// 3. Revoke permissions and only grant read_published
		tx((tx) -> {
			RoleDao roleDao = tx.roleDao();
			HibNode node = content();
			roleDao.revokePermissions(role(), node, READ_PERM);
			roleDao.revokePermissions(role(), node, CREATE_PERM);
			roleDao.revokePermissions(role(), node, DELETE_PERM);
			roleDao.revokePermissions(role(), node, UPDATE_PERM);
			roleDao.revokePermissions(role(), node, PUBLISH_PERM);
			roleDao.grantPermissions(role(), node, READ_PUBLISHED_PERM);
			tx.success();
		});

		// Assert that draft node can't be loaded (e.g. default version = draft)
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid), FORBIDDEN, "error_missing_perm", uuid, READ_PERM.getRestPerm().getName());

		// Assert that draft node can't be loaded with either "draft" or draft version number
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft()), FORBIDDEN, "error_missing_perm", uuid,
			READ_PERM.getRestPerm().getName());
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setVersion(draftResponse2.getVersion())), FORBIDDEN,
			"error_missing_perm", uuid, READ_PERM.getRestPerm().getName());

		// Assert that published node can be loaded with either "published" or publish version number
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().published()));
		String version = publishedResponse.getVersion();
		System.out.println(version);
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setVersion(version)));

	}

	@Test
	@Ignore("Disabled until custom 404 handler has been added")
	public void testReadNodeWithBogusProject() {
		call(() -> client().findNodeByUuid("BOGUS", "someUuuid"), BAD_REQUEST, "project_not_found", "BOGUS");
	}

	@Test
	public void testCreateUpdateReadDeleteMultithreaded() throws Exception {

		Logger log = LoggerFactory.getLogger(NodeEndpointTest.class);

		int nJobs = 200;
		long nNodesFound;
		String uuid;

		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();
			HibNode parentNode = folder("news");
			uuid = parentNode.getUuid();

			nNodesFound = nodeDao.count(project());
		}

		Function<Long, NodeCreateRequest> createRequest = nr -> {
			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReferenceImpl().setName("content"));
			request.setLanguage("en");
			request.getFields().put("title", FieldUtil.createStringField("some title"));
			request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			request.getFields().put("slug", FieldUtil.createStringField("new-page" + nr + ".html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setParentNodeUuid(uuid);
			return request;
		};

		NodeUpdateRequest updateRequest = new NodeUpdateRequest();
		updateRequest.setLanguage("en");
		updateRequest.getFields().put("teaser", FieldUtil.createStringField("UPDATED"));

		// Creates, updates, reads and then deletes a node every 25 milliseconds.
		Observable.intervalRange(0, nJobs, 0, 25, TimeUnit.MILLISECONDS)
			.flatMapSingle(i -> {
				log.info("Invoking createNode REST call for job {" + i + "}");
				return client().createNode(PROJECT_NAME, createRequest.apply(i)).toSingle();
			}).flatMapSingle(node -> {
				log.info("Created {" + node.getUuid() + "}");
				return client().updateNode(PROJECT_NAME, node.getUuid(), updateRequest).toSingle();
			}).flatMapSingle(node -> {
				log.info("Updated {" + node.getUuid() + "}");
				return client().findNodeByUuid(PROJECT_NAME, node.getUuid()).toSingle();
			}).flatMapCompletable(node -> {
				log.info("Read {" + node.getUuid() + "}");
				return client().deleteNode(PROJECT_NAME, node.getUuid()).toCompletable()
					.doOnComplete(() -> log.info("Deleted {" + node.getUuid() + "}"));
			}).blockingAwait();

		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();
			long nNodesFoundAfterRest = nodeDao.findAll(project()).count();
			assertEquals("All created nodes should have been created.", nNodesFound, nNodesFoundAfterRest);
		}
	}

	@Test
	@Override
	public void testCreateMultithreaded() throws InterruptedException {
		int nJobs = 500;
		String uuid = null;

		try (Tx tx = tx()) {
			HibNode parentNode = folder("news");
			uuid = parentNode.getUuid();
			assertNotNull(parentNode);
			assertNotNull(parentNode.getUuid());
		}

		String fUuid = uuid;

		validateCreation(nJobs, i -> {
			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReferenceImpl().setName("content"));
			request.setLanguage("en");
			request.getFields().put("title", FieldUtil.createStringField("some title"));
			request.getFields().put("teaser", FieldUtil.createStringField("some-teaser"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.getFields().put("slug", FieldUtil.createStringField("new-page" + i + ".html"));
			request.setParentNodeUuid(fUuid);
			return client().createNode(PROJECT_NAME, request);
		});
	}

	@Test
	@Override
	@Ignore("Multithreaded update is currently only possible for multiple nodes not a single node")
	public void testUpdateMultithreaded() throws InterruptedException {
		ContentDao contentDao = boot().contentDao();
		final String newName = "english renamed name";
		String uuid = tx(() -> folder("2015").getUuid());
		assertEquals("2015", tx(() -> contentDao.getLatestDraftFieldContainer(folder("2015"), english()).getString("slug").getString()));
		VersionNumber version = tx(() -> contentDao.getLatestDraftFieldContainer(folder("2015"), english()).getVersion());

		NodeUpdateRequest request = new NodeUpdateRequest();
		request.setLanguage("en");

		NodeParametersImpl parameters = new NodeParametersImpl();
		parameters.setLanguages("en", "de");

		int nJobs = 5;
		awaitConcurrentRequests(nJobs, i -> {
			System.out.println(version.getFullVersion());
			request.setVersion(version.getFullVersion());
			request.getFields().put("name", FieldUtil.createStringField(newName + ":" + i));
			return client().updateNode(PROJECT_NAME, uuid, request, parameters);
		});
	}

	@Test
	@Override
	public void testDeleteByUUIDMultithreaded() {
		String uuid = tx(() -> folder("2015").getUuid());
		int nJobs = 6;

		validateDeletion(i -> client().deleteNode(PROJECT_NAME, uuid, new DeleteParametersImpl().setRecursive(true)), nJobs);
	}

	@Test
	@Override
	public void testReadByUuidMultithreaded() throws InterruptedException {
		int nJobs = 50;
		try (Tx tx = tx()) {
			Observable.range(0, nJobs)
				.flatMapCompletable(
					i -> client().findNodeByUuid(PROJECT_NAME, folder("2015").getUuid(), new VersioningParametersImpl().draft()).toCompletable())
				.blockingAwait();
		}
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws InterruptedException {
		int nJobs = 200;
		try (Tx tx = tx()) {
			Observable.range(0, nJobs)
				.flatMapCompletable(
					i -> client().findNodeByUuid(PROJECT_NAME, folder("2015").getUuid(), new VersioningParametersImpl().draft()).toCompletable())
				.blockingAwait();
		}
	}

	@Test
	public void testReadByUuidWithRolePerms() {
		try (Tx tx = tx()) {
			HibNode node = folder("2015");
			String uuid = node.getUuid();

			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new RolePermissionParametersImpl().setRoleUuid(role()
				.getUuid()), new VersioningParametersImpl().draft()));
			assertNotNull(response.getRolePerms());
			assertThat(response.getRolePerms()).hasPerm(Permission.values());
		}
	}

	@Test
	public void testReadByUUID() throws Exception {
		String folderUuid = tx(() -> folder("2015").getUuid());
		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, folderUuid, new VersioningParametersImpl().draft()));
		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();

			String branchUuid = project().getLatestBranch().getUuid();
			assertThat(folder("2015")).matches(response);
			assertNotNull(response.getParentNode());
			assertEquals(nodeDao.getParentNode(folder("2015"), branchUuid).getUuid(), response.getParentNode().getUuid());
			assertEquals("News", response.getParentNode().getDisplayName());
			assertEquals("en", response.getLanguage());
		}
	}

	@Test
	public void testReadByUUIDWithNoUser() throws Exception {
		String folderUuid = tx(() -> folder("2015").getUuid());
		// Remove the editor and creator references to simulate that the user has been deleted.
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			folder("2015").setCreated(null);
			contentDao.getLatestDraftFieldContainer(folder("2015"), english()).setEditor(null);
			tx.success();
		}

		call(() -> client().getNodePublishStatus(PROJECT_NAME, folderUuid));

		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, folderUuid, new VersioningParametersImpl().draft()));
		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();

			String branchUuid = project().getLatestBranch().getUuid();
			assertThat(folder("2015")).matches(response);
			assertNotNull(response.getParentNode());
			assertEquals(nodeDao.getParentNode(folder("2015"), branchUuid).getUuid(), response.getParentNode().getUuid());
			assertEquals("News", response.getParentNode().getDisplayName());
			assertEquals("en", response.getLanguage());
		}
	}

	@Test
	public void testReadVersionByNumber() {
		try (Tx tx = tx()) {
			HibNode node = folder("2015");
			String uuid = node.getUuid();

			// Load node and assert initial versions and field values
			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft()));
			assertThat(response).hasVersion("1.0").hasLanguage("en").hasStringField("name", "2015");

			// create version 1.1
			NodeUpdateRequest updateRequest = new NodeUpdateRequest();
			updateRequest.setLanguage("en");
			updateRequest.setVersion("1.0");
			updateRequest.getFields().put("name", FieldUtil.createStringField("one"));
			call(() -> client().updateNode(PROJECT_NAME, uuid, updateRequest));

			// create version 1.2
			updateRequest.setVersion("1.1");
			updateRequest.getFields().put("name", FieldUtil.createStringField("two"));
			call(() -> client().updateNode(PROJECT_NAME, uuid, updateRequest));

			// create version 1.3
			updateRequest.setVersion("1.2");
			updateRequest.getFields().put("name", FieldUtil.createStringField("three"));
			call(() -> client().updateNode(PROJECT_NAME, uuid, updateRequest));

			// create german version 0.1
			updateRequest.setLanguage("de");
			updateRequest.setVersion(null);
			updateRequest.getFields().put("name", FieldUtil.createStringField("eins"));
			call(() -> client().updateNode(PROJECT_NAME, uuid, updateRequest));

			// create german version 0.2
			updateRequest.setVersion("0.1");
			updateRequest.getFields().put("name", FieldUtil.createStringField("zwei"));
			call(() -> client().updateNode(PROJECT_NAME, uuid, updateRequest));

			// create german version 0.3
			updateRequest.setVersion("0.2");
			updateRequest.getFields().put("name", FieldUtil.createStringField("drei"));
			call(() -> client().updateNode(PROJECT_NAME, uuid, updateRequest));

			// Test english versions
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft()))).as("Draft").hasVersion("1.3")
				.hasLanguage("en").hasStringField("name", "three");

			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setVersion("1.0")))).as("Version 1.0")
				.hasVersion("1.0").hasLanguage("en").hasStringField("name", "2015");

			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setVersion("1.1")))).as("Version 1.1")
				.hasVersion("1.1").hasLanguage("en").hasStringField("name", "one");
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setVersion("1.2")))).as("Version 1.2")
				.hasVersion("1.2").hasLanguage("en").hasStringField("name", "two");
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setVersion("1.3")))).as("Version 1.3")
				.hasVersion("1.3").hasLanguage("en").hasStringField("name", "three");

			// Test german versions
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new NodeParametersImpl().setLanguages("de"),
				new VersioningParametersImpl().draft()))).as("German draft").hasVersion("0.3").hasLanguage("de").hasStringField("name", "drei");
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new NodeParametersImpl().setLanguages("de"),
				new VersioningParametersImpl().setVersion("0.1")))).as("German version 0.1").hasVersion("0.1").hasLanguage("de").hasStringField(
					"name", "eins");
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new NodeParametersImpl().setLanguages("de"),
				new VersioningParametersImpl().setVersion("0.2")))).as("German version 0.2").hasVersion("0.2").hasLanguage("de").hasStringField(
					"name", "zwei");
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new NodeParametersImpl().setLanguages("de"),
				new VersioningParametersImpl().setVersion("0.3")))).as("German version 0.3").hasVersion("0.3").hasLanguage("de").hasStringField(
					"name", "drei");
		}
	}

	@Test
	public void testReadBogusVersion() {
		try (Tx tx = tx()) {
			HibNode node = folder("2015");
			String uuid = node.getUuid();

			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setVersion("bogus")), BAD_REQUEST,
				"error_illegal_version", "bogus");
		}
	}

	@Test
	public void testReadInexistentVersion() {
		try (Tx tx = tx()) {
			HibNode node = folder("2015");
			String uuid = node.getUuid();

			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setVersion("47.11")), NOT_FOUND,
				"object_not_found_for_version", "47.11");
		}
	}

	@Test
	public void testReadPublishedVersion() {
		String uuid = tx(() -> folder("2015").getUuid());
		String branchUuid = tx(() -> project().getLatestBranch().getUuid());

		// 1. Take node offline
		call(() -> client().takeNodeOffline(PROJECT_NAME, uuid, new PublishParametersImpl().setRecursive(true)));

		// 2. Load node using published options.
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().published()), NOT_FOUND,
			"node_error_published_not_found_for_uuid_branch_language", uuid, "en", branchUuid);

		// 3. Publish the node again.
		call(() -> client().publishNode(PROJECT_NAME, uuid));

		// 4. Assert that the node can be found.
		NodeResponse nodeResponse = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));
		assertThat(nodeResponse).as("Published node").hasLanguage("en").hasVersion("2.0");
	}

	@Test
	public void testReadNodeForBranch() {
		HibNode node = folder("2015");
		HibBranch newBranch = createBranch("newbranch", true);

		try (Tx tx = tx()) {
			String uuid = node.getUuid();
			NodeUpdateRequest updateRequest = new NodeUpdateRequest();
			updateRequest.setLanguage("en");
			updateRequest.getFields().put("name", FieldUtil.createStringField("2015 in new branch"));
			updateRequest.setVersion("0.1");
			call(() -> client().updateNode(PROJECT_NAME, uuid, updateRequest, new VersioningParametersImpl().setBranch(newBranch.getName())));

			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setBranch(initialBranch().getName())
				.draft()))).as("Initial Branch Version").hasVersion("1.0").hasStringField("name", "2015");
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setBranch(newBranch.getName())
				.draft()))).as("New Branch Version").hasVersion("1.1").hasStringField("name", "2015 in new branch");
		}

	}

	@Test
	public void testReadNodeVersionForBranch() {
		disableAutoPurge();

		HibNode node = folder("2015");
		String uuid = tx(() -> node.getUuid());
		HibBranch newBranch = createBranch("newbranch", true);

		try (Tx tx = tx()) {
			NodeUpdateRequest updateRequest = new NodeUpdateRequest();
			updateRequest.setLanguage("en");
			updateRequest.setVersion("1.0");

			// create version 0.1 in new branch
			updateRequest.getFields().put("name", FieldUtil.createStringField("2015 v1.1 new branch"));
			NodeResponse response = call(() -> client().updateNode(PROJECT_NAME, uuid, updateRequest, new VersioningParametersImpl().setBranch(
				newBranch.getName())));
			assertEquals("1.1", response.getVersion());

			// create version 1.1 in initial branch (1.0 is the current published en node)
			updateRequest.getFields().put("name", FieldUtil.createStringField("2015 v1.1 initial branch"));
			updateRequest.setVersion("1.0");
			response = call(() -> client().updateNode(PROJECT_NAME, uuid, updateRequest, new VersioningParametersImpl().setBranch(initialBranch()
				.getName())));
			assertEquals("1.1", response.getVersion());

			// create version 0.2 in new branch
			updateRequest.getFields().put("name", FieldUtil.createStringField("2015 v1.2 new branch"));
			updateRequest.setVersion("1.1");
			response = call(() -> client().updateNode(PROJECT_NAME, uuid, updateRequest, new VersioningParametersImpl().setBranch(newBranch
				.getName())));
			assertEquals("1.2", response.getVersion());

			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setBranch(initialBranch().getName())
				.setVersion("0.1")))).as("Initial Branch Version").hasVersion("0.1").hasStringField("name", "2015");
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setBranch(newBranch.getName())
				.setVersion("1.1")))).as("New Branch Version").hasVersion("1.1").hasStringField("name", "2015 v1.1 new branch");
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setBranch(initialBranch().getName())
				.setVersion("1.1")))).as("Initial Branch Version").hasVersion("1.1").hasStringField("name", "2015 v1.1 initial branch");
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setBranch(newBranch.getName())
				.setVersion("1.2")))).as("New Branch Version").hasVersion("1.2").hasStringField("name", "2015 v1.2 new branch");
		}
	}

	/**
	 * Test reading a node with link resolving enabled. Ensure that the schema segment field of the node is not set.
	 */
	@Test
	public void testReadByUUIDWithLinkPathsAndNoSegmentFieldRef() {
		String nodeUuid = tx(tx -> { return folder("news").getUuid(); });
		String schemaUuid = tx(tx -> { return folder("news").getSchemaContainer().getUuid(); });
		SchemaResponse schema = call(() -> client().findSchemaByUuid(schemaUuid));

		waitForJobs(() -> {
			call(() -> client().updateSchema(schemaUuid, schema.toUpdateRequest().setSegmentField(null)));
		}, COMPLETED, 1);		

		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, nodeUuid, new NodeParametersImpl().setResolveLinks(
				LinkType.FULL), new VersioningParametersImpl().draft()));
			assertEquals(CURRENT_API_BASE_PATH + "/dummy/webroot/error/404", response.getPath());
			assertThat(response.getLanguagePaths()).containsEntry("en", CURRENT_API_BASE_PATH + "/dummy/webroot/error/404");
			assertThat(response.getLanguagePaths()).containsEntry("de", CURRENT_API_BASE_PATH + "/dummy/webroot/error/404");
	}

	@Test
	public void testReadByUUIDWithLinkPaths() {
		try (Tx tx = tx()) {
			HibNode node = folder("news");
			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().draft(),
				new NodeParametersImpl().setResolveLinks(LinkType.FULL)));
			assertThat(response.getAvailableLanguages().keySet()).containsExactly("de", "en");
			assertThat(response.getLanguagePaths()).containsEntry("en", CURRENT_API_BASE_PATH + "/dummy/webroot/News");
			assertThat(response.getLanguagePaths()).containsEntry("de", CURRENT_API_BASE_PATH + "/dummy/webroot/Neuigkeiten");
		}
	}

	@Test
	public void testReadBreadcrumbWithLangfallback() {
		String baseNodeUuid = tx(() -> project().getBaseNode().getUuid());

		// level 0
		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchema(new SchemaReferenceImpl().setName("folder"));
		request.setLanguage("en");
		request.getFields().put("name", FieldUtil.createStringField("english folder-0"));
		request.getFields().put("slug", FieldUtil.createStringField("english folder-0"));
		request.setParentNodeUuid(baseNodeUuid);
		NodeResponse response = call(() -> client().createNode(PROJECT_NAME, request));

		// level 1
		request.setParentNodeUuid(response.getUuid());
		request.getFields().put("name", FieldUtil.createStringField("english folder-1"));
		request.getFields().put("slug", FieldUtil.createStringField("english folder-1"));
		response = call(() -> client().createNode(PROJECT_NAME, request));

		// level 2
		request.setLanguage("de");
		request.setParentNodeUuid(response.getUuid());
		request.getFields().put("name", FieldUtil.createStringField("german folder-2"));
		request.getFields().put("slug", FieldUtil.createStringField("german folder-2"));
		response = call(() -> client().createNode(PROJECT_NAME, request));

		// Load the german folder
		String uuid = response.getUuid();
		response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new NodeParametersImpl().setResolveLinks(LinkType.FULL).setLanguages("de"),
			new VersioningParametersImpl().setVersion("draft")));

		List<NodeReference> breadcrumb = response.getBreadcrumb();
		assertEquals(CURRENT_API_BASE_PATH + "/dummy/webroot/english%20folder-0/english%20folder-1/german%20folder-2", response.getPath());
		assertEquals(CURRENT_API_BASE_PATH + "/dummy/webroot/", breadcrumb.get(0).getPath());
		assertEquals(CURRENT_API_BASE_PATH + "/dummy/webroot/english%20folder-0", breadcrumb.get(1).getPath());
		assertEquals(CURRENT_API_BASE_PATH + "/dummy/webroot/english%20folder-0/english%20folder-1", breadcrumb.get(2).getPath());
		assertEquals(CURRENT_API_BASE_PATH + "/dummy/webroot/english%20folder-0/english%20folder-1/german%20folder-2", breadcrumb.get(3).getPath());

	}

	@Test
	public void testReadByUUIDBreadcrumb() {
		try (Tx tx = tx()) {
			HibNode node = content("news_2014");
			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new NodeParametersImpl().setResolveLinks(
				LinkType.FULL), new VersioningParametersImpl().draft()));
			assertNull(response.getBreadcrumb().get(0).getDisplayName());
			assertEquals(response.getBreadcrumb().get(1).getUuid(), folder("news").getUuid());
			assertEquals("News", response.getBreadcrumb().get(1).getDisplayName());
			assertEquals(response.getBreadcrumb().get(2).getUuid(), folder("2014").getUuid());
			assertEquals("2014", response.getBreadcrumb().get(2).getDisplayName());
			assertEquals(response.getBreadcrumb().get(3).getUuid(), node.getUuid());
			assertEquals("News_2014 english title", response.getBreadcrumb().get(3).getDisplayName());
			assertEquals(CURRENT_API_BASE_PATH + "/dummy/webroot/", response.getBreadcrumb().get(0).getPath());
			assertEquals(CURRENT_API_BASE_PATH + "/dummy/webroot/News", response.getBreadcrumb().get(1).getPath());
			assertEquals(CURRENT_API_BASE_PATH + "/dummy/webroot/News/2014", response.getBreadcrumb().get(2).getPath());
			assertEquals(CURRENT_API_BASE_PATH + "/dummy/webroot/News/2014/News_2014.en.html", response.getBreadcrumb().get(3).getPath());
			assertEquals("Only four items should be listed in the breadcrumb", 4, response.getBreadcrumb().size());

			response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().draft()));
			assertNull(response.getBreadcrumb().get(0).getDisplayName());
			assertEquals(response.getBreadcrumb().get(1).getUuid(), folder("news").getUuid());
			assertEquals("News", response.getBreadcrumb().get(1).getDisplayName());
			assertEquals(response.getBreadcrumb().get(2).getUuid(), folder("2014").getUuid());
			assertEquals("2014", response.getBreadcrumb().get(2).getDisplayName());
			assertEquals(response.getBreadcrumb().get(3).getUuid(), response.getUuid());
			assertEquals("News_2014 english title", response.getBreadcrumb().get(3).getDisplayName());
			response.getBreadcrumb()
				.forEach(element -> assertNull("No path should be rendered since by default the linkType is OFF", element.getPath()));
			assertEquals("Only 4 items should be listed in the breadcrumb", 4, response.getBreadcrumb().size());
		}
	}

	@Test
	public void testReadBaseNode() throws Exception {
		try (Tx tx = tx()) {
			HibNode node = project().getBaseNode();
			String uuid = node.getUuid();
			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));
			assertNotNull(response);
			assertEquals("folder", response.getSchema().getName());
			assertThat(response.getAvailableLanguages().keySet()).containsExactly("en");
			assertEquals("en", response.getLanguage());

			response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new NodeParametersImpl().setResolveLinks(LinkType.FULL)));
			assertNotNull(response);
			assertEquals("folder", response.getSchema().getName());
			assertEquals(CURRENT_API_BASE_PATH + "/dummy/webroot/", response.getLanguagePaths().get("en"));
		}
	}

	@Test
	public void testReadNodeByUUIDLanguageFallback() {
		try (Tx tx = tx()) {
			HibNode node = folder("products");
			String uuid = node.getUuid();

			call(() -> client().takeNodeOffline(PROJECT_NAME, uuid, new PublishParametersImpl().setRecursive(true)));
			call(() -> client().deleteNode(PROJECT_NAME, uuid, "en", new DeleteParametersImpl().setRecursive(true)));
			tx.success();
		}

		try (Tx tx = tx()) {
			HibNode node = folder("products");
			String uuid = node.getUuid();

			// Request the node with various language parameter values. Fallback to "de"
			NodeParametersImpl parameters = new NodeParametersImpl();
			parameters.setLanguages("dv,nl,de,en");
			VersioningParameters versionParams = new VersioningParametersImpl().draft();
			NodeResponse restNode = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, parameters, versionParams));
			assertThat(folder("products")).matches(restNode);

			// Ensure "de" version was returned
			StringField field = restNode.getFields().getStringField("slug");
			String nameText = field.getString();
			assertEquals("Produkte", nameText);
		}

	}

	@Test
	public void testReadNodeByUUIDSingleLanguage() throws Exception {
		try (Tx tx = tx()) {
			HibNode node = folder("products");
			String uuid = node.getUuid();

			NodeParametersImpl parameters = new NodeParametersImpl();
			parameters.setLanguages("de");
			VersioningParameters versionParams = new VersioningParametersImpl().draft();
			NodeResponse restNode = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, parameters, versionParams));
			assertThat(folder("products")).matches(restNode);

			StringField field = restNode.getFields().getStringField("slug");
			String nameText = field.getString();
			assertEquals("Produkte", nameText);
		}
	}

	@Test
	public void testReadNodeByUUIDNoLanguage() throws Exception {
		HibNode node;
		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();
			RoleDao roleDao = tx.roleDao();
			// Create node with nl language
			HibProject project = tx.projectDao().findByUuid(project().getUuid());
			HibNode parentNode = nodeDao.findByUuid(project, folder("products").getUuid());
			HibLanguage languageNl = tx.languageDao().findByLanguageTag("nl");
			HibSchemaVersion version = tx.schemaDao().findByUuid(schemaContainer("content").getUuid()).getLatestVersion();
			HibUser user = tx.userDao().findByUuid(user().getUuid());
			node = nodeDao.create(parentNode, user, version, project);
			HibNodeFieldContainer englishContainer = boot().contentDao().createFieldContainer(node, languageNl.getLanguageTag(),
				node.getProject().getLatestBranch(), user());
			englishContainer.createString("teaser").setString("name");
			englishContainer.createString("title").setString("title");
			//englishContainer.createString("displayName").setString("displayName");
			englishContainer.createString("slug").setString("filename.nl.html");
			englishContainer.createHTML("content").setHtml("nl content");
			roleDao.grantPermissions(role(), node, READ_PERM);
			tx.success();
		}

		// Request the node in english en
		try (Tx tx = tx()) {
			NodeParametersImpl parameters = new NodeParametersImpl();
			parameters.setLanguages("en");
			VersioningParameters versionParams = new VersioningParametersImpl().draft();
			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), parameters, versionParams));
			assertThat(response.getLanguage()).as("Node language").isNull();
			assertThat(response.getAvailableLanguages().keySet()).as("Available languages").containsOnly("nl");
			assertThat(response.getFields()).as("Node Fields").isNull();
			assertNotNull(response.getProject());
			assertEquals(project().getUuid(), response.getProject().getUuid());
			assertEquals(project().getName(), response.getProject().getName());
		}

	}

	@Test
	public void testReadNodeWithBogusLanguageCode() throws Exception {
		try (Tx tx = tx()) {

			HibNode node = folder("2015");
			String uuid = node.getUuid();
			assertNotNull(node);
			assertNotNull(node.getUuid());

			NodeParametersImpl parameters = new NodeParametersImpl();
			parameters.setLanguages("blabla", "edgsdg");
			VersioningParameters versionParams = new VersioningParametersImpl().draft();

			assertThat(trackingSearchProvider()).recordedStoreEvents(0);
			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, parameters, versionParams),
				BAD_REQUEST, "error_language_not_found", "blabla");
			assertThat(trackingSearchProvider()).recordedStoreEvents(0);
		}
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		String uuid;
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			HibNode node = folder("2015");
			uuid = node.getUuid();
			roleDao.revokePermissions(role(), node, READ_PERM);
			roleDao.revokePermissions(role(), node, READ_PUBLISHED_PERM);
			tx.success();
		}
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft()), FORBIDDEN, "error_missing_perm", uuid,
			READ_PERM.getRestPerm().getName());

	}

	@Test
	public void testReadNodeByBogusUUID() throws Exception {
		call(() -> client().findNodeByUuid(PROJECT_NAME, "bogusUUID"), NOT_FOUND, "object_not_found_for_uuid", "bogusUUID");
	}

	@Test
	public void testReadNodeByInvalidUUID() throws Exception {
		String uuid = "dde8ba06bb7211e4897631a9ce2772f5";
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid), NOT_FOUND, "object_not_found_for_uuid", uuid);
	}

	@Test
	public void testFindOrCreateNode() throws Exception {
		String uuid = "dde8ba06bb7211e4897631a9ce2772f5";

		NodeResponse node = client().findNodeByUuid(PROJECT_NAME, uuid).toSingle()
			.compose(onErrorCodeResumeNext(404, createBinaryContent(uuid)))
			.blockingGet();

		assertThat(node).hasUuid(uuid);
	}

	@Test
	@Override
	public void testPermissionResponse() {
		NodeResponse node = client().findNodes(PROJECT_NAME).blockingGet().getData().get(0);
		assertThat(node.getPermissions()).hasPublishPermsSet();
	}

	// Update

	@Test
	@Override
	public void testUpdate() throws GenericRestException, Exception {
		final String newSlug = "english renamed name";
		final String contentSchemaUuid = tx(() -> schemaContainer("content").getUuid());

		// 1. Load Ids / Objects
		String uuid = tx(() -> content("concorde").getUuid());
		final HibNode node = tx(() -> content("concorde"));
		HibNodeFieldContainer origContainer = tx(tx -> {
			ContentDao contentDao = tx.contentDao();
			GroupDao groupRoot = tx.groupDao();
			HibNode prod = content("concorde");
			HibNodeFieldContainer container = contentDao.getLatestDraftFieldContainer(prod, english());
			assertEquals("Concorde_english_name", container.getString("teaser").getString());
			assertEquals("Concorde english title", container.getString("title").getString());
			UserInfo userInfo = data().createUserInfo("dummy", "Dummy Firstname", "Dummy Lastname");
			groupRoot.addUser(group(), userInfo.getUser());
			return container;
		});

		// Now login with a different user to see that the editor field gets updated correctly
		client().logout().blockingGet();
		client().setLogin("dummy", "test123");
		client().login().blockingGet();

		// 2. Prepare the update request (change name field of english node)
		NodeUpdateRequest request = new NodeUpdateRequest();
		request.setLanguage("en");
		request.setVersion("0.1");
		request.getFields().put("slug", FieldUtil.createStringField(newSlug));

		// 3. Invoke update
		searchProvider().clear().blockingAwait();

		expect(NODE_UPDATED).match(1, NodeMeshEventModel.class, event -> {
			assertThat(event)
				.hasProject(PROJECT_NAME, projectUuid())
				.hasLanguage("en").hasSchema("content", contentSchemaUuid)
				.hasUuid(uuid)
				.hasBranchUuid(initialBranchUuid());
		});

		NodeResponse restNode = call(() -> client().updateNode(PROJECT_NAME, uuid, request, new NodeParametersImpl().setLanguages("en", "de")));
		awaitEvents();

		// Assert updater information
		assertEquals("Dummy Firstname", restNode.getEditor().getFirstName());
		assertEquals("Dummy Lastname", restNode.getEditor().getLastName());

		String projectUuid = tx(() -> project().getUuid());
		String branchUuid = tx(() -> project().getLatestBranch().getUuid());
		String schemaContainerVersionUuid = tx(tx -> {
			ContentDao contentDao = tx.contentDao();
			return contentDao.getLatestDraftFieldContainer(node, english()).getSchemaContainerVersion().getUuid();
		});

		waitForSearchIdleEvent();
		assertThat(trackingSearchProvider()).hasStore(ContentDao.composeIndexName(projectUuid, branchUuid, schemaContainerVersionUuid,
			ContainerType.DRAFT, null), ContentDao.composeDocumentId(uuid, "en"));
		assertThat(trackingSearchProvider()).hasEvents(1, 0, 0, 0, 0);

		// 4. Assert that new version 1.1 was created. (1.0 was the published 0.1 draft)
		assertThat(restNode).as("update response").isNotNull().hasLanguage("en").hasVersion("1.1").hasStringField("slug", newSlug).hasStringField(
			"title", "Concorde english title");

		// 5. Assert graph changes
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();

			// First check whether the objects we check are the correct ones
			assertEquals("The original container should be 1.0 (the latest published version)", "1.0", origContainer.getVersion().toString());
			HibNodeFieldContainer container = contentDao.getLatestDraftFieldContainer(node, english());
			assertEquals("The loaded container did not match the latest version.", "1.1", container.getVersion().toString());

			// Assert applied changes
			assertEquals("The string field was not updated within the new container", newSlug, container.getString("slug").getString());
			assertEquals("Concorde english title", container.getString("title").getString());

			// Assert that the containers were linked together as expected
			// 0.1 -> 1.0 -> 1.1
			assertThat(container).as("new container").hasPrevious(origContainer);
			assertThat(container).as("new container").isLast();
			assertThat(origContainer).as("orig container").hasNext(container);
			assertThat(origContainer.getPreviousVersion()).isFirst();

			// Verify that exactly the selected language was updated
			String indexName = ContentDao.composeIndexName(project().getUuid(), project().getLatestBranch().getUuid(), origContainer
				.getSchemaContainerVersion().getUuid(), ContainerType.DRAFT, null);
			String documentId = ContentDao.composeDocumentId(uuid, "en");
			assertThat(trackingSearchProvider()).hasStore(indexName, documentId);
			assertThat(trackingSearchProvider()).recordedStoreEvents(1);
		}

	}

	@Test
	public void testUpdateCreateLanguage() {
		final String germanName = "Zweitausendfünfzehn";
		final HibNode node = tx(() -> folder("2015"));
		final String uuid = tx(() -> folder("2015").getUuid());

		NodeUpdateRequest request = new NodeUpdateRequest();
		request.setLanguage("de");
		request.setVersion("0.1");
		request.getFields().put("name", FieldUtil.createStringField(germanName));

		String projectUuid = tx(() -> project().getUuid());
		String branchUuid = tx(() -> project().getLatestBranch().getUuid());
		String schemaContainerVersionUuid = tx(tx -> {
			ContentDao contentDao = tx.contentDao();
			return contentDao.getLatestDraftFieldContainer(node, english()).getSchemaContainerVersion().getUuid();
		});
		String schemaUuid = tx(() -> schemaContainer("folder").getUuid());

		searchProvider().clear().blockingAwait();

		expect(NODE_CONTENT_CREATED).match(1, NodeMeshEventModel.class, event -> {
			assertEquals(branchUuid, event.getBranchUuid());
			assertEquals(uuid, event.getUuid());
			assertEquals("de", event.getLanguageTag());

			ProjectReference projectRef = event.getProject();
			assertNotNull(projectRef);
			assertEquals(PROJECT_NAME, projectRef.getName());
			assertEquals(projectUuid(), projectRef.getUuid());

			SchemaReference schemaRef = event.getSchema();
			assertNotNull(schemaRef);
			assertEquals(schemaUuid, schemaRef.getUuid());
			assertEquals("folder", schemaRef.getName());
		});
		NodeResponse restNode = call(() -> client().updateNode(PROJECT_NAME, uuid, request, new NodeParametersImpl().setLanguages("de")));
		awaitEvents();
		assertEquals("de", restNode.getLanguage());
		waitForSearchIdleEvent();
		// Only the new language container is stored in the index. The existing one does not need to be updated since it does not reference other languages
		assertThat(trackingSearchProvider()).hasStore(ContentDao.composeIndexName(projectUuid, branchUuid, schemaContainerVersionUuid,
			ContainerType.DRAFT, null), ContentDao.composeDocumentId(uuid, "de"));
		assertThat(trackingSearchProvider()).hasEvents(1, 0, 0, 0, 0);
	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		HibNode node = folder("2015");
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			roleDao.revokePermissions(role(), node, UPDATE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			String uuid = node.getUuid();
			NodeUpdateRequest request = new NodeUpdateRequest();
			request.setLanguage("en");
			call(() -> client().updateNode(PROJECT_NAME, uuid, request), FORBIDDEN, "error_missing_perm", uuid, UPDATE_PERM.getRestPerm().getName());
		}
	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {

		try (Tx tx = tx()) {
			NodeUpdateRequest request = new NodeUpdateRequest();
			request.setLanguage("en");

			NodeParametersImpl parameters = new NodeParametersImpl();
			parameters.setLanguages("en", "de");

			call(() -> client().updateNode(PROJECT_NAME, "bogus", request, parameters), BAD_REQUEST, "error_illegal_uuid", "bogus");
		}
	}

	@Test
	public void testCreateNodeWithExtraField() throws UnknownHostException, InterruptedException {
		try (Tx tx = tx()) {
			HibNode parentNode = folder("news");
			String uuid = parentNode.getUuid();
			assertNotNull(parentNode);
			assertNotNull(parentNode.getUuid());

			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReferenceImpl().setName("content").setUuid(schemaContainer("content").getUuid()));
			request.setLanguage("en");
			request.getFields().put("title", FieldUtil.createStringField("some title"));
			request.getFields().put("extrafield", FieldUtil.createStringField("some extra field value"));
			request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setParentNodeUuid(uuid);

			call(() -> client().createNode(PROJECT_NAME, request), BAD_REQUEST, "node_unhandled_fields", "content", "[extrafield]");
		}

	}

	@Test
	public void testCreateNodeWithMissingRequiredField() {
		try (Tx tx = tx()) {
			HibNode parentNode = folder("news");
			String uuid = parentNode.getUuid();
			assertNotNull(parentNode);
			assertNotNull(parentNode.getUuid());

			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReferenceImpl().setName("content").setUuid(schemaContainer("content").getUuid()));
			request.setLanguage("en");
			// non required title field is missing
			// required name field is missing
			request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setParentNodeUuid(uuid);

			call(() -> client().createNode(PROJECT_NAME, request), BAD_REQUEST, "node_error_missing_required_field_value", "slug", "content");
		}
	}

	@Test
	public void testCreateNodeWithMissingField() throws UnknownHostException, InterruptedException {
		try (Tx tx = tx()) {
			HibNode parentNode = folder("news");
			String uuid = parentNode.getUuid();
			assertNotNull(parentNode);
			assertNotNull(parentNode.getUuid());

			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReferenceImpl().setName("content").setUuid(schemaContainer("content").getUuid()));
			request.setLanguage("en");
			// title field is missing
			request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setParentNodeUuid(uuid);

			NodeResponse response = call(() -> client().createNode(PROJECT_NAME, request));
			assertNotNull(response);
		}
	}

	@Test
	public void testUpdateNodeWithExtraField2() throws GenericRestException, Exception {
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNode node = folder("2015");
			String uuid = node.getUuid();

			NodeUpdateRequest request = new NodeUpdateRequest();
			request.setLanguage("en");
			request.setVersion("0.1");
			final String newName = "english renamed name";
			final String newDisplayName = "display name changed";

			request.getFields().put("name", FieldUtil.createStringField(newName));

			// Add another field which has not been specified in the content schema
			request.getFields().put("someField", FieldUtil.createStringField(newDisplayName));

			NodeParametersImpl parameters = new NodeParametersImpl();
			parameters.setLanguages("de", "en");
			call(() -> client().updateNode(PROJECT_NAME, uuid, request, parameters), BAD_REQUEST, "node_unhandled_fields", "folder", "[someField]");

			HibNodeFieldContainer englishContainer = contentDao.getLatestDraftFieldContainer(folder("2015"), english());
			assertNotEquals("The name should not have been changed.", newName, englishContainer.getString("name").getString());
		}
	}

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		HibNode node = content("concorde");
		String uuid = tx(() -> node.getUuid());
		String schemaUuid = tx(() -> schemaContainer("content").getUuid());
		assertTrue("The node is expected to be published", tx(tx -> {
			return tx.contentDao().isPublished(tx.contentDao().getFieldContainer(node, "en"));
		}));

		expect(NODE_DELETED).match(1, NodeMeshEventModel.class, event -> {
			assertThat(event)
				.uuidNotNull()
				.hasSchemaName("content")
				.hasSchemaUuid(schemaUuid);
		});

		call(() -> client().deleteNode(PROJECT_NAME, uuid));

		awaitEvents();
		waitForSearchIdleEvent();

		try (Tx tx = tx()) {
			assertElement(tx.nodeDao(), project(), uuid, false);
			// Delete Events after node delete. We expect 4 since both languages have draft and publish version.
			int deletes = 4;
			assertThat(trackingSearchProvider()).hasEvents(0, 0, deletes, 0, 0);
		}
	}

	/**
	 * Assert that the version history is not interrupted when invoking publish, unpublish and update end
	 */
	@Test
	public void testPublishUnPublishUpdateVersionConsistency() {
		String parentNodeUuid = tx(() -> folder("news").getUuid());

		// 1. Create node (en)
		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchema(new SchemaReferenceImpl().setName("content"));
		request.setLanguage("en");
		request.getFields().put("title", FieldUtil.createStringField("some title"));
		request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		request.getFields().put("slug", FieldUtil.createStringField("new-page.en.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.setParentNodeUuid(parentNodeUuid);
		NodeResponse response = call(() -> client().createNode(PROJECT_NAME, request));
		String uuid = response.getUuid();

		// 2. Update (de)
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("de");
		nodeUpdateRequest.setVersion("0.1");
		nodeUpdateRequest.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("old-page.de.html"));
		call(() -> client().updateNode(PROJECT_NAME, uuid, nodeUpdateRequest));

		nodeUpdateRequest.setVersion("0.1");
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("old-page.de2.html"));
		call(() -> client().updateNode(PROJECT_NAME, uuid, nodeUpdateRequest));

		nodeUpdateRequest.setVersion("0.2");
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("old-page.de3.html"));
		call(() -> client().updateNode(PROJECT_NAME, uuid, nodeUpdateRequest));

		// 3. Delete (de)
		call(() -> client().deleteNode(PROJECT_NAME, uuid, "de"));

		// 4. Update (de) again
		nodeUpdateRequest.setLanguage("de");
		nodeUpdateRequest.setVersion(null);
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("new-page.de1.html"));
		response = call(() -> client().updateNode(PROJECT_NAME, uuid, nodeUpdateRequest));

		nodeUpdateRequest.setVersion("0.1");
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("new-page.de2.html"));
		call(() -> client().updateNode(PROJECT_NAME, uuid, nodeUpdateRequest));

		nodeUpdateRequest.setVersion("0.2");
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("new-page.de3.html"));
		call(() -> client().updateNode(PROJECT_NAME, uuid, nodeUpdateRequest));
	}

	@Test
	public void testCreateInBranchWithoutParent() throws Exception {
		HibBranch newBranch = createBranch("newbranch", true);

		try (Tx tx = tx()) {
			// create a new branch
			HibProject project = project();
			HibBranch initialBranch = project.getInitialBranch();

			// create node in one branch
			HibNode node = content("concorde");
			NodeCreateRequest parentRequest = new NodeCreateRequest();
			parentRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
			parentRequest.setLanguage("en");
			parentRequest.getFields().put("slug", FieldUtil.createStringField("some slug"));
			parentRequest.setParentNodeUuid(node.getUuid());
			NodeResponse parentNode = call(
				() -> client().createNode(PROJECT_NAME, parentRequest, new VersioningParametersImpl().setBranch(newBranch.getName())));

			// create child in same branch
			NodeCreateRequest sameBranchChildRequest = new NodeCreateRequest();
			sameBranchChildRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
			sameBranchChildRequest.setLanguage("en");
			sameBranchChildRequest.getFields().put("slug", FieldUtil.createStringField("some slug"));
			sameBranchChildRequest.setParentNodeUuid(parentNode.getUuid());
			call(() -> client().createNode(PROJECT_NAME, sameBranchChildRequest, new VersioningParametersImpl().setBranch(newBranch.getName())));

			// try to create node with same uuid in other branch with first node as parent
			NodeCreateRequest childRequest = new NodeCreateRequest();
			childRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
			childRequest.setLanguage("en");
			childRequest.getFields().put("slug", FieldUtil.createStringField("some slug"));
			childRequest.setParentNodeUuid(parentNode.getUuid());
			call(() -> client().createNode(PROJECT_NAME, childRequest, new VersioningParametersImpl().setBranch(initialBranch.getName())), NOT_FOUND,
				"object_not_found_for_uuid", parentNode.getUuid());

			tx.success();
		}
	}

	@Test
	public void testCreateInBranchSameUUIDWithoutParent() throws Exception {
		HibBranch initialBranch;
		HibBranch newBranch;

		waitForJobs(() -> {
			call(() -> client().createBranch(PROJECT_NAME, new BranchCreateRequest().setName("newbranch")));
		}, COMPLETED, 1);
		try (Tx tx = tx()) {
			// create a new branch
			HibProject project = project();
			initialBranch = reloadBranch(project.getInitialBranch());
			newBranch = tx.branchDao().findByName(project, "newbranch");
		}

		try (Tx tx = tx()) {
			// create node in one branch
			HibNode node = content("concorde");
			NodeCreateRequest parentRequest = new NodeCreateRequest();
			parentRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
			parentRequest.setLanguage("en");
			parentRequest.getFields().put("slug", FieldUtil.createStringField("some slug"));
			parentRequest.setParentNodeUuid(node.getUuid());
			NodeResponse parentNode = call(
				() -> client().createNode(PROJECT_NAME, parentRequest, new VersioningParametersImpl().setBranch(newBranch.getName())));

			// create child in same branch
			NodeCreateRequest sameBranchChildRequest = new NodeCreateRequest();
			sameBranchChildRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
			sameBranchChildRequest.setLanguage("en");
			sameBranchChildRequest.getFields().put("slug", FieldUtil.createStringField("some slug"));
			sameBranchChildRequest.setParentNodeUuid(parentNode.getUuid());
			NodeResponse sameBranchChildResponse = call(
				() -> client().createNode(PROJECT_NAME, sameBranchChildRequest, new VersioningParametersImpl().setBranch(newBranch.getName())));

			// try to create node with same uuid in other branch with first node as parent
			NodeCreateRequest childRequest = new NodeCreateRequest();
			childRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
			childRequest.setLanguage("en");
			childRequest.getFields().put("slug", FieldUtil.createStringField("some slug"));
			childRequest.setParentNodeUuid(parentNode.getUuid());
			call(() -> client().createNode(sameBranchChildResponse.getUuid(), PROJECT_NAME, childRequest,
				new VersioningParametersImpl().setBranch(initialBranch.getName())), NOT_FOUND, "object_not_found_for_uuid", parentNode.getUuid());

			tx.success();
		}
	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		String uuid;
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			HibNode node = folder("2015");
			uuid = node.getUuid();
			roleDao.revokePermissions(role(), node, DELETE_PERM);
			tx.success();
		}

		call(() -> client().deleteNode(PROJECT_NAME, uuid), FORBIDDEN, "error_missing_perm", uuid, DELETE_PERM.getRestPerm().getName());
		try (Tx tx = tx()) {
			assertNotNull(boot().nodeDao().findByUuid(project(), uuid));
		}
	}

	@Test
	public void testConflictByUpdateAdditionalLanguage() {
		try (Tx tx = tx()) {
			HibNode node = folder("2015");
			String nodeUuid = node.getUuid();

			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("de");
			update.getFields().put("slug", FieldUtil.createStringField("2015"));
			call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update), CONFLICT, "node_conflicting_segmentfield_update", "slug", "2015");
			// TODO also assert message properties
		}
	}

	@Test
	public void testRootNodeBreadcrumb() {
		MeshWebrootResponse node = client().webroot(PROJECT_NAME, "/").toSingle().blockingGet();
		List<NodeReference> breadcrumb = node.getNodeResponse().getBreadcrumb();
		assertEquals(1, breadcrumb.size());
		assertEquals(node.getNodeResponse().getUuid(), breadcrumb.get(0).getUuid());
	}
}
