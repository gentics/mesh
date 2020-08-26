package com.gentics.mesh.assertj.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.tag.TagReference;

public class NodeResponseAssert extends AbstractAssert<NodeResponseAssert, NodeResponse> {

	public NodeResponseAssert(NodeResponse actual) {
		super(actual, NodeResponseAssert.class);
	}

	/**
	 * Checks whether the given tag is listed within the node rest response.
	 * 
	 * @param restNode
	 * @param tag
	 * @return
	 */
	public boolean contains(HibTag tag) {
		assertNotNull(tag);
		assertNotNull(tag.getUuid());
		assertNotNull(actual);
		assertNotEquals("There were not tags listed in the restNode.", 0, actual.getTags().size());
		if (actual.getTags() == null) {
			return false;
		}

		for (TagReference tagRef : actual.getTags()) {
			if (tag.getUuid().equals(tagRef.getUuid())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Assert that the node response contains a version reference with the given number
	 *
	 * @param number
	 * @return Fluent API
	 */
	public NodeResponseAssert hasVersion(String number) {
		assertThat(actual.getVersion()).as(descriptionText() + " version").isNotNull();
		assertThat(actual.getVersion()).as(descriptionText() + " version number").isEqualTo(number);
		return this;
	}

	/**
	 * Assert that the node response contains the given string field
	 * 
	 * @param name
	 *            field name
	 * @param value
	 *            field value
	 * @return Fluent API
	 */
	public NodeResponseAssert hasStringField(String name, String value) {
		assertThat(actual.getFields().getStringField(name)).as(descriptionText() + " string field").isNotNull();
		assertThat(actual.getFields().getStringField(name).getString()).as(descriptionText() + " string field value").isEqualTo(value);
		return this;
	}

	/**
	 * Assert that the node response has the given language
	 * 
	 * @param languageTag
	 * @return Fluent API
	 */
	public NodeResponseAssert hasLanguage(String languageTag) {
		assertThat(actual.getLanguage()).as(descriptionText() + " language").isEqualTo(languageTag);
		return this;
	}

	/**
	 * Assert that the response contains the given node
	 *
	 * @param node
	 *            node
	 * @return Fluent API
	 */
	public NodeResponseAssert is(HibNode node) {
		assertThat(actual.getUuid()).as("Uuid").isEqualTo(node.getUuid());
		return this;
	}

	/**
	 * Assert that the response has the given uuid.
	 * 
	 * @param uuid
	 *            uuid
	 * @return Fluent API
	 */
	public NodeResponseAssert hasUuid(String uuid) {
		assertThat(actual.getUuid()).as("Uuid").isEqualTo(uuid);
		return this;
	}

	/**
	 * Assert that the create request matches up with the actual response.
	 * 
	 * @param request
	 * @return Fluent API
	 */
	public NodeResponseAssert matches(NodeCreateRequest request) {

		// for (Map.Entry<String, String> entry : request.getProperties().entrySet()) {
		// String value = request.getParentNodeUuid();
		// assertEquals("The property {" + entry.getKey() + "} did not match with the response object property", entry.getValue(),
		// restNode.getProperty(entry.getKey()));
		//
		// }
		assertNotNull(actual);
		assertNotNull(request);
		String schemaName = request.getSchema().getName();
		assertEquals("The schemaname of the request does not match the response schema name", schemaName, actual.getSchema().getName());
		// assertEquals(request.getOrder(), restNode.getOrder());
		// String nodeUuid = request.getParentNodeUuid();
		// TODO how to match the parent node?

		assertNotNull(actual.getUuid());
		assertNotNull(actual.getCreator());
		assertNotNull(actual.getPermissions());

		return this;

	}

	/**
	 * Assert that the given schema version matches up with the actual schema version of the response.
	 * 
	 * @param name
	 *            Expected schema name
	 * @param version
	 *            Expected schema version
	 * @return Fluent API
	 */
	public NodeResponseAssert hasSchemaVersion(String name, String version) {
		assertEquals("The schema version did not match.", version, actual.getSchema().getVersion());
		assertEquals("The schema name did not match.", name, actual.getSchema().getName());
		return this;
	}
}
