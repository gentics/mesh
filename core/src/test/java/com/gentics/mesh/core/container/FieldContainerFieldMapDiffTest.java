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
import com.gentics.mesh.util.FieldUtil;

public class FieldContainerFieldMapDiffTest extends AbstractFieldContainerDiffTest implements FieldDiffTestcases {

	@Test
	@Override
	public void testNoDiffByValue() {
		db.trx(() -> {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue");
			FieldMap dummyMap = new FieldMapJsonImpl();
			dummyMap.put("dummy", FieldUtil.createStringField("someValue"));
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
			assertNoDiff(list);
			return null;
		});
	}

	@Override
	public void testDiffByValue() {
		db.trx(() -> {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue");
			FieldMap dummyMap = new FieldMapJsonImpl();
			dummyMap.put("dummy", FieldUtil.createStringField("someValue2"));
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
			assertChanges(list, FieldChangeTypes.UPDATED);
			return null;
		});
	}

	@Test
	@Override
	public void testNoDiffByValuesNull() {
		db.trx(() -> {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString(null);
			FieldMap dummyMap = new FieldMapJsonImpl();
			dummyMap.put("dummy", null);
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
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
			FieldMap dummyMap = new FieldMapJsonImpl();
			dummyMap.put("dummy", FieldUtil.createStringField("someValue2"));
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
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
			FieldMap dummyMap = new FieldMapJsonImpl();
			dummyMap.put("dummy", null);
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
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
			FieldMap dummyMap = new FieldMapJsonImpl();
			Schema schema = new SchemaModel();
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
			assertChanges(list, FieldChangeTypes.REMOVED);
			return null;
		});
	}

	@Test
	@Override
	public void testDiffBySchemaFieldAdded() {
		db.trx(() -> {
			NodeGraphFieldContainer containerA = createContainer(null);
			FieldMap dummyMap = new FieldMapJsonImpl();
			Schema schema = createSchema(FieldUtil.createStringFieldSchema("dummy"));
			dummyMap.put("dummy", FieldUtil.createStringField("someValue"));
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
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
			FieldMap dummyMap = new FieldMapJsonImpl();
			Schema schema = createSchema(FieldUtil.createHtmlFieldSchema("dummy"));
			dummyMap.put("dummy", FieldUtil.createHtmlField("someValue"));
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
			assertChanges(list, FieldChangeTypes.UPDATED);
			return null;
		});
	}
}
