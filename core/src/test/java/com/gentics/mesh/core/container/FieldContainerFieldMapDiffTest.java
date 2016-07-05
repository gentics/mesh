package com.gentics.mesh.core.container;

import java.util.List;

import org.junit.Ignore;
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
			dummyMap.put("dummy", null);
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
			// The field existed in both objects but the null value fieldmap update is still an update of the existing field.
			assertChanges(list, FieldChangeTypes.UPDATED);
		}
	}

	@Test
	@Override
	public void testDiffBySchemaFieldAdded() {
		try (NoTrx noTx = db.noTrx()) {
			// Create a container which does not contain a dummy field value 
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			FieldMap dummyMap = new FieldMapJsonImpl();
			dummyMap.put("dummy", FieldUtil.createStringField("someValue"));
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
			// The field exists in both but the change was an update 
			assertChanges(list, FieldChangeTypes.UPDATED);
		}
	}

	@Test
	@Override
	@Ignore("Not applicable")
	public void testDiffBySchemaFieldTypeChanged() {
		try (NoTrx noTx = db.noTrx()) {
			// Create container with string field value
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue");

			// Create fieldmap with html field value
			FieldMap dummyMap = new FieldMapJsonImpl();
			dummyMap.put("dummy", FieldUtil.createHtmlField("someValue"));

			// Compare both
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
			assertChanges(list, FieldChangeTypes.UPDATED);
		}
	}
}
