package com.gentics.mesh.core.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.FieldUtil;
import com.syncleus.ferma.FramedGraph;

public class NodeFieldContainerDiffTest extends AbstractFieldContainerDiffTest implements FieldDiffTestcases {

	@Test
	@Override
	public void testNoDiffByValue() {
		db.trx(() -> {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue");
			NodeGraphFieldContainer containerB = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerB.createString("dummy").setString("someValue");
			List<FieldContainerChange> list = containerA.compareTo(containerB);
			assertNoDiff(list);
			return null;
		});
	}

	@Test
	@Override
	public void testDiffByValue() {
		db.trx(() -> {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue");
			NodeGraphFieldContainer containerB = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerB.createString("dummy").setString("someValue2");
			List<FieldContainerChange> list = containerA.compareTo(containerB);
			assertChanges(list, FieldChangeTypes.UPDATED);
			return null;
		});
	}

	@Test
	public void testDiffMicronodeField() {
		db.trx(() -> {
			NodeGraphFieldContainer containerA = createContainer(
					FieldUtil.createMicronodeFieldSchema("micronodeField").setAllowedMicroSchemas("vcard"));

			// Create microschema vcard 

			FramedGraph graph = Database.getThreadLocalGraph();
			// 1. Setup schema
			MicroschemaContainer schemaContainer = graph.addFramedVertex(MicroschemaContainerImpl.class);
			MicroschemaContainerVersionImpl version = graph.addFramedVertex(MicroschemaContainerVersionImpl.class);
			schemaContainer.setLatestVersion(version);
			version.setSchemaContainer(schemaContainer);
			Microschema microschema = new MicroschemaModel();
			microschema.setName("vcard");
			microschema.getFields().add(FieldUtil.createStringFieldSchema("firstName"));
			microschema.getFields().add(FieldUtil.createStringFieldSchema("lastName"));
			version.setSchema(microschema);

			MicronodeGraphField micronodeA = containerA.createMicronode("micronodeField", version);
			micronodeA.getMicronode().createString("firstName").setString("firstnameValue");
			micronodeA.getMicronode().createString("lastName").setString("lastnameValue");

			NodeGraphFieldContainer containerB = createContainer(
					FieldUtil.createMicronodeFieldSchema("micronodeField").setAllowedMicroSchemas("vcard"));
			MicronodeGraphField micronodeB = containerB.createMicronode("micronodeField", version);
			micronodeB.getMicronode().createString("firstName").setString("firstnameValue");
			micronodeB.getMicronode().createString("lastName").setString("lastnameValue-CHANGED");

			List<FieldContainerChange> list = containerA.compareTo(containerB);
			assertThat(list).hasSize(1);
			FieldContainerChange change = list.get(0);
			assertEquals(FieldChangeTypes.UPDATED, change.getType());
			assertEquals("micronodeField.lastName", change.getFieldKey());
			return null;
		});
	}

	@Test
	@Override
	public void testNoDiffByValuesNull() {
		db.trx(() -> {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString(null);
			NodeGraphFieldContainer containerB = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerB.createString("dummy").setString(null);
			List<FieldContainerChange> list = containerA.compareTo(containerB);
			assertNoDiff(list);
			return null;
		});
	}

	@Test
	@Override
	public void testDiffByValueNonNull() {
		db.trx(() -> {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString(null);
			NodeGraphFieldContainer containerB = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerB.createString("dummy").setString("someValue2");
			List<FieldContainerChange> list = containerA.compareTo(containerB);
			assertChanges(list, FieldChangeTypes.UPDATED);
			return null;
		});
	}

	@Test
	@Override
	public void testDiffByValueNonNull2() {
		db.trx(() -> {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue2");
			NodeGraphFieldContainer containerB = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerB.createString("dummy").setString(null);
			List<FieldContainerChange> list = containerA.compareTo(containerB);
			assertChanges(list, FieldChangeTypes.UPDATED);
			return null;
		});
	}

	@Test
	@Override
	public void testDiffBySchemaFieldRemoved() {
		db.trx(() -> {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue");
			NodeGraphFieldContainer containerB = createContainer(null);
			List<FieldContainerChange> list = containerA.compareTo(containerB);
			assertChanges(list, FieldChangeTypes.REMOVED);
			return null;
		});
	}

	@Test
	@Override
	public void testDiffBySchemaFieldAdded() {
		db.trx(() -> {
			NodeGraphFieldContainer containerA = createContainer(null);
			NodeGraphFieldContainer containerB = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerB.createString("dummy").setString("someValue");
			List<FieldContainerChange> list = containerA.compareTo(containerB);
			assertChanges(list, FieldChangeTypes.ADDED);
			return null;
		});
	}

	@Test
	@Override
	public void testDiffBySchemaFieldTypeChanged() {
		db.trx(() -> {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue");
			NodeGraphFieldContainer containerB = createContainer(FieldUtil.createHtmlFieldSchema("dummy"));
			containerB.createHTML("dummy").setHtml("someValue");
			List<FieldContainerChange> list = containerA.compareTo(containerB);
			assertChanges(list, FieldChangeTypes.UPDATED);
			return null;
		});
	}
}
