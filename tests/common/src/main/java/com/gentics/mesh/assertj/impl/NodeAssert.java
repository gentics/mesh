package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.data.CreatorTrackingVertex;
import com.gentics.mesh.core.data.EditorTrackingVertex;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.result.Result;

/**
 * Assert for Node
 */
public class NodeAssert extends AbstractAssert<NodeAssert, HibNode> {
	public NodeAssert(HibNode actual) {
		super(actual, NodeAssert.class);
	}

	/**
	 * Assert that the node uses the given schema.
	 * 
	 * @param schemaContainer
	 *            schema container
	 * @return fluent API
	 */
	public NodeAssert isOf(HibSchema schemaContainer) {
		assertThat(actual.getSchemaContainer()).as(descriptionText() + " Schema").isEqualTo(schemaContainer);
		return this;
	}

	/**
	 * Assert that the node has a translation in the given language.
	 * 
	 * @param languageTag
	 *            language tag
	 * @return fluent API
	 */
	public NodeAssert hasTranslation(String languageTag) {
		NodeDaoWrapper nodeDao = Tx.get().nodeDao();
		assertThat(nodeDao.getAvailableLanguageNames(actual)).as(descriptionText() + " languages").contains(languageTag);
		return this;
	}

	/**
	 * Assert that the node does not have a translation in the given language.
	 * 
	 * @param languageTag
	 *            language tag
	 * @return fluent API
	 */
	public NodeAssert doesNotHaveTranslation(String languageTag) {
		NodeDaoWrapper nodeDao = Tx.get().nodeDao();
		assertThat(nodeDao.getAvailableLanguageNames(actual)).as(descriptionText() + " languages").doesNotContain(languageTag);
		return this;
	}

	/**
	 * Assert that the node has the given nodes as children in the branch.
	 * 
	 * @param branch
	 *            branch
	 * @param nodes
	 *            list of nodes
	 * @return fluent API
	 */
	public NodeAssert hasChildren(HibBranch branch, HibNode... nodes) {
		NodeDaoWrapper nodeDao = Tx.get().nodeDao();
		Stream<? extends HibNode> stream = StreamSupport.stream(nodeDao.getChildren(actual, branch.getUuid()).spliterator(), false);
		List<HibNode> list = stream.collect(Collectors.toList());
		assertThat(list).as(descriptionText() + " children").usingElementComparatorOnFields("uuid").contains(nodes);
		return this;
	}

	/**
	 * Assert that the node has no children in the branch.
	 * 
	 * @param branch
	 *            branch
	 * @return fluent API
	 */
	public NodeAssert hasNoChildren(HibBranch branch) {
		NodeDaoWrapper nodeDao = Tx.get().nodeDao();
		Stream<? extends HibNode> stream = StreamSupport.stream(nodeDao.getChildren(actual, branch.getUuid()).spliterator(), false);
		List<HibNode> list = stream.collect(Collectors.toList());
		assertThat(list).as(descriptionText() + " children").hasSize(0);
		return this;
	}

	/**
	 * Assert that the node has only the given nodes as children in the branch.
	 * 
	 * @param branch
	 *            branch
	 * @param nodes
	 *            list of nodes
	 * @return fluent API
	 */
	public NodeAssert hasOnlyChildren(HibBranch branch, HibNode... nodes) {
		NodeDaoWrapper nodeDao = Tx.get().nodeDao();
		Stream<? extends HibNode> stream = StreamSupport.stream(nodeDao.getChildren(actual, branch.getUuid()).spliterator(), false);
		List<HibNode> list = stream.collect(Collectors.toList());
		assertThat(list).as(descriptionText() + " children").usingElementComparatorOnFields("uuid").containsOnly(nodes);
		return this;
	}

	/**
	 * Assert that the node has none of the given nodes as children in the branch.
	 * 
	 * @param branch
	 *            branch
	 * @param nodes
	 *            list of nodes
	 * @return fluent API
	 */
	public NodeAssert hasNotChildren(HibBranch branch, HibNode... nodes) {
		NodeDaoWrapper nodeDao = Tx.get().nodeDao();
		Result<? extends HibNode> children = nodeDao.getChildren(actual, branch.getUuid());
		List<HibNode> childrenNodes = (List<HibNode>) children.list();
		assertThat(childrenNodes)
			.as(descriptionText() + " children")
			.usingElementComparatorOnFields("uuid")
			.doesNotContain(nodes);
		return this;
	}

	public NodeAssert matches(NodeCreateRequest request) {
		assertNotNull(request);
		assertNotNull(actual);

		// for (Entry<String, String> entry : request.getProperties().entrySet()) {
		// // Language language = languageService.findByLanguageTag(languageTag);
		// String propValue = node.getI18nProperties(language).getProperty(entry.getKey());
		// assertEquals("The property {" + entry.getKey() + "} did not match with the response object property", entry.getValue(), propValue);
		// }

		assertNotNull(actual.getUuid());
		assertNotNull(actual.getCreator());
		return this;
	}

	public NodeAssert matches(NodeResponse restNode) {
		assertGenericNode(toGraph(actual), restNode);
		HibSchema schema = actual.getSchemaContainer();
		assertNotNull("The schema of the test object should not be null. No further assertion can be verified.", schema);
		assertEquals(schema.getName(), restNode.getSchema().getName());
		assertEquals(schema.getUuid(), restNode.getSchema().getUuid());
		assertNotNull(restNode.getParentNode().getUuid());
		// TODO match fields
		return this;
	}

	public void assertGenericNode(MeshCoreVertex<?> node, AbstractGenericRestResponse model) {
		assertNotNull(node);
		assertNotNull(model);
		assertNotNull("UUID field was not set in the rest response.", model.getUuid());
		assertEquals("The uuids should not be different", node.getUuid(), model.getUuid());
		assertNotNull("Permissions field was not set in the rest response.", model.getPermissions());
		if (node instanceof EditorTrackingVertex) {
			assertNotNull("Editor field was not set in the rest response.", model.getEditor());
			EditorTrackingVertex editedNode = (EditorTrackingVertex) node;
			assertNotNull("The editor of the graph node was not set.", editedNode.getEditor());
			assertEquals(editedNode.getEditor().getFirstname(), model.getEditor());
			assertEquals(editedNode.getEditor().getLastname(), model.getEditor().getLastName());
			assertEquals(editedNode.getEditor().getUuid(), model.getEditor().getUuid());
		}
		if (node instanceof CreatorTrackingVertex && ((CreatorTrackingVertex) node).getCreator() != null) {
			assertNotNull("Creator field was not set in the rest response.", model.getCreator());
			CreatorTrackingVertex createdNode = (CreatorTrackingVertex) node;
			assertEquals(createdNode.getCreator().getFirstname(), model.getCreator().getFirstName());
			assertEquals(createdNode.getCreator().getLastname(), model.getCreator().getLastName());
			assertEquals(createdNode.getCreator().getUuid(), model.getCreator().getUuid());
		}
	}
}
