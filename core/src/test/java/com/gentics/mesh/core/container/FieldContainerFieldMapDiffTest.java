package com.gentics.mesh.core.container;

import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapJsonImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.util.FieldUtil;

public class FieldContainerFieldMapDiffTest extends AbstractFieldContainerDiffTest implements FieldDiffTestcases {

	@Test
	@Override
	public void testNoDiffByValue() {
		try (NoTrx noTx = db.noTrx()) {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue");
			FieldMap dummyMap = new FieldMapJsonImpl();
			dummyMap.put("dummy", FieldUtil.createStringField("someValue"));
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
			assertNoDiff(list);
		}
	}

	@Override
	public void testDiffByValue() {
		try (NoTrx noTx = db.noTrx()) {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue");
			FieldMap dummyMap = new FieldMapJsonImpl();
			dummyMap.put("dummy", FieldUtil.createStringField("someValue2"));
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
			assertChanges(list, FieldChangeTypes.UPDATED);
		}
	}

	@Test
	@Override
	public void testNoDiffByValuesNull() {
		try (NoTrx noTx = db.noTrx()) {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString(null);
			FieldMap dummyMap = new FieldMapJsonImpl();
			dummyMap.put("dummy", null);
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
			assertNoDiff(list);
		}
	}

	@Test
	@Override
	public void testDiffByValueNonNull() {
		try (NoTrx noTx = db.noTrx()) {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString(null);
			FieldMap dummyMap = new FieldMapJsonImpl();
			dummyMap.put("dummy", FieldUtil.createStringField("someValue2"));
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
			assertChanges(list, FieldChangeTypes.UPDATED);
		}
	}

	@Test
	@Override
	public void testDiffByValueNonNull2() {
		try (NoTrx noTx = db.noTrx()) {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue2");
			FieldMap dummyMap = new FieldMapJsonImpl();
			dummyMap.put("dummy", null);
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
			assertChanges(list, FieldChangeTypes.UPDATED);
		}
	}

	@Test
	@Override
	public void testDiffBySchemaFieldRemoved() {
		try (NoTrx noTx = db.noTrx()) {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue");
			FieldMap dummyMap = new FieldMapJsonImpl();
			Schema schema = new SchemaModel();
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
			assertChanges(list, FieldChangeTypes.REMOVED);
		}
	}

	@Test
	@Override
	public void testDiffBySchemaFieldAdded() {
		try (NoTrx noTx = db.noTrx()) {
			NodeGraphFieldContainer containerA = createContainer(null);
			FieldMap dummyMap = new FieldMapJsonImpl();
			Schema schema = createSchema(FieldUtil.createStringFieldSchema("dummy"));
			dummyMap.put("dummy", FieldUtil.createStringField("someValue"));
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
			assertChanges(list, FieldChangeTypes.ADDED);
		}
	}

	@Test
	@Override
	public void testDiffBySchemaFieldTypeChanged() {
		try (NoTrx noTx = db.noTrx()) {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue");
			FieldMap dummyMap = new FieldMapJsonImpl();
			Schema schema = createSchema(FieldUtil.createHtmlFieldSchema("dummy"));
			dummyMap.put("dummy", FieldUtil.createHtmlField("someValue"));
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
			assertChanges(list, FieldChangeTypes.UPDATED);
		}
	}
}
