package com.gentics.mesh.database;

import static com.gentics.mesh.test.ClientHelper.call;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;

/**
 * Abstract base for query counting tests for Users
 */
public abstract class AbstractNodeQueryCountingTest extends AbstractCountingTest {
	public final static String PROJECT_NAME = "testproject";

	public final static int NUM_NODES = 53;

	public final static int NUM_TAGS_PER_NODE = 5;

	protected static int totalNumNodes = NUM_NODES;

	protected static Set<String> initialNodeUuids = new HashSet<>();

	@Before
	public void setup() {
		if (getTestContext().needsSetup()) {
			// create project
			ProjectCreateRequest projectCreateRequest = new ProjectCreateRequest();
			projectCreateRequest.setName(PROJECT_NAME);
			projectCreateRequest.setHostname(PROJECT_NAME);
			projectCreateRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
			ProjectResponse project = call(() -> client().createProject(projectCreateRequest));

			db().tx(tx -> {
				tx.nodeDao().findAllGlobal().map(HibNode::getUuid).forEach(initialNodeUuids::add);
			});
			totalNumNodes += initialNodeUuids.size();

			// create tag family
			TagFamilyResponse tagFamily = createTagFamily(PROJECT_NAME, "tagfamily");

			// create tags
			Set<String> tagUuids = new HashSet<>();
			for (int j = 0; j < NUM_TAGS_PER_NODE; j++) {
				TagResponse tag = createTag(PROJECT_NAME, tagFamily.getUuid(), "tag_%d".formatted(j));
				tagUuids.add(tag.getUuid());
			}

			// create nodes
			for (int i = 0; i < NUM_NODES; i++) {
				NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
				nodeCreateRequest.setParentNode(new NodeReference().setUuid(project.getRootNode().getUuid()));
				nodeCreateRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
				nodeCreateRequest.setLanguage("en");
				nodeCreateRequest.getFields().put("name", FieldUtil.createStringField("node_%d".formatted(i)));
				nodeCreateRequest.setPublish(true);
				NodeResponse nodeResponse = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest, new NodeParametersImpl().setLanguages("en")));

				for (String tagUuid : tagUuids) {
					call(() -> client().addTagToNode(PROJECT_NAME, nodeResponse.getUuid(), tagUuid));
				}
			}
		}

		// clear the cache
		adminCall(() -> client().clearCache());
	}

}
