package com.gentics.mesh.linkrenderer;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.json.JsonObject;

@RunWith(value = Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true)
public class BranchLinkRendererTest extends AbstractMeshTest {

	private WebRootLinkReplacer replacer;

	private String branchUuid;

	@Parameter(0)
	public String hostname;

	@Parameter(1)
	public String pathPrefix;

	@Parameter(2)
	public boolean ssl;

	public String contentUuid;

	public String localNodeUuid;

	public String urlFieldNodeUuid;

	public String updatedUuid;

	public String branchName;

	private String latestBranchUuid;

	/**
	 * Get the test parameters
	 * 
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: hostname {0}, pathPrefix {1}, ssl {2}")
	public static Collection<Object[]> params() {
		Collection<Object[]> testData = new ArrayList<Object[]>();
		for (String hostname : Arrays.asList("www.nodea.com", "www.nodeb.com", null)) {
			for (String pathPrefix : Arrays.asList("", "/bla", "/bla/blubb", null)) {
				for (boolean ssl : Arrays.asList(true, false)) {
					testData.add(new Object[] { hostname, pathPrefix, ssl });
				}
			}
		}
		return testData;
	}

	@Before
	public void setupDeps() {
		latestBranchUuid = call(() -> client().findBranches(PROJECT_NAME)).getData().get(0).getUuid();

		// create schema, that contains a URL field (but not segment field)
		SchemaCreateRequest schemaCreateRequest = new SchemaCreateRequest()
				.setAutoPurge(true)
				.setContainer(false)
				.setName("schema_without_segment")
				.setFields(Arrays.asList(FieldUtil.createStringFieldSchema("url"), FieldUtil.createStringFieldSchema("name")))
				.setUrlFields(Arrays.asList("url"));
		SchemaResponse schemaResponse = call(() -> client().createSchema(schemaCreateRequest));

		// assign schema to project
		call(() -> client().assignSchemaToProject(PROJECT_NAME, schemaResponse.getUuid()));

		// Grant admin perms. Otherwise we can't check the jobs
		grantAdmin();

		contentUuid = tx(() -> content("news overview").getUuid());

		replacer = meshDagger().webRootLinkReplacer();

		branchName = String.format("%s_%b", hostname, ssl);

		BranchCreateRequest request = new BranchCreateRequest();
		request.setName(branchName);
		request.setHostname(hostname);
		request.setLatest(false);
		request.setSsl(ssl);
		request.setPathPrefix(pathPrefix);

		waitForJobs(() -> {
			BranchResponse response = call(() -> client().createBranch(PROJECT_NAME, request));
			assertThat(response).as("Branch Response").isNotNull().hasName(branchName).isActive().isNotMigrated();
			branchUuid = response.getUuid();
		}, COMPLETED, 1);

		// Create node which is only available in the newly created branch.
		String folderUuid = tx(() -> folder("news").getUuid());
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setParentNodeUuid(folderUuid);
		nodeCreateRequest.setSchemaName("content");
		FieldMap fields = new FieldMapImpl();
		fields.put("slug", FieldUtil.createStringField("localNode"));
		fields.put("content", FieldUtil.createStringField("Content2: {{mesh.link('" + contentUuid + "')}}"));
		fields.put("teaser", FieldUtil.createStringField("The Local Node teaser"));
		fields.put("title", FieldUtil.createStringField("The Local Node"));
		nodeCreateRequest.setFields(fields);
		localNodeUuid = call(() -> client().createNode(projectName(), nodeCreateRequest, new VersioningParametersImpl().setBranch(branchName)))
			.getUuid();

		updatedUuid = updateExistingNode();

		// create another node using the schema with the URL field
		urlFieldNodeUuid = call(() -> {
			NodeCreateRequest anotherNodeCreateRequest = new NodeCreateRequest()
				.setLanguage("en")
				.setParentNodeUuid(folderUuid)
				.setSchemaName("schema_without_segment");
			anotherNodeCreateRequest.getFields().put("url", FieldUtil.createStringField("/this/is/the/url.html"));
			anotherNodeCreateRequest.getFields().put("name", FieldUtil.createStringField("Node name"));
			return client().createNode(PROJECT_NAME, anotherNodeCreateRequest, new VersioningParametersImpl().setBranch(branchName));
		}).getUuid();
	}

	/**
	 * Update existing node in default branch only
	 * @return
	 */
	private String updateExistingNode() {
		NodeResponse node = call(() -> client().webroot(PROJECT_NAME, "/Deals")).getNodeResponse();
		NodeUpdateRequest updateRequest = node.toRequest();
		node.getFields().putString("slug", "changedDeals");
		call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), updateRequest));

		return node.getUuid();
	}

	@Test
	public void testResolve() {
		String replacedContent = replaceContent("{{mesh.link('" + contentUuid + "')}}");
		assertEquals("Check rendered content", getPrefix() + "/News/News%20Overview.en.html", replacedContent);
	}

	@Test
	public void testResolveWithBranchParameter() {
		String replacedContent;

		// New Branch:
		replacedContent = replaceContent(String.format("{{mesh.link('%s', 'en', '%s')}}", updatedUuid, branchUuid));
		assertEquals("Check rendered content", getPrefix() + "/Deals", replacedContent);
		replacedContent = replaceContent(String.format("{{mesh.link('%s', 'en', '%s')}}", updatedUuid, branchName));
		assertEquals("Check rendered content", getPrefix() + "/Deals", replacedContent);

		// Latest Branch:

		replacedContent = replaceContent(String.format("{{mesh.link('%s', 'en', '%s')}}", updatedUuid, latestBranchUuid));
		assertEquals("Check rendered content", "/changedDeals", replacedContent);
		replacedContent = replaceContent(String.format("{{mesh.link('%s', 'en', '%s')}}", updatedUuid, PROJECT_NAME));
		assertEquals("Check rendered content", "/changedDeals", replacedContent);

		// Invalid Branches (should fall back to latest):

		replacedContent = replaceContent(String.format("{{mesh.link('%s', 'en', '')}}", updatedUuid));
		assertEquals("Check rendered content", "/changedDeals", replacedContent);
		replacedContent = replaceContent(String.format("{{mesh.link('%s', 'en', 'abcdefghijklmnop')}}", updatedUuid));
		assertEquals("Check rendered content", "/changedDeals", replacedContent);
		replacedContent = replaceContent(String.format("{{mesh.link('%s', 'en', '72da59204f994856ab97520d3a571f6f')}}", updatedUuid));
		assertEquals("Check rendered content", "/changedDeals", replacedContent);
		replacedContent = replaceContent(String.format("{{mesh.link('%s', 'en', '%s')}}", updatedUuid, call(() -> client().me()).getUuid()));
		assertEquals("Check rendered content", "/changedDeals", replacedContent);
	}

	private String replaceContent(String content) {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			return replacer.replace(ac, branchUuid, ContainerType.DRAFT, content, LinkType.SHORT, null,
				null);
		}
	}

	@Test
	public void testResolveViaRest() {
		NodeParametersImpl nodeParams = new NodeParametersImpl();
		nodeParams.setResolveLinks(LinkType.SHORT);
		VersioningParametersImpl versionParams = new VersioningParametersImpl();
		versionParams.setBranch(branchUuid);
		NodeResponse response = call(() -> client().findNodeByUuid(projectName(), contentUuid, nodeParams, versionParams));
		String expectedPath = "/News/News%20Overview.en.html";
		String expectedPathWithPrefix = getPrefix() + expectedPath;
		assertEquals("The path did not match", expectedPathWithPrefix, response.getPath());

		// Test resolving the path using a local node to the created branch
		NodeResponse response2 = call(() -> client().findNodeByUuid(projectName(), localNodeUuid, nodeParams, versionParams));
		String expectedPath2 = getPrefix() + "/News/localNode";
		assertEquals("The path did not match", expectedPath2, response2.getPath());
		assertEquals("Content2: " + getPathPrefix() + expectedPath, response2.getFields().getStringField("content").getString());
	}

	@Test
	public void testRenderUrlField() {
		assertThat(replaceContent(String.format("{{mesh.link('%s', 'en', '%s')}}", urlFieldNodeUuid, branchUuid)))
				.as("URL").isEqualTo(getPrefix() + "/this/is/the/url.html");
	}

	@Test
	public void testRenderUrlFieldViaGraphQL() {
		GraphQLRequest request = new GraphQLRequest()
				.setQuery("query nodeByUuid($uuid:String) {\n" +
					"  node(uuid: $uuid) {\n" +
					"    path\n" +
					"  }\n" +
					"}")
				.setVariables(new JsonObject().put("uuid", urlFieldNodeUuid));
		GraphQLResponse graphQLResponse = call(() -> client().graphql(PROJECT_NAME, request, new VersioningParametersImpl().setBranch(branchName)));
		assertThat(graphQLResponse.getData()).compliesTo("$.node.path=" + getPathPrefix() + "/this/is/the/url.html");
	}

	private String getPrefix() {
		String prefix = "";
		if (!StringUtils.isEmpty(hostname)) {
			prefix = String.format("http%s://%s", ssl ? "s" : "", hostname);
		}
		prefix = prefix + getPathPrefix();
		return prefix;
	}

	private String getPathPrefix() {
		if (pathPrefix == null) {
			return "";
		} else {
			return pathPrefix;
		}
	}

}
