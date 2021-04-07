package com.gentics.mesh.core.container;

import static com.gentics.mesh.test.TestSize.FULL;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.test.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = false)
public class FieldContainerFieldMapDiffTest extends AbstractFieldContainerDiffTest implements FieldDiffTestcases {

	@Test
	@Override
	public void testNoDiffByValue() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue");
			FieldMap dummyMap = new FieldMapImpl();
			dummyMap.put("dummy", FieldUtil.createStringField("someValue"));
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
			assertNoDiff(list);
		}
	}

	@Override
	public void testDiffByValue() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue");
			FieldMap dummyMap = new FieldMapImpl();
			dummyMap.put("dummy", FieldUtil.createStringField("someValue2"));
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
			assertChanges(list, FieldChangeTypes.UPDATED);
		}
	}

	@Test
	@Override
	public void testNoDiffByValuesNull() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString(null);
			FieldMap dummyMap = new FieldMapImpl();
			dummyMap.put("dummy", null);
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
			assertNoDiff(list);
		}
	}

	@Test
	@Override
	public void testDiffByValueNonNull() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString(null);
			FieldMap dummyMap = new FieldMapImpl();
			dummyMap.put("dummy", FieldUtil.createStringField("someValue2"));
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
			assertChanges(list, FieldChangeTypes.UPDATED);
		}
	}

	@Test
	@Override
	public void testDiffByValueNonNull2() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue2");
			FieldMap dummyMap = new FieldMapImpl();
			dummyMap.put("dummy", null);
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
			assertChanges(list, FieldChangeTypes.UPDATED);
		}
	}

	@Test
	@Override
	public void testDiffBySchemaFieldRemoved() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue");
			FieldMap dummyMap = new FieldMapImpl();
			dummyMap.put("dummy", null);
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
			// The field existed in both objects but the null value fieldmap update is still an update of the existing field.
			assertChanges(list, FieldChangeTypes.UPDATED);
		}
	}

	@Test
	@Override
	public void testDiffBySchemaFieldAdded() {
		try (Tx tx = tx()) {
			// Create a container which does not contain a dummy field value 
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			FieldMap dummyMap = new FieldMapImpl();
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
		try (Tx tx = tx()) {
			// Create container with string field value
			NodeGraphFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue");

			// Create fieldmap with html field value
			FieldMap dummyMap = new FieldMapImpl();
			dummyMap.put("dummy", FieldUtil.createHtmlField("someValue"));

			// Compare both
			List<FieldContainerChange> list = containerA.compareTo(dummyMap);
			assertChanges(list, FieldChangeTypes.UPDATED);
		}
	}
}
