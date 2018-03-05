package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.expectException;
import static com.gentics.mesh.test.ClientHelper.validateDeletion;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.util.MeshAssert.assertElement;
import static com.gentics.mesh.test.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.test.util.MeshAssert.failingLatch;
import static com.gentics.mesh.test.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.demo.UserInfo;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.impl.DeleteParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicRestTestcases;
import com.gentics.mesh.util.UUIDUtil;
import com.gentics.mesh.util.VersionNumber;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
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

		assertThat(trackingSearchProvider()).recordedStoreEvents(0);
		NodeResponse restNode = call(() -> client().createNode(PROJECT_NAME, request));
		assertThat(restNode).matches(request);
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
		assertThat(trackingSearchProvider()).recordedStoreEvents(1);
	}

	@Test
	public void testCreateMultiple() {
		// TODO migrate test to performance tests
		try (Tx tx = tx()) {
			Node parentNode = folder("news");
			String uuid = parentNode.getUuid();
			assertNotNull(parentNode);
			assertNotNull(parentNode.getUuid());

			long start = System.currentTimeMillis();
			for (int i = 1; i < 100; i++) {
				trackingSearchProvider().reset();
				NodeCreateRequest request = new NodeCreateRequest();
				request.setSchema(new SchemaReferenceImpl().setName("content").setUuid(schemaContainer("content").getUuid()));
				request.setLanguage("en");
				request.getFields().put("title", FieldUtil.createStringField("some title " + i));
				request.getFields().put("teaser", FieldUtil.createStringField("some teaser " + i));
				request.getFields().put("slug", FieldUtil.createStringField("new-page_" + i + ".html"));
				request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
				request.setParentNodeUuid(uuid);

				MeshResponse<NodeResponse> future = client().createNode(PROJECT_NAME, request).invoke();
				latchFor(future);
				assertSuccess(future);
				long duration = System.currentTimeMillis() - start;
				System.out.println("Duration:" + i + " " + (duration / i));
			}
		}
	}

	@Test
	@Override
	public void testCreate() throws Exception {

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
			assertThat(trackingSearchProvider()).recordedStoreEvents(0);
			NodeResponse restNode = call(() -> client().createNode(PROJECT_NAME, request));
			assertThat(restNode).matches(request);
			assertThat(trackingSearchProvider()).recordedStoreEvents(1);
		}
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
			role().revokePermissions(folder("news"), CREATE_PERM);
			tx.success();
		}

		call(() -> client().createNode(PROJECT_NAME, request), FORBIDDEN, "error_missing_perm", parentNodeUuid);

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
		String nodeUuid = tx(() -> project().getUuid());
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
			call(() -> client().createNode(nodeUuid, PROJECT_NAME, request), INTERNAL_SERVER_ERROR, "error_internal");
		}
	}

	@Test
	public void testCreateForReleaseByName() {
		try (Tx tx = tx()) {
			Project project = project();
			Release initialRelease = project.getReleaseRoot().getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("newrelease", user());

			Node parentNode = folder("news");
			String uuid = parentNode.getUuid();
			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReferenceImpl().setName("content").setUuid(schemaContainer("content").getUuid()));
			request.setLanguage("en");
			request.getFields().put("title", FieldUtil.createStringField("some title"));
			request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setParentNodeUuid(uuid);

			NodeResponse nodeResponse = call(() -> client().createNode(project.getName(), request, new VersioningParametersImpl().setRelease(
				initialRelease.getName())));

			Node newNode = meshRoot().getNodeRoot().findByUuid(nodeResponse.getUuid());
			for (ContainerType type : Arrays.asList(ContainerType.INITIAL, ContainerType.DRAFT)) {
				assertThat(newNode.getGraphFieldContainer("en", initialRelease.getUuid(), type)).as(type + " Field container for initial release")
					.isNotNull().hasVersion("0.1");
				assertThat(newNode.getGraphFieldContainer("en", newRelease.getUuid(), type)).as(type + " Field Container for new release").isNull();
			}
		}
	}

	@Test
	public void testCreateForReleaseByUuid() {
		try (Tx tx = tx()) {
			Project project = project();
			Release initialRelease = project.getReleaseRoot().getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("newrelease", user());

			Node parentNode = folder("news");
			String uuid = parentNode.getUuid();
			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReferenceImpl().setName("content").setUuid(schemaContainer("content").getUuid()));
			request.setLanguage("en");
			request.getFields().put("title", FieldUtil.createStringField("some title"));
			request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setParentNodeUuid(uuid);

			NodeResponse nodeResponse = call(() -> client().createNode(project.getName(), request, new VersioningParametersImpl().setRelease(
				initialRelease.getUuid())));

			Node newNode = meshRoot().getNodeRoot().findByUuid(nodeResponse.getUuid());
			for (ContainerType type : Arrays.asList(ContainerType.INITIAL, ContainerType.DRAFT)) {
				assertThat(newNode.getGraphFieldContainer("en", initialRelease.getUuid(), type)).as(type + " Field container for initial release")
					.isNotNull().hasVersion("0.1");
				assertThat(newNode.getGraphFieldContainer("en", newRelease.getUuid(), type)).as(type + " Field Container for new release").isNull();
			}
		}
	}

	@Test
	public void testCreateForLatestRelease() {
		Release newRelease;
		try (Tx tx = tx()) {
			Project project = project();
			newRelease = project.getReleaseRoot().create("newrelease", user());
			tx.success();
		}

		try (Tx tx = tx()) {
			Node parentNode = folder("news");
			String uuid = parentNode.getUuid();
			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReferenceImpl().setName("content").setUuid(schemaContainer("content").getUuid()));
			request.setLanguage("en");
			request.getFields().put("title", FieldUtil.createStringField("some title"));
			request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setParentNodeUuid(uuid);

			NodeResponse nodeResponse = call(() -> client().createNode(PROJECT_NAME, request));

			Node newNode = meshRoot().getNodeRoot().findByUuid(nodeResponse.getUuid());

			for (ContainerType type : Arrays.asList(ContainerType.INITIAL, ContainerType.DRAFT)) {
				assertThat(newNode.getGraphFieldContainer("en", initialReleaseUuid(), type)).as(type + " Field container for initial release")
					.isNull();
				assertThat(newNode.getGraphFieldContainer("en", newRelease.getUuid(), type)).as(type + " Field Container for new release").isNotNull()
					.hasVersion("0.1");
			}
		}
	}

	@Test
	public void testCreateForBogusRelease() {
		try (Tx tx = tx()) {
			Project project = project();
			project.getReleaseRoot().create("newrelease", user());

			Node parentNode = folder("news");
			String uuid = parentNode.getUuid();
			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReferenceImpl().setName("content").setUuid(schemaContainer("content").getUuid()));
			request.setLanguage("en");
			request.getFields().put("title", FieldUtil.createStringField("some title"));
			request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setParentNodeUuid(uuid);

			call(() -> client().createNode(project.getName(), request, new VersioningParametersImpl().setRelease("bogusrelease")), BAD_REQUEST,
				"release_error_not_found", "bogusrelease");
		}
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
			assertThat(trackingSearchProvider()).recordedStoreEvents(1);
			assertThat(restNode).matches(request);

			Node node = meshRoot().getNodeRoot().findByUuid(restNode.getUuid());
			assertNotNull(node);
			assertThat(node).matches(request);

			// Load the node again
			restNode2 = call(() -> client().findNodeByUuid(PROJECT_NAME, restNode.getUuid(), new NodeParametersImpl().setLanguages("de"),
				new VersioningParametersImpl().draft()));

			// Delete the node
			call(() -> client().deleteNode(PROJECT_NAME, restNode2.getUuid()));

			Node deletedNode = meshRoot().getNodeRoot().findByUuid(restNode2.getUuid());
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
			request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setSchema(new SchemaReferenceImpl().setName("content").setUuid(schemaContainer("content").getUuid()));

			MeshResponse<NodeResponse> future = client().createNode(PROJECT_NAME, request).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "node_missing_parentnode_field");
		}
	}

	@Test
	public void testCreateNodeWithMissingSchemaPermission() {
		Node node = folder("news");

		try (Tx tx = tx()) {
			role().revokePermissions(schemaContainer("content"), READ_PERM);
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
			call(() -> client().createNode(PROJECT_NAME, request), FORBIDDEN, "error_missing_perm", schemaContainer("content").getUuid());
		}
	}

	@Test
	public void testCreateNodeWithMissingPermission() throws Exception {

		// Revoke create perm
		try (Tx tx = tx()) {
			role().revokePermissions(folder("news"), CREATE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			Node node = folder("news");
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
			call(() -> client().createNode(PROJECT_NAME, request), FORBIDDEN, "error_missing_perm", uuid);
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
		assertEquals(25, restResponse.getMetainfo().getPerPage());
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals(getNodeCount(), restResponse.getData().size());
	}

	@Test
	public void testReadMultipleAndAssertOrder() {
		try (Tx tx = tx()) {
			Node parentNode = folder("2015");
			int nNodes = 20;
			for (int i = 0; i < nNodes; i++) {
				Node node = parentNode.create(user(), schemaContainer("content").getLatestVersion(), project());
				node.createGraphFieldContainer(english(), initialRelease(), user());
				assertNotNull(node);
				role().grantPermissions(node, READ_PERM);
			}
			tx.success();
		}

		String firstUuid = null;
		for (int i = 0; i < 10; i++) {
			NodeListResponse response = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 100), new VersioningParametersImpl()
				.draft()));
			if (firstUuid == null) {
				firstUuid = response.getData().get(0).getUuid();
			}
			assertEquals("The first element in the page should not change but it changed in run {" + i + "}", firstUuid, response.getData().get(0)
				.getUuid());
		}

	}

	@Test
	@Override
	public void testReadMultiple() throws Exception {
		try (Tx tx = tx()) {
			Node parentNode = folder("2015");
			// Don't grant permissions to the no perm node. We want to make sure that this one will not be listed.
			Node noPermNode = parentNode.create(user(), schemaContainer("content").getLatestVersion(), project());
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
			int perPage = 11;
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
			assertEquals(perPage, restResponse.getMetainfo().getPerPage());

			List<NodeResponse> allNodes = new ArrayList<>();
			for (int page = 1; page <= totalPages; page++) {
				MeshResponse<NodeListResponse> pageFuture = client().findNodes(PROJECT_NAME, new PagingParametersImpl(page, perPage),
					new VersioningParametersImpl().draft()).invoke();
				latchFor(pageFuture);
				assertSuccess(pageFuture);
				restResponse = pageFuture.result();
				allNodes.addAll(restResponse.getData());
			}
			assertEquals("Somehow not all nodes were loaded when loading all pages.", totalNodes, allNodes.size());

			// Verify that the no_perm_node is not part of the response
			List<NodeResponse> filteredUserList = allNodes.parallelStream().filter(restNode -> restNode.getUuid().equals(noPermNodeUUID)).collect(
				Collectors.toList());
			assertTrue("The no perm node should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

			MeshResponse<NodeListResponse> pageFuture = client().findNodes(PROJECT_NAME, new PagingParametersImpl(-1, 25)).invoke();
			latchFor(pageFuture);
			expectException(pageFuture, BAD_REQUEST, "error_page_parameter_must_be_positive", "-1");

			pageFuture = client().findNodes(PROJECT_NAME, new PagingParametersImpl(0, 25)).invoke();
			latchFor(pageFuture);
			expectException(pageFuture, BAD_REQUEST, "error_page_parameter_must_be_positive", "0");

			pageFuture = client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, -1)).invoke();
			latchFor(pageFuture);
			expectException(pageFuture, BAD_REQUEST, "error_pagesize_parameter", "-1");

			NodeListResponse list = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(4242, 25), new VersioningParametersImpl()
				.draft()));
			assertEquals(4242, list.getMetainfo().getCurrentPage());
			assertEquals(0, list.getData().size());
			assertEquals(25, list.getMetainfo().getPerPage());
			assertEquals(2, list.getMetainfo().getPageCount());
			assertEquals(getNodeCount() + nNodes, list.getMetainfo().getTotalCount());
		}
	}

	@Test
	public void testReadMultipleOnlyMetadata() {
		NodeListResponse listResponse = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 0), new VersioningParametersImpl()
			.draft()));
		assertEquals(0, listResponse.getData().size());
	}

	@Test
	public void testReadNodesWithoutPermissions() throws Exception {

		// TODO add node that has no perms and check the response
		NodeListResponse restResponse = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 10), new VersioningParametersImpl()
			.draft()));

		int nElements = restResponse.getData().size();
		assertEquals("The amount of elements in the list did not match the expected count", 10, nElements);
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals(2, restResponse.getMetainfo().getPageCount());
		assertEquals(10, restResponse.getMetainfo().getPerPage());
		assertEquals(getNodeCount(), restResponse.getMetainfo().getTotalCount());
	}

	@Test
	public void testReadNodesForRelease() {

		Release newRelease;
		try (Tx tx = tx()) {
			newRelease = project().getReleaseRoot().create("newrelease", user());
			tx.success();
		}

		try (Tx tx = tx()) {
			NodeListResponse restResponse = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 1000),
				new VersioningParametersImpl().draft()));
			assertThat(restResponse.getData()).as("Node List for latest release").isEmpty();

			restResponse = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 1000), new VersioningParametersImpl().setRelease(
				initialRelease().getName()).draft()));
			assertThat(restResponse.getData()).as("Node List for initial release").hasSize(getNodeCount());

			// update a single node in the new release
			Node node = folder("2015");
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("en");
			update.getFields().put("name", FieldUtil.createStringField("2015 new release"));
			call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), update));

			// check whether there is one node in the new release now
			restResponse = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 1000), new VersioningParametersImpl().draft()));
			assertThat(restResponse.getData()).as("Node List for latest release").hasSize(1);

			restResponse = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 1000), new VersioningParametersImpl().draft()
				.setRelease(newRelease.getName())));
			assertThat(restResponse.getData()).as("Node List for latest release").hasSize(1);
		}

	}

	@Test
	public void testReadPublishedNodes() {
		try (Tx tx = tx()) {
			List<Node> nodes = Arrays.asList(folder("products"), folder("deals"), folder("news"), folder("2015"));

			// 1. Take all nodes offline
			call(() -> client().takeNodeOffline(PROJECT_NAME, project().getBaseNode().getUuid(), new PublishParametersImpl().setRecursive(true)));

			// 2. Assert that all nodes are offline. The findNodes method should not find any node because it searches for published nodes by default.
			NodeListResponse listResponse = call(() -> client().findNodes(PROJECT_NAME, new VersioningParametersImpl().published(),
				new PagingParametersImpl(1, 1000)));
			assertThat(listResponse.getData()).as("Published nodes list").isEmpty();

			// 3. Assert that the offline nodes are also not loadable if requests via uuid
			for (Node node : nodes) {
				call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().published()), NOT_FOUND,
					"node_error_published_not_found_for_uuid_release_language", node.getUuid(), "en", latestRelease().getUuid());
			}

			// Publish a few nodes
			nodes.stream().forEach(node -> call(() -> client().publishNode(PROJECT_NAME, node.getUuid())));

			// Read each node individually
			List<NodeResponse> publishedNodes = nodes.stream().map(node -> call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid()))).collect(
				Collectors.toList());
			assertThat(publishedNodes).hasSize(nodes.size());

			// Read a bunch of nodes
			listResponse = call(() -> client().findNodes(PROJECT_NAME, new VersioningParametersImpl().published(), new PagingParametersImpl(1,
				1000)));
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
			new PagingParametersImpl(1, 1000)));
		assertThat(listResponse.getData()).as("Published nodes list").isEmpty();

		// Republish all listed
		List<Node> nodes = tx(() -> {
			ArrayList<Node> list = new ArrayList<>(Arrays.asList(folder("products"), folder("deals"), folder("news"), folder("2015")));
			list.stream().forEach(node -> call(() -> client().publishNode(PROJECT_NAME, node.getUuid())));
			return list;
		});

		// Revoke permission on one folder after the other
		while (!nodes.isEmpty()) {
			Node folder = nodes.remove(0);
			tx((tx) -> {
				role().revokePermissions(folder, READ_PUBLISHED_PERM);
				role().revokePermissions(folder, READ_PERM);
				tx.success();
			});

			// Load all nodes and check whether they are readable
			List<NodeResponse> publishedNodes = nodes.stream().map(node -> {
				String uuid = tx(() -> node.getUuid());
				return call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().published()));
			}).collect(Collectors.toList());
			assertThat(publishedNodes).hasSize(nodes.size());

			listResponse = call(() -> client().findNodes(PROJECT_NAME, new VersioningParametersImpl().published(), new PagingParametersImpl(1,
				1000)));
			assertThat(listResponse.getData()).as("Published nodes list").usingElementComparatorOnFields("uuid").containsOnlyElementsOf(
				publishedNodes);
		}
	}

	@Test
	public void testReadPublishedNodesNoPermission2() {

		// Republish all listed nodes
		List<Node> nodes = tx(() -> {
			ArrayList<Node> list = new ArrayList<>(Arrays.asList(folder("products"), folder("deals"), folder("news"), folder("2015")));
			list.stream().forEach(node -> call(() -> client().publishNode(PROJECT_NAME, node.getUuid())));
			return list;
		});

		NodeListResponse initialListResponse = call(() -> client().findNodes(PROJECT_NAME, new VersioningParametersImpl().published(),
			new PagingParametersImpl(1, 1000)));

		int revoked = 0;
		for (Node node : nodes) {
			// Revoke the read perm but keep the read published perm on the node
			tx((tx) -> {
				role().revokePermissions(node, READ_PERM);
				role().grantPermissions(node, READ_PUBLISHED_PERM);
				tx.success();
			});
			revoked++;

			// Read the given node - It should still be readable
			String uuid = tx(() -> node.getUuid());
			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().published()));
			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft()), FORBIDDEN, "error_missing_perm", uuid);

			// Verify also that the read nodes endpoint still finds all nodes
			NodeListResponse listResponse = call(() -> client().findNodes(PROJECT_NAME, new VersioningParametersImpl().published(),
				new PagingParametersImpl(1, 1000)));
			assertThat(listResponse.getData()).as("Published nodes list").hasSameSizeAs(initialListResponse.getData());
			listResponse = call(() -> client().findNodes(PROJECT_NAME, new VersioningParametersImpl().draft(), new PagingParametersImpl(1, 1000)));
			assertThat(listResponse.getData()).as("Draft nodes list").hasSize(initialListResponse.getData().size() - revoked);
		}
	}

	@Test
	@Ignore
	public void testReadPublishedNodeNoPermission3() {
		String uuid = tx(() -> content().getUuid());
		NodeResponse draftResponse = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));
		call(() -> client().publishNode(PROJECT_NAME, uuid));
		NodeResponse publishedResponse = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().published()));
		assertEquals("Draft and publish versions should be the same since mesh automatically creates a new draft based on the published version.",
			draftResponse.getVersion(), publishedResponse.getVersion());

		tx((tx) -> {
			Node node = content();
			role().revokePermissions(node, READ_PERM);
			role().revokePermissions(node, CREATE_PERM);
			role().revokePermissions(node, DELETE_PERM);
			role().revokePermissions(node, UPDATE_PERM);
			role().revokePermissions(node, PUBLISH_PERM);
			role().grantPermissions(node, READ_PUBLISHED_PERM);
			tx.success();
		});

		// version=<default>
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid), FORBIDDEN, "error_missing_perm", uuid);
		// version=draft
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft()), FORBIDDEN, "error_missing_perm", uuid);
		// version=published
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().published()));
		// version=<draftversion>

		// TODO, FIXME Loading a node using the version will return the draft version and thus no permission is granted. Draft and publish versions use the same
		// version number.
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setVersion(draftResponse.getVersion())));

		// version=<publishedversion>
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setVersion(publishedResponse.getVersion())));

	}

	@Test
	@Ignore("Disabled until custom 404 handler has been added")
	public void testReadNodeWithBogusProject() {
		MeshResponse<NodeResponse> future = client().findNodeByUuid("BOGUS", "someUuuid").invoke();
		latchFor(future);
		expectException(future, BAD_REQUEST, "project_not_found", "BOGUS");
	}

	@Test
	@Ignore("Disabled since test is unstable - CL-246")
	public void testCreateUpdateReadDeleteMultithreaded() throws Exception {

		Logger log = LoggerFactory.getLogger(NodeEndpointTest.class);

		int nJobs = 200;
		try (Tx tx = tx()) {
			CountDownLatch latch = new CountDownLatch(nJobs);

			Node parentNode = folder("news");
			String uuid = parentNode.getUuid();

			long nNodesFound = meshRoot().getNodeRoot().computeCount();

			NodeCreateRequest createRequest = new NodeCreateRequest();
			createRequest.setSchema(new SchemaReferenceImpl().setName("content").setUuid(schemaContainer("content").getUuid()));
			createRequest.setLanguage("en");
			createRequest.getFields().put("title", FieldUtil.createStringField("some title"));
			createRequest.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			createRequest.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
			createRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			createRequest.setParentNodeUuid(uuid);

			NodeUpdateRequest updateRequest = new NodeUpdateRequest();
			updateRequest.setLanguage("en");
			updateRequest.getFields().put("teaser", FieldUtil.createStringField("UPDATED"));

			// Create various nodes and update them directly after creation. Ensure that update was successful.
			for (int i = 0; i < nJobs; i++) {
				log.info("Invoking createNode REST call for job {" + i + "}");
				MeshResponse<NodeResponse> createFuture = client().createNode(PROJECT_NAME, createRequest).invoke();

				createFuture.setHandler(rh -> {
					if (rh.failed()) {
						fail(rh.cause().getMessage());
					} else {
						log.info("Created {" + rh.result().getUuid() + "}");
						NodeResponse response = rh.result();
						MeshResponse<NodeResponse> updateFuture = client().updateNode(PROJECT_NAME, response.getUuid(), updateRequest).invoke();
						updateFuture.setHandler(uh -> {
							if (uh.failed()) {
								fail(uh.cause().getMessage());
							} else {
								log.info("Updated {" + uh.result().getUuid() + "}");
								MeshResponse<NodeResponse> readFuture = client().findNodeByUuid(PROJECT_NAME, uh.result().getUuid(),
									new VersioningParametersImpl().draft()).invoke();
								readFuture.setHandler(rf -> {
									if (rh.failed()) {
										fail(rh.cause().getMessage());
									} else {
										log.info("Read {" + rf.result().getUuid() + "}");
										MeshResponse<Void> deleteFuture = client().deleteNode(PROJECT_NAME, rf.result().getUuid()).invoke();
										deleteFuture.setHandler(df -> {
											if (df.failed()) {
												fail(df.cause().getMessage());
											} else {
												log.info("Deleted {" + rf.result().getUuid() + "}");
												latch.countDown();
											}
										});
									}

								});

							}
						});
					}
				});
				Thread.sleep(250);
				log.info("Invoked call create requests.");
			}

			failingLatch(latch);

			long nNodesFoundAfterRest = meshRoot().getNodeRoot().computeCount();
			assertEquals("All created nodes should have been created.", nNodesFound, nNodesFoundAfterRest);
			// for (Future<NodeResponse> future : set) {
			// latchFor(future);
			// assertSuccess(future);
			// }
		}
	}

	@Test
	@Override
	public void testCreateMultithreaded() throws InterruptedException {
		try (Tx tx = tx()) {
			Node parentNode = folder("news");
			String uuid = parentNode.getUuid();
			assertNotNull(parentNode);
			assertNotNull(parentNode.getUuid());

			int nJobs = 500;
			// CyclicBarrier barrier = new CyclicBarrier(nJobs);
			// Trx.enableDebug();
			// Trx.setBarrier(barrier);
			Set<MeshResponse<NodeResponse>> set = new HashSet<>();
			final AtomicInteger e = new AtomicInteger(0);
			for (int i = 0; i < nJobs; i++) {
				new Thread(() -> {
					NodeCreateRequest request = new NodeCreateRequest();
					request.setSchema(new SchemaReferenceImpl().setName("content"));
					request.setLanguage("en");
					request.getFields().put("title", FieldUtil.createStringField("some title"));
					request.getFields().put("teaser", FieldUtil.createStringField("some-teaser"));
					request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
					request.getFields().put("slug", FieldUtil.createStringField("new-page" + e.incrementAndGet() + ".html"));
					request.setParentNodeUuid(uuid);
					set.add(client().createNode(PROJECT_NAME, request).invoke());
				}).start();
			}

			Thread.sleep(10000);
			//
			// // Check each call response
			Set<String> uuids = new HashSet<>();
			for (MeshResponse<NodeResponse> future : set) {
				latchFor(future);
				assertSuccess(future);
				String currentUuid = future.result().getUuid();
				assertFalse("The rest api returned a node response with a uuid that was returned before. Each create request must always be atomic.",
					uuids.contains(currentUuid));
				uuids.add(currentUuid);
			}
			// Trx.disableDebug();
			// assertFalse("The barrier should not break. Somehow not all threads reached the barrier point.", barrier.isBroken());
		}
	}

	@Test
	@Override
	@Ignore("Multithreaded update is currently only possible for multiple nodes not a single node")
	public void testUpdateMultithreaded() throws InterruptedException {

		final String newName = "english renamed name";
		String uuid = tx(() -> folder("2015").getUuid());
		assertEquals("2015", tx(() -> folder("2015").getLatestDraftFieldContainer(english()).getString("slug").getString()));
		VersionNumber version = tx(() -> folder("2015").getLatestDraftFieldContainer(english()).getVersion());

		NodeUpdateRequest request = new NodeUpdateRequest();
		request.setLanguage("en");

		NodeParametersImpl parameters = new NodeParametersImpl();
		parameters.setLanguages("en", "de");

		int nJobs = 5;
		// CyclicBarrier barrier = new CyclicBarrier(nJobs);
		// Trx.enableDebug();
		// Trx.setBarrier(barrier);
		Set<MeshResponse<NodeResponse>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			System.out.println(version.getFullVersion());
			request.setVersion(version.getFullVersion());
			request.getFields().put("name", FieldUtil.createStringField(newName + ":" + i));
			set.add(client().updateNode(PROJECT_NAME, uuid, request, parameters).invoke());
			// version = version.nextDraft();
			// VersionNumber currentVersion = tx(() -> folder("2015").getLatestDraftFieldContainer(english()).getVersion());
			// System.out.println("CurrentVersion: " + currentVersion.getFullVersion());
		}

		for (MeshResponse<NodeResponse> response : set) {
			latchFor(response);
			assertSuccess(response);
		}
		// Trx.disableDebug();
		// assertFalse("The barrier should not break. Somehow not all threads reached the barrier point.", barrier.isBroken());

	}

	@Test
	@Override
	public void testDeleteByUUIDMultithreaded() {

		String uuid = tx(() -> folder("2015").getUuid());
		int nJobs = 6;

		// CyclicBarrier barrier = new CyclicBarrier(nJobs);
		// Trx.enableDebug();
		// Trx.setBarrier(barrier);
		Set<MeshResponse<Void>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(client().deleteNode(PROJECT_NAME, uuid, new DeleteParametersImpl().setRecursive(true)).invoke());
		}

		validateDeletion(set, null);
		// call(() -> getClient().deleteNode(PROJECT_NAME, uuid));

	}

	@Test
	@Override
	public void testReadByUuidMultithreaded() throws InterruptedException {
		int nJobs = 50;
		try (Tx tx = tx()) {
			Set<MeshResponse<NodeResponse>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(client().findNodeByUuid(PROJECT_NAME, folder("2015").getUuid(), new VersioningParametersImpl().draft()).invoke());
			}
			for (MeshResponse<NodeResponse> future : set) {
				latchFor(future);
				assertSuccess(future);
			}
		}
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws InterruptedException {
		int nJobs = 200;
		try (Tx tx = tx()) {
			Set<MeshResponse<NodeResponse>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(client().findNodeByUuid(PROJECT_NAME, folder("2015").getUuid(), new VersioningParametersImpl().draft()).invoke());
			}
			for (MeshResponse<NodeResponse> future : set) {
				latchFor(future);
				assertSuccess(future);
			}
			// Trx.disableDebug();
		}
	}

	@Test
	public void testReadByUuidWithRolePerms() {
		try (Tx tx = tx()) {
			Node node = folder("2015");
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
			String releaseUuid = project().getLatestRelease().getUuid();
			assertThat(folder("2015")).matches(response);
			assertNotNull(response.getParentNode());
			assertEquals(folder("2015").getParentNode(releaseUuid).getUuid(), response.getParentNode().getUuid());
			assertEquals("News", response.getParentNode().getDisplayName());
			assertEquals("en", response.getLanguage());
		}
	}

	@Test
	public void testReadByUUIDWithNoUser() throws Exception {
		String folderUuid = tx(() -> folder("2015").getUuid());
		// Remove the editor and creator references to simulate that the user has been deleted.
		try (Tx tx = tx()) {
			folder("2015").setCreated(null);
			folder("2015").getLatestDraftFieldContainer(english()).setEditor(null);
			tx.success();
		}

		call(() -> client().getNodePublishStatus(PROJECT_NAME, folderUuid));

		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, folderUuid, new VersioningParametersImpl().draft()));
		try (Tx tx = tx()) {
			String releaseUuid = project().getLatestRelease().getUuid();
			assertThat(folder("2015")).matches(response);
			assertNotNull(response.getParentNode());
			assertEquals(folder("2015").getParentNode(releaseUuid).getUuid(), response.getParentNode().getUuid());
			assertEquals("News", response.getParentNode().getDisplayName());
			assertEquals("en", response.getLanguage());
		}
	}

	@Test
	public void testReadVersionByNumber() {
		try (Tx tx = tx()) {
			Node node = folder("2015");
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
			Node node = folder("2015");
			String uuid = node.getUuid();

			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setVersion("bogus")), BAD_REQUEST,
				"error_illegal_version", "bogus");
		}
	}

	@Test
	public void testReadInexistentVersion() {
		try (Tx tx = tx()) {
			Node node = folder("2015");
			String uuid = node.getUuid();

			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setVersion("47.11")), NOT_FOUND,
				"object_not_found_for_version", "47.11");
		}
	}

	@Test
	public void testReadPublishedVersion() {
		String uuid = tx(() -> folder("2015").getUuid());
		String releaseUuid = tx(() -> project().getLatestRelease().getUuid());

		// 1. Take node offline
		call(() -> client().takeNodeOffline(PROJECT_NAME, uuid, new PublishParametersImpl().setRecursive(true)));

		// 2. Load node using published options.
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().published()), NOT_FOUND,
			"node_error_published_not_found_for_uuid_release_language", uuid, "en", releaseUuid);

		// 3. Publish the node again.
		call(() -> client().publishNode(PROJECT_NAME, uuid));

		// 4. Assert that the node can be found.
		NodeResponse nodeResponse = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));
		assertThat(nodeResponse).as("Published node").hasLanguage("en").hasVersion("2.0");
	}

	@Test
	public void testReadNodeForRelease() {
		Node node = folder("2015");
		Release newRelease;

		try (Tx tx = tx()) {
			newRelease = project().getReleaseRoot().create("newrelease", user());
			tx.success();
		}

		try (Tx tx = tx()) {
			String uuid = node.getUuid();
			NodeUpdateRequest updateRequest = new NodeUpdateRequest();
			updateRequest.setLanguage("en");
			updateRequest.getFields().put("name", FieldUtil.createStringField("2015 in new release"));
			call(() -> client().updateNode(PROJECT_NAME, uuid, updateRequest, new VersioningParametersImpl().setRelease(newRelease.getName())));

			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setRelease(initialRelease().getName())
				.draft()))).as("Initial Release Version").hasVersion("1.0").hasStringField("name", "2015");
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setRelease(newRelease.getName())
				.draft()))).as("New Release Version").hasVersion("0.1").hasStringField("name", "2015 in new release");
		}

	}

	@Test
	public void testReadNodeVersionForRelease() {
		String uuid;
		Node node = folder("2015");
		Release newRelease;

		try (Tx tx = tx()) {
			uuid = node.getUuid();
			Project project = project();
			newRelease = project.getReleaseRoot().create("newrelease", user());
			tx.success();
		}

		try (Tx tx = tx()) {
			NodeUpdateRequest updateRequest = new NodeUpdateRequest();
			updateRequest.setLanguage("en");

			// create version 0.1 in new release
			updateRequest.getFields().put("name", FieldUtil.createStringField("2015 v0.1 new release"));
			NodeResponse response = call(() -> client().updateNode(PROJECT_NAME, uuid, updateRequest, new VersioningParametersImpl().setRelease(
				newRelease.getName())));
			assertEquals("0.1", response.getVersion());

			// create version 1.1 in initial release (1.0 is the current published en node)
			updateRequest.getFields().put("name", FieldUtil.createStringField("2015 v1.1 initial release"));
			updateRequest.setVersion("1.0");
			response = call(() -> client().updateNode(PROJECT_NAME, uuid, updateRequest, new VersioningParametersImpl().setRelease(initialRelease()
				.getName())));
			assertEquals("1.1", response.getVersion());

			// create version 0.2 in new release
			updateRequest.getFields().put("name", FieldUtil.createStringField("2015 v0.2 new release"));
			updateRequest.setVersion("0.1");
			response = call(() -> client().updateNode(PROJECT_NAME, uuid, updateRequest, new VersioningParametersImpl().setRelease(newRelease
				.getName())));
			assertEquals("0.2", response.getVersion());

			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setRelease(initialRelease().getName())
				.setVersion("0.1")))).as("Initial Release Version").hasVersion("0.1").hasStringField("name", "2015");
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setRelease(newRelease.getName())
				.setVersion("0.1")))).as("New Release Version").hasVersion("0.1").hasStringField("name", "2015 v0.1 new release");
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setRelease(initialRelease().getName())
				.setVersion("1.1")))).as("Initial Release Version").hasVersion("1.1").hasStringField("name", "2015 v1.1 initial release");
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setRelease(newRelease.getName())
				.setVersion("0.2")))).as("New Release Version").hasVersion("0.2").hasStringField("name", "2015 v0.2 new release");
		}
	}

	/**
	 * Test reading a node with link resolving enabled. Ensure that the schema segment field of the node is not set.
	 */
	@Test
	public void testReadByUUIDWithLinkPathsAndNoSegmentFieldRef() {
		Node node = folder("news");
		try (Tx tx = tx()) {
			// Update the schema
			SchemaModel schema = node.getSchemaContainer().getLatestVersion().getSchema();
			schema.setSegmentField(null);
			node.getSchemaContainer().getLatestVersion().setSchema(schema);
			MeshInternal.get().serverSchemaStorage().clear();
			tx.success();
		}

		try (Tx tx = tx()) {
			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new NodeParametersImpl().setResolveLinks(
				LinkType.FULL), new VersioningParametersImpl().draft()));
			assertEquals("/api/v1/dummy/webroot/error/404", response.getPath());
			assertThat(response.getLanguagePaths()).containsEntry("en", "/api/v1/dummy/webroot/error/404");
			assertThat(response.getLanguagePaths()).containsEntry("de", "/api/v1/dummy/webroot/error/404");
		}

	}

	@Test
	public void testReadByUUIDWithLinkPaths() {
		try (Tx tx = tx()) {
			Node node = folder("news");
			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().draft(),
				new NodeParametersImpl().setResolveLinks(LinkType.FULL)));
			assertThat(response.getAvailableLanguages().keySet()).containsExactly("de", "en");
			assertThat(response.getLanguagePaths()).containsEntry("en", "/api/v1/dummy/webroot/News");
			assertThat(response.getLanguagePaths()).containsEntry("de", "/api/v1/dummy/webroot/Neuigkeiten");
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

		assertEquals("/api/v1/dummy/webroot/english%20folder-0/english%20folder-1/german%20folder-2", response.getPath());
		assertEquals("/api/v1/dummy/webroot/english%20folder-0/english%20folder-1", response.getBreadcrumb().getFirst().getPath());
		assertEquals("/api/v1/dummy/webroot/english%20folder-0", response.getBreadcrumb().getLast().getPath());

	}

	@Test
	public void testReadByUUIDBreadcrumb() {
		try (Tx tx = tx()) {
			Node node = content("news_2014");
			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new NodeParametersImpl().setResolveLinks(
				LinkType.FULL), new VersioningParametersImpl().draft()));
			assertTrue(response.getBreadcrumb().getFirst().getUuid().equals(folder("2014").getUuid()));
			assertTrue(response.getBreadcrumb().getFirst().getDisplayName().equals("2014"));
			assertTrue(response.getBreadcrumb().getLast().getUuid().equals(folder("news").getUuid()));
			assertTrue(response.getBreadcrumb().getLast().getDisplayName().equals("News"));
			assertEquals("/api/v1/dummy/webroot/News/2014", response.getBreadcrumb().getFirst().getPath());
			assertEquals("/api/v1/dummy/webroot/News", response.getBreadcrumb().getLast().getPath());
			assertEquals("Only two items should be listed in the breadcrumb", 2, response.getBreadcrumb().size());

			response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().draft()));
			assertTrue(response.getBreadcrumb().getFirst().getUuid().equals(folder("2014").getUuid()));
			assertTrue(response.getBreadcrumb().getFirst().getDisplayName().equals("2014"));
			assertTrue(response.getBreadcrumb().getLast().getUuid().equals(folder("news").getUuid()));
			assertTrue(response.getBreadcrumb().getLast().getDisplayName().equals("News"));
			assertNull("No path should be rendered since by default the linkType is OFF", response.getBreadcrumb().getFirst().getPath());
			assertNull("No path should be rendered since by default the linkType is OFF", response.getBreadcrumb().getLast().getPath());
			assertEquals("Only two items should be listed in the breadcrumb", 2, response.getBreadcrumb().size());
		}
	}

	@Test
	public void testReadBaseNode() throws Exception {
		try (Tx tx = tx()) {
			Node node = project().getBaseNode();
			String uuid = node.getUuid();
			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));
			assertNotNull(response);
			assertEquals("folder", response.getSchema().getName());
			assertThat(response.getAvailableLanguages().keySet()).containsExactly("en");
			assertEquals("en", response.getLanguage());

			response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new NodeParametersImpl().setResolveLinks(LinkType.FULL)));
			assertNotNull(response);
			assertEquals("folder", response.getSchema().getName());
			assertEquals("/api/v1/dummy/webroot/", response.getLanguagePaths().get("en"));
		}
	}

	@Test
	public void testReadNodeByUUIDLanguageFallback() {
		try (Tx tx = tx()) {
			Node node = folder("products");
			SearchQueueBatch batch = createBatch();
			node.getLatestDraftFieldContainer(english()).delete(batch);
			tx.success();
		}

		try (Tx tx = tx()) {
			Node node = folder("products");
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
			Node node = folder("products");
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
		Node node;
		try (Tx tx = tx()) {
			// Create node with nl language
			Node parentNode = folder("products");
			Language languageNl = meshRoot().getLanguageRoot().findByLanguageTag("nl");
			SchemaContainerVersion version = schemaContainer("content").getLatestVersion();
			node = parentNode.create(user(), version, project());
			NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(languageNl, node.getProject().getLatestRelease(), user());
			englishContainer.createString("teaser").setString("name");
			englishContainer.createString("title").setString("title");
			englishContainer.createString("displayName").setString("displayName");
			englishContainer.createString("slug").setString("filename.nl.html");
			englishContainer.createHTML("content").setHtml("nl content");
			role().grantPermissions(node, READ_PERM);
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
			assertThat(response.getFields()).as("Node Fields").isEmpty();
			assertNotNull(response.getProject());
			assertEquals(project().getUuid(), response.getProject().getUuid());
			assertEquals(project().getName(), response.getProject().getName());
		}

	}

	@Test
	public void testReadNodeWithBogusLanguageCode() throws Exception {
		try (Tx tx = tx()) {

			Node node = folder("2015");
			String uuid = node.getUuid();
			assertNotNull(node);
			assertNotNull(node.getUuid());

			NodeParametersImpl parameters = new NodeParametersImpl();
			parameters.setLanguages("blabla", "edgsdg");
			VersioningParameters versionParams = new VersioningParametersImpl().draft();

			assertThat(trackingSearchProvider()).recordedStoreEvents(0);
			MeshResponse<NodeResponse> future = client().findNodeByUuid(PROJECT_NAME, uuid, parameters, versionParams).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "error_language_not_found", "blabla");
			assertThat(trackingSearchProvider()).recordedStoreEvents(0);
		}
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		String uuid;
		try (Tx tx = tx()) {
			Node node = folder("2015");
			uuid = node.getUuid();
			role().revokePermissions(node, READ_PERM);
			tx.success();
		}
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft()), FORBIDDEN, "error_missing_perm", uuid);

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

	// Update

	@Test
	@Override
	public void testUpdate() throws GenericRestException, Exception {
		final String newSlug = "english renamed name";

		// 1. Load Ids / Objects
		String uuid = tx(() -> content("concorde").getUuid());
		final Node node = tx(() -> content("concorde"));
		NodeGraphFieldContainer origContainer = tx(() -> {
			Node prod = content("concorde");
			NodeGraphFieldContainer container = prod.getLatestDraftFieldContainer(english());
			assertEquals("Concorde_english_name", container.getString("teaser").getString());
			assertEquals("Concorde english title", container.getString("title").getString());
			UserInfo userInfo = data().createUserInfo("dummy", "Dummy Firstname", "Dummy Lastname");
			group().addUser(userInfo.getUser());
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
		NodeResponse restNode = call(() -> client().updateNode(PROJECT_NAME, uuid, request, new NodeParametersImpl().setLanguages("en", "de")));
		// Assert updater information
		assertEquals("Dummy Firstname", restNode.getEditor().getFirstName());
		assertEquals("Dummy Lastname", restNode.getEditor().getLastName());

		String projectUuid = tx(() -> project().getUuid());
		String releaseUuid = tx(() -> project().getLatestRelease().getUuid());
		String schemaContainerVersionUuid = tx(() -> node.getLatestDraftFieldContainer(english()).getSchemaContainerVersion().getUuid());

		assertThat(trackingSearchProvider()).hasStore(NodeGraphFieldContainer.composeIndexName(projectUuid, releaseUuid, schemaContainerVersionUuid,
			ContainerType.DRAFT), NodeGraphFieldContainer.composeDocumentId(uuid, "en"));
		assertThat(trackingSearchProvider()).hasEvents(1, 0, 0, 0);

		// 4. Assert that new version 1.1 was created. (1.0 was the published 0.1 draft)
		assertThat(restNode).as("update response").isNotNull().hasLanguage("en").hasVersion("1.1").hasStringField("slug", newSlug).hasStringField(
			"title", "Concorde english title");

		// 5. Assert graph changes
		try (Tx tx = tx()) {

			// First check whether the objects we check are the correct ones
			assertEquals("The original container should be 1.0 (the latest published version)", "1.0", origContainer.getVersion().toString());
			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
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
			String indexName = NodeGraphFieldContainer.composeIndexName(project().getUuid(), project().getLatestRelease().getUuid(), origContainer
				.getSchemaContainerVersion().getUuid(), ContainerType.DRAFT);
			String documentId = NodeGraphFieldContainer.composeDocumentId(uuid, "en");
			assertThat(trackingSearchProvider()).hasStore(indexName, documentId);
			assertThat(trackingSearchProvider()).recordedStoreEvents(1);
		}

	}

	@Test
	public void testUpdateCreateLanguage() {
		final String germanName = "Zweitausendfünfzehn";
		final Node node = tx(() -> folder("2015"));
		final String uuid = tx(() -> folder("2015").getUuid());

		NodeUpdateRequest request = new NodeUpdateRequest();
		request.setLanguage("de");
		request.setVersion("0.1");
		request.getFields().put("name", FieldUtil.createStringField(germanName));

		String projectUuid = tx(() -> project().getUuid());
		String releaseUuid = tx(() -> project().getLatestRelease().getUuid());
		String schemaContainerVersionUuid = tx(() -> node.getLatestDraftFieldContainer(english()).getSchemaContainerVersion().getUuid());

		searchProvider().clear().blockingAwait();
		NodeResponse restNode = call(() -> client().updateNode(PROJECT_NAME, uuid, request, new NodeParametersImpl().setLanguages("de")));
		assertEquals("de", restNode.getLanguage());
		// Only the new language container is stored in the index. The existing one does not need to be updated since it does not reference other languages
		assertThat(trackingSearchProvider()).hasStore(NodeGraphFieldContainer.composeIndexName(projectUuid, releaseUuid, schemaContainerVersionUuid,
			ContainerType.DRAFT), NodeGraphFieldContainer.composeDocumentId(uuid, "de"));

		assertThat(trackingSearchProvider()).hasEvents(1, 0, 0, 0);
	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		Node node = folder("2015");
		try (Tx tx = tx()) {
			role().revokePermissions(node, UPDATE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			String uuid = node.getUuid();
			NodeUpdateRequest request = new NodeUpdateRequest();
			request.setLanguage("en");
			call(() -> client().updateNode(PROJECT_NAME, uuid, request), FORBIDDEN, "error_missing_perm", uuid);
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
			Node parentNode = folder("news");
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
			Node parentNode = folder("news");
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
			Node parentNode = folder("news");
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
			Node node = folder("2015");
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

			NodeGraphFieldContainer englishContainer = folder("2015").getLatestDraftFieldContainer(english());
			assertNotEquals("The name should not have been changed.", newName, englishContainer.getString("name").getString());
		}
	}

	@Test
	public void testDeleteBaseNode() throws Exception {
		try (Tx tx = tx()) {
			Node node = project().getBaseNode();
			String uuid = node.getUuid();

			call(() -> client().deleteNode(PROJECT_NAME, uuid), METHOD_NOT_ALLOWED, "node_basenode_not_deletable");

			Node foundNode = meshRoot().getNodeRoot().findByUuid(uuid);
			assertNotNull("The node should still exist.", foundNode);
		}
	}

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		try (Tx tx = tx()) {
			Node node = content("concorde");
			String uuid = node.getUuid();
			call(() -> client().deleteNode(PROJECT_NAME, uuid));

			assertElement(meshRoot().getNodeRoot(), uuid, false);
			// Delete Events after node delete. We expect 4 since both languages have draft and publish version.
			assertThat(trackingSearchProvider()).hasEvents(0, 4, 0, 0);
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
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("new-page.de.html"));
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
	public void testDeleteForRelease() throws Exception {
		try (Tx tx = tx()) {
			// 1. get the node
			Node node = content("concorde");
			String uuid = node.getUuid();

			// 2. create new release
			Project project = project();
			Release initialRelease = project.getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("newrelease", user());

			// 3. migrate nodes
			meshDagger().releaseMigrationHandler().migrateRelease(newRelease, null);
			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft().setRelease(initialRelease.getUuid())));
			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft().setRelease(newRelease.getUuid())));

			// 4. delete node in new release
			call(() -> client().deleteNode(PROJECT_NAME, uuid, new VersioningParametersImpl().setRelease(newRelease.getUuid())));

			// 5. Assert
			assertElement(meshRoot().getNodeRoot(), uuid, true);
			assertThat(node.getGraphFieldContainers(initialRelease, ContainerType.DRAFT)).as("draft containers for initial release").isNotEmpty();
			assertThat(node.getGraphFieldContainers(newRelease, ContainerType.DRAFT)).as("draft containers for new release").isEmpty();
		}
	}

	@Test
	public void testDeletePublishedForRelease() throws Exception {
		try (Tx tx = tx()) {
			// 1. get the node
			Node node = content("concorde");
			String uuid = node.getUuid();

			// 2. publish the node
			SearchQueueBatch batch = createBatch();
			node.publish(mockActionContext(), batch);

			// 3. create new release
			Project project = project();
			Release initialRelease = project.getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("newrelease", user());

			// 4. migrate nodes
			meshDagger().releaseMigrationHandler().migrateRelease(newRelease, null);
			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft().setRelease(initialRelease.getUuid())));
			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft().setRelease(newRelease.getUuid())));

			// 5. delete node in new release
			call(() -> client().deleteNode(PROJECT_NAME, uuid, new VersioningParametersImpl().setRelease(newRelease.getUuid())));

			// 6. assert
			assertElement(meshRoot().getNodeRoot(), uuid, true);
			assertThat(node.getGraphFieldContainers(initialRelease, ContainerType.DRAFT)).as("draft containers for initial release").isNotEmpty();
			assertThat(node.getGraphFieldContainers(initialRelease, ContainerType.PUBLISHED)).as("published containers for initial release")
				.isNotEmpty();
			assertThat(node.getGraphFieldContainers(newRelease, ContainerType.DRAFT)).as("draft containers for new release").isEmpty();
			assertThat(node.getGraphFieldContainers(newRelease, ContainerType.PUBLISHED)).as("published containers for new release").isEmpty();
		}
	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		String uuid;
		try (Tx tx = tx()) {
			Node node = folder("2015");
			uuid = node.getUuid();
			role().revokePermissions(node, DELETE_PERM);
			tx.success();
		}

		call(() -> client().deleteNode(PROJECT_NAME, uuid), FORBIDDEN, "error_missing_perm", uuid);
		try (Tx tx = tx()) {
			assertNotNull(meshRoot().getNodeRoot().findByUuid(uuid));
		}
	}

	@Test
	public void testConflictByUpdateAdditionalLanguage() {
		try (Tx tx = tx()) {
			Node node = folder("2015");
			String nodeUuid = node.getUuid();

			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("de");
			update.getFields().put("slug", FieldUtil.createStringField("2015"));
			call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update), CONFLICT, "node_conflicting_segmentfield_update", "slug", "2015");
			// TODO also assert message properties
		}
	}

}
