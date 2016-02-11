package com.gentics.mesh.core.data.fieldhandler.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATESCHEMA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.test.AbstractDBTest;
import com.gentics.mesh.util.FieldUtil;

public class SchemaComparatorSchemaTest extends AbstractDBTest {

	@Autowired
	private SchemaComparator comparator;

	@Test
	public void testEmptySchema() throws IOException {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).isEmpty();
	}

	@Test
	public void testSchemaFieldReorder() throws IOException {
		Schema schemaA = new SchemaImpl();
		schemaA.addField(FieldUtil.createHtmlFieldSchema("first"));
		schemaA.addField(FieldUtil.createHtmlFieldSchema("second"));

		Schema schemaB = new SchemaImpl();
		schemaB.addField(FieldUtil.createHtmlFieldSchema("second"));
		schemaB.addField(FieldUtil.createHtmlFieldSchema("first"));
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATESCHEMA).hasProperty("order", new String[] { "second", "first" });
	}

	@Test
	public void testSchemaFieldNoReorder() throws IOException {
		Schema schemaA = new SchemaImpl();
		schemaA.addField(FieldUtil.createHtmlFieldSchema("first"));
		schemaA.addField(FieldUtil.createHtmlFieldSchema("second"));

		Schema schemaB = new SchemaImpl();
		schemaB.addField(FieldUtil.createHtmlFieldSchema("first"));
		schemaB.addField(FieldUtil.createHtmlFieldSchema("second"));
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).isEmpty();
	}

	@Test
	public void testSegmentFieldAdded() throws IOException {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaB.setSegmentField("someSegement");
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertEquals(SchemaChangeOperation.UPDATESCHEMA, changes.get(0).getOperation());
	}

	@Test
	public void testSegmentFieldRemoved() throws IOException {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.setSegmentField("someSegement");
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertEquals(SchemaChangeOperation.UPDATESCHEMA, changes.get(0).getOperation());
	}

	@Test
	public void testSegmentFieldSame() throws IOException {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.setSegmentField("someSegement");
		schemaB.setSegmentField("someSegement");
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).isEmpty();
	}

	@Test
	public void testSegmentFieldUpdated() throws IOException {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.setSegmentField("test123");
		schemaB.setSegmentField("someSegement");
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertEquals(SchemaChangeOperation.UPDATESCHEMA, changes.get(0).getOperation());
	}

	@Test
	public void testDisplayFieldAdded() throws IOException {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaB.setDisplayField("someField");
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertEquals(SchemaChangeOperation.UPDATESCHEMA, changes.get(0).getOperation());
	}

	@Test
	public void testDisplayFieldRemoved() throws IOException {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.setDisplayField("someField");
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertEquals(SchemaChangeOperation.UPDATESCHEMA, changes.get(0).getOperation());
	}

	@Test
	public void testDisplayFieldUpdated() throws IOException {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.setDisplayField("someField");
		schemaB.setDisplayField("someField2");
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertEquals(SchemaChangeOperation.UPDATESCHEMA, changes.get(0).getOperation());
	}

	@Test
	public void testDisplayFieldSame() throws IOException {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.setDisplayField("someField");
		schemaB.setDisplayField("someField");
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).isEmpty();
	}

	@Test
	public void testContainerFlagUpdated() throws IOException {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.setContainer(true);
		schemaB.setContainer(false);
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
	}

	@Test
	public void testContainerFlagSame() throws IOException {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.setContainer(true);
		schemaB.setContainer(true);
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).isEmpty();

		schemaA.setContainer(false);
		schemaB.setContainer(false);
		changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).isEmpty();
	}

	@Test
	public void testSameDescription() throws IOException {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.setDescription("test123");
		schemaB.setDescription("test123");
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).isEmpty();
	}

	@Test
	public void testDescriptionUpdated() throws IOException {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.setDescription("test123");
		schemaB.setDescription("test123-changed");
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATESCHEMA).hasProperty("description", "test123-changed");
	}

	@Test
	public void testDescriptionUpdatedToNull() throws IOException {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.setDescription("test123");
		schemaB.setDescription(null);
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATESCHEMA).hasProperty("description", null);
	}

	@Test
	public void testSameName() throws IOException {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.setName("test123");
		schemaB.setName("test123");
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).isEmpty();
	}

	@Test
	public void testNameUpdated() throws IOException {
		Schema schemaA = new SchemaImpl();
		Schema schemaB = new SchemaImpl();
		schemaA.setName("test123");
		schemaB.setName("test123-changed");
		List<SchemaChangeModel> changes = comparator.diff(schemaA, schemaB);
		assertThat(changes).hasSize(1);
		assertThat(changes.get(0)).is(UPDATESCHEMA).hasProperty("name", "test123-changed");
	}

}
