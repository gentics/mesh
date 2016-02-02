package com.gentics.mesh.core.data.schema.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModelImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.test.AbstractEmptyDBTest;
import com.gentics.mesh.util.FieldUtil;

public class SchemaComparatorTest extends AbstractEmptyDBTest {

	@Autowired
	private SchemaComparator comparator;

	@Test
	public void testEmptySchema() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		List<SchemaChangeModelImpl> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).isEmpty();
	}

	@Test
	public void testSegmentFieldAdded() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaB.setSegmentField("someSegement");
		List<SchemaChangeModelImpl> changes = comparator.diff(schemaA, schemaB);
		assertEquals(SchemaChangeOperation.UPDATESCHEMA, changes.get(0).getOperation());
	}

	@Test
	public void testSegmentFieldRemoved() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.setSegmentField("someSegement");
		List<SchemaChangeModelImpl> changes = comparator.diff(schemaA, schemaB);
		assertEquals(SchemaChangeOperation.UPDATESCHEMA, changes.get(0).getOperation());
	}

	@Test
	public void testSegmentFieldSame() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.setSegmentField("someSegement");
		schemaB.setSegmentField("someSegement");
		List<SchemaChangeModelImpl> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).isEmpty();
	}

	@Test
	public void testSegmentFieldUpdated() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.setSegmentField("test123");
		schemaB.setSegmentField("someSegement");
		List<SchemaChangeModelImpl> changes = comparator.diff(schemaA, schemaB);
		assertEquals(SchemaChangeOperation.UPDATESCHEMA, changes.get(0).getOperation());
	}

	@Test
	public void testDisplayFieldAdded() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaB.setDisplayField("someField");
		List<SchemaChangeModelImpl> changes = comparator.diff(schemaA, schemaB);
		assertEquals(SchemaChangeOperation.UPDATESCHEMA, changes.get(0).getOperation());
	}

	@Test
	public void testDisplayFieldRemoved() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.setDisplayField("someField");
		List<SchemaChangeModelImpl> changes = comparator.diff(schemaA, schemaB);
		assertEquals(SchemaChangeOperation.UPDATESCHEMA, changes.get(0).getOperation());
	}

	@Test
	public void testDisplayFieldUpdated() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.setDisplayField("someField");
		schemaB.setDisplayField("someField2");
		List<SchemaChangeModelImpl> changes = comparator.diff(schemaA, schemaB);
		assertEquals(SchemaChangeOperation.UPDATESCHEMA, changes.get(0).getOperation());
	}

	@Test
	public void testDisplayFieldSame() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.setDisplayField("someField");
		schemaB.setDisplayField("someField");
		List<SchemaChangeModelImpl> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).isEmpty();
	}

	@Test
	public void testContainerFlagUpdated() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.setContainer(true);
		schemaB.setContainer(false);
		List<SchemaChangeModelImpl> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
	}

	@Test
	public void testContainerFlagSame() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.setContainer(true);
		schemaB.setContainer(true);
		List<SchemaChangeModelImpl> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).isEmpty();

		schemaA.setContainer(false);
		schemaB.setContainer(false);
		changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).isEmpty();
	}

	@Test
	public void testStringFieldAdded() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.addField(FieldUtil.createStringFieldSchema("othertest"));
		schemaB.addField(FieldUtil.createStringFieldSchema("othertest"));
		schemaB.addField(FieldUtil.createStringFieldSchema("test"));
		List<SchemaChangeModelImpl> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertEquals(SchemaChangeOperation.ADDFIELD, changes.get(0).getOperation());
	}

	@Test
	public void testStringFieldRemoved() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.addField(FieldUtil.createStringFieldSchema("test"));
		List<SchemaChangeModelImpl> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertEquals(SchemaChangeOperation.REMOVEFIELD, changes.get(0).getOperation());
	}

	@Test
	public void testStringFieldRemoved2() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.addField(FieldUtil.createStringFieldSchema("test"));
		schemaA.addField(FieldUtil.createStringFieldSchema("test2"));
		schemaB.addField(FieldUtil.createStringFieldSchema("test2"));
		List<SchemaChangeModelImpl> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertEquals(SchemaChangeOperation.REMOVEFIELD, changes.get(0).getOperation());
	}

	@Test
	public void testStringFieldSame() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.addField(FieldUtil.createStringFieldSchema("test"));
		schemaB.addField(FieldUtil.createStringFieldSchema("test"));
		List<SchemaChangeModelImpl> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).isEmpty();
	}

	@Test
	public void testBinaryFieldPropertiesUpdated() {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.addField(FieldUtil.createBinaryFieldSchema("test").setAllowedMimeTypes("bla"));
		schemaB.addField(FieldUtil.createBinaryFieldSchema("test").setAllowedMimeTypes("blue"));
		List<SchemaChangeModelImpl> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).isNotEmpty();
		String[] list = (String[]) changes.get(0).getProperties().get("allowedMimeTypes");
		assertEquals("blue", list[0]);
	}

}
