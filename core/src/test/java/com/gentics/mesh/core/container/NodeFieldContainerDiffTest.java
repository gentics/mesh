package com.gentics.mesh.core.container;

import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.util.FieldUtil;

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
