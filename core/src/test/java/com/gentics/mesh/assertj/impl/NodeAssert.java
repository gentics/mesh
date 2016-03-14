package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;

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
	 * @param schemaVersion
	 *            schema container
	 * @return fluent API
	 */
	public NodeAssert isOf(SchemaContainerVersion schemaVersion) {
		assertThat(actual.getSchemaContainer()).as(descriptionText() + " Schema").equals(schemaVersion.getSchemaContainer());
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
}
