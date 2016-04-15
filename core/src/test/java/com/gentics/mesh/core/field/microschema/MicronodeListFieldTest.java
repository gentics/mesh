package com.gentics.mesh.core.field.microschema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;

public class MicronodeListFieldTest extends AbstractFieldTest {

	@Test
	@Override
	public void testFieldTransformation() throws Exception {

		Node node = folder("2015");
		prepareNode(node, "micronodeList", "micronode");

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		MicronodeGraphFieldList micronodeList = container.createMicronodeFieldList("micronodeList");
		micronodeList.addItem(null);
		//TODO add values
		fail("add values");
		micronodeList.addItem(null);

		//TODO Add micronode assertion
		NodeResponse response = transform(node);
		assertList(2, "micronodeList", "micronode", response);
		//		assertList(2, "micronodeList", MicronodeFieldListImpl.class, response);

	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		MicronodeGraphFieldList list = container.createMicronodeFieldList("dummyList");
		assertNotNull(list);
	}

	@Test
	@Override
	public void testClone() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		MicronodeGraphFieldList testField = container.createMicronodeFieldList("testField");

		Micronode micronode = testField.createMicronode(new MicronodeResponse());
		micronode.setMicroschemaContainerVersion(microschemaContainers().get("vcard").getLatestVersion());
		micronode.createString("firstName").setString("Donald");
		micronode.createString("lastName").setString("Duck");

		micronode = testField.createMicronode(new MicronodeResponse());
		micronode.setMicroschemaContainerVersion(microschemaContainers().get("vcard").getLatestVersion());
		micronode.createString("firstName").setString("Mickey");
		micronode.createString("lastName").setString("Mouse");

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		testField.cloneTo(otherContainer);

		assertThat(otherContainer.getMicronodeList("testField")).as("cloned field").isEqualToComparingFieldByField(testField);
	}

}
