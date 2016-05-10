package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainer;

/**
 * Assert for Node
 */
public class NodeAssert extends AbstractAssert<NodeAssert, Node> {
	public NodeAssert(Node actual) {
		super(actual, NodeAssert.class);
	}

	/**
	 * Assert that the node uses the given schema.
	 * 
	 * @param schemaContainer
	 *            schema container
	 * @return fluent API
	 */
	public NodeAssert isOf(SchemaContainer schemaContainer) {
		assertThat(actual.getSchemaContainer()).as(descriptionText() + " Schema").equals(schemaContainer);
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
		assertThat(actual.getAvailableLanguageNames()).as(descriptionText() + " languages").contains(languageTag);
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
		assertThat(actual.getAvailableLanguageNames()).as(descriptionText() + " languages").doesNotContain(languageTag);
		return this;
	}

	/**
	 * Assert that the node has the given nodes as children in the release
	 * 
	 * @param release release
	 * @param nodes list of nodes
	 * @return fluent API
	 */
	public NodeAssert hasChildren(Release release, Node... nodes) {
		assertThat(new ArrayList<Node>(actual.getChildren(release.getUuid()))).as(descriptionText() + " children")
				.usingElementComparatorOnFields("uuid").contains(nodes);
		return this;
	}

	/**
	 * Assert that the node has no children in the release
	 * 
	 * @param release release
	 * @return fluent API
	 */
	public NodeAssert hasNoChildren(Release release) {
		assertThat(new ArrayList<Node>(actual.getChildren(release.getUuid()))).as(descriptionText() + " children")
				.isEmpty();
		return this;
	}

	/**
	 * Assert that the node has only the given nodes as children in the release
	 * 
	 * @param release release
	 * @param nodes list of nodes
	 * @return fluent API
	 */
	public NodeAssert hasOnlyChildren(Release release, Node... nodes) {
		assertThat(new ArrayList<Node>(actual.getChildren(release.getUuid()))).as(descriptionText() + " children")
				.usingElementComparatorOnFields("uuid").containsOnly(nodes);
		return this;
	}

	/**
	 * Assert that the node has none of the given nodes as children in the release
	 * 
	 * @param release release
	 * @param nodes list of nodes
	 * @return fluent API
	 */
	public NodeAssert hasNotchildren(Release release, Node... nodes) {
		assertThat(new ArrayList<Node>(actual.getChildren(release.getUuid()))).as(descriptionText() + " children")
				.usingElementComparatorOnFields("uuid").doesNotContain(nodes);
		return this;
	}
}
