package com.gentics.mesh.database;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.hibernate.util.QueryCounter;
import com.gentics.mesh.parameter.client.GenericParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.ResetTestDb;
import com.gentics.mesh.test.TestSize;

/**
 * Test cases which count the number of executed REST call queries.
 */
@MeshTestSetting(testSize = TestSize.PROJECT, monitoring = true, startServer = true, customOptionChanger = QueryCounter.EnableHibernateStatistics.class, resetBetweenTests = ResetTestDb.NEVER)
@RunWith(Parameterized.class)
public class QueryCountingTest extends AbstractCountingTest {

	/**
	 * Number of nodes to create
	 */
	public final static int NUM_NODES = 100;
	/**
	 * Number of tags to create (and assign to the nodes)
	 */
	public final static int NUM_TAGS = 100;

	/**
	 * Number of allowed queries when getting nodes with etag
	 */
	public final static int ALLOWED_WITH_ETAG = 20;

	/**
	 * Number of allowed queries when getting nodes without etag
	 */
	public final static int ALLOWED_NO_ETAG = 15;

	protected static TagFamilyResponse tagFamily;

	@Parameters(name = "{index}: field {0}, etag {1}")
	public static Collection<Object[]> parameters() throws Exception {
		Collection<Object[]> data = new ArrayList<>();
		for (String field : Arrays.asList("uuid", "languages", "parent", "perms", "children", "tags", "breadcrumb", "path", "project", "editor", "edited", "creator", "created")) {
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
			String projectName = tx(() -> projectName());
			String parentNodeUuid = tx(() -> folder("2015").getUuid());

			// create a tag family and some tags
			tagFamily = createTagFamily(projectName, "testtags");
			List<String> tagUuids = new ArrayList<>();
			IntStream.range(0, NUM_TAGS).forEach(i -> {
				tagUuids.add(createTag(projectName, tagFamily.getUuid(), "tag-" + i).getUuid());
			});
			List<TagReference> tagReferences = tagUuids.stream().map(tagUuid -> new TagReference().setTagFamily(tagFamily.getName()).setUuid(tagUuid)).collect(Collectors.toList());

			// create some additional nodes
			IntStream.range(0, NUM_NODES).forEach(i -> {
				NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
				nodeCreateRequest.setParentNode(new NodeReference().setUuid(parentNodeUuid));
				nodeCreateRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
				nodeCreateRequest.setLanguage("en");
				nodeCreateRequest.setTags(tagReferences);
				nodeCreateRequest.getFields().put("slug", new StringFieldImpl().setString(RandomStringUtils.randomAlphabetic(5)));
				call(() -> client().createNode(projectName, nodeCreateRequest, new NodeParametersImpl().setLanguages("en")));
			});
		}

		// clear the cache
		adminCall(() -> client().clearCache());
	}

	/**
	 * Test getting all nodes of a project
	 */
	@Test
	public void testGetAllNodes() {
		doTest(() -> client().findNodes(projectName(), new GenericParametersImpl().setETag(etag).setFields(field)), countNodesNoMoreThan());
	}

	/**
	 * Test getting children of a node
	 */
	@Test
	public void testGetChildren() {
		String parentNodeUuid = tx(() -> folder("2015").getUuid());
		NodeListResponse result = doTest(() -> client().findNodeChildren(projectName(), parentNodeUuid, new GenericParametersImpl().setETag(etag).setFields(field)), countNodesNoMoreThan());
		assertThat(result.getData()).hasSize(NUM_NODES);
	}

	/**
	 * Test getting nodes, which have been tagged with a specific tag
	 */
	@Test
	public void testGetTaggedNodes() {
		String tagName = "tag-" + RandomUtils.nextInt(0, NUM_TAGS);
		HibTagFamily family = tx(() -> Tx.get().tagFamilyDao().findByName(tagFamily.getName()));
		String tagUuid = tx(() -> Tx.get().tagDao().findByName(family, tagName)).getUuid();

		NodeListResponse result = doTest(() -> client().findNodesForTag(projectName(), tagFamily.getUuid(), tagUuid, new GenericParametersImpl().setETag(etag).setFields(field)), countNodesNoMoreThan());
		assertThat(result.getData()).hasSize(NUM_NODES);
	}

	private int countNodesNoMoreThan() {
		return etag ? ALLOWED_WITH_ETAG : ALLOWED_NO_ETAG;
	}
}
