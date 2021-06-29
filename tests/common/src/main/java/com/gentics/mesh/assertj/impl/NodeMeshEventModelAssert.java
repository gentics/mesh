package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.gentics.mesh.assertj.AbstractMeshElementEventModelAssert;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;

public class NodeMeshEventModelAssert extends AbstractMeshElementEventModelAssert<NodeMeshEventModelAssert, NodeMeshEventModel> {

	public NodeMeshEventModelAssert(NodeMeshEventModel actual) {
		super(actual, NodeMeshEventModelAssert.class);
	}

	public NodeMeshEventModelAssert hasBranchUuid(String uuid) {
		assertEquals("The branch uuid of the event did not match.", uuid, actual.getBranchUuid());
		return this;
	}

	public NodeMeshEventModelAssert hasSchemaName(String name) {
		assertNotNull("The schema information was missing in the node event", actual.getSchema());
		assertEquals("The schema name in the event did not match.", name, actual.getSchema().getName());
		return this;
	}

	public NodeMeshEventModelAssert hasSchemaUuid(String uuid) {
		assertNotNull("The schema information was missing in the node event", actual.getSchema());
		assertEquals("The schema uuid in the event did not match.", uuid, actual.getSchema().getUuid());
		return this;
	}

	public NodeMeshEventModelAssert hasLanguage(String lang) {
		assertEquals("The language within the node event did not match.", lang, actual.getLanguageTag());
		return this;
	}

	public NodeMeshEventModelAssert hasType(ContainerType type) {
		assertEquals("The type within the node event did not match.", type, actual.getType());
		return this;
	}

	public NodeMeshEventModelAssert hasProject(String projectName, String projectUuid) {
		assertNotNull("The project information was missing in the node event", actual.getProject());
		assertEquals("Project name of the node event did not match.", projectName, actual.getProject().getName());
		assertEquals("Project uuid of the node event did not match.", projectUuid, actual.getProject().getUuid());
		return this;
	}

	public NodeMeshEventModelAssert hasSchema(String schemaName, String schemaUuid) {
		hasSchemaName(schemaName);
		hasSchemaUuid(schemaUuid);
		return this;
	}

}
