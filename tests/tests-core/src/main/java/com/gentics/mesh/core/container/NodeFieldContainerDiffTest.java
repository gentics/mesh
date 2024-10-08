package com.gentics.mesh.core.container;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.node.field.list.HibStringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.NoConsistencyCheck;

@MeshTestSetting(testSize = FULL, startServer = false)
@NoConsistencyCheck
public class NodeFieldContainerDiffTest extends AbstractFieldContainerDiffTest implements FieldDiffTestcases {

	@Test
	@Override
	public void testNoDiffByValue() {
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNodeFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue");
			HibNodeFieldContainer containerB = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerB.createString("dummy").setString("someValue");
			List<FieldContainerChange> list = contentDao.compareTo(containerA, containerB);
			assertNoDiff(list);
		}
	}

	@Test
	@Override
	public void testDiffByValue() {
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNodeFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue");
			HibNodeFieldContainer containerB = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerB.createString("dummy").setString("someValue2");
			List<FieldContainerChange> list = contentDao.compareTo(containerA, containerB);
			assertChanges(list, FieldChangeTypes.UPDATED);
		}
	}

	@Test
	public void testNoDiffStringFieldList() {
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNodeFieldContainer containerA = createContainer(FieldUtil.createListFieldSchema("dummy").setListType("string"));
			HibStringFieldList listA = containerA.createStringList("dummy");
			listA.addItem(listA.createString("test123"));

			HibNodeFieldContainer containerB = createContainer(FieldUtil.createListFieldSchema("dummy").setListType("string"));
			HibStringFieldList listB = containerB.createStringList("dummy");
			listB.addItem(listB.createString("test123"));

			List<FieldContainerChange> list = contentDao.compareTo(containerA, containerB);
			assertNoDiff(list);
		}
	}

	@Test
	public void testDiffStringFieldList() {
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNodeFieldContainer containerA = createContainer(FieldUtil.createListFieldSchema("dummy").setListType("string"));
			HibStringFieldList listA = containerA.createStringList("dummy");
			listA.addItem(listA.createString("test123"));

			HibNodeFieldContainer containerB = createContainer(FieldUtil.createListFieldSchema("dummy").setListType("string"));
			HibStringFieldList listB = containerB.createStringList("dummy");
			listB.addItem(listB.createString("test1234"));

			List<FieldContainerChange> list = contentDao.compareTo(containerA, containerB);
			assertThat(list).hasSize(1);
			assertChanges(list, FieldChangeTypes.UPDATED);
			FieldContainerChange change = list.get(0);
			assertEquals("dummy", change.getFieldKey());
		}
	}

	@Test
	public void testDiffMicronodeField() throws IOException {
		MicronodeFieldSchema micronodeField = FieldUtil.createMicronodeFieldSchema("micronodeField").setAllowedMicroSchemas("vcard");
		HibMicroschemaVersion version = tx(tx -> {
			// Create microschema vcard
			HibMicroschema schemaContainer = createMicroschema(tx);
			HibMicroschemaVersion version1 = createMicroschemaVersion(tx, schemaContainer, v -> {
				schemaContainer.setLatestVersion(v);
				MicroschemaVersionModel microschema = new MicroschemaModelImpl();
				microschema.setName("vcard");
				microschema.getFields().add(FieldUtil.createStringFieldSchema("firstName"));
				microschema.getFields().add(FieldUtil.createStringFieldSchema("lastName"));
				v.setSchema(microschema);
			});
			tx.commit();
			prepareTypedMicroschema(schemaContainer, List.of(micronodeField));
			return version1;
		});
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNodeFieldContainer containerA = createContainer(micronodeField);

			HibMicronodeField micronodeA = containerA.createMicronode("micronodeField", version);
			micronodeA.getMicronode().createString("firstName").setString("firstnameValue");
			micronodeA.getMicronode().createString("lastName").setString("lastnameValue");

			HibNodeFieldContainer containerB = createContainer(
				FieldUtil.createMicronodeFieldSchema("micronodeField").setAllowedMicroSchemas("vcard"));
			HibMicronodeField micronodeB = containerB.createMicronode("micronodeField", version);
			micronodeB.getMicronode().createString("firstName").setString("firstnameValue");
			micronodeB.getMicronode().createString("lastName").setString("lastnameValue-CHANGED");

			List<FieldContainerChange> list = contentDao.compareTo(containerA, containerB);
			assertThat(list).hasSize(1);
			FieldContainerChange change = list.get(0);
			assertEquals(FieldChangeTypes.UPDATED, change.getType());
			assertEquals("micronodeField", change.getFieldKey());
			assertEquals("micronodeField.lastName", change.getFieldCoordinates());
		}
	}

	@Test
	@Override
	public void testNoDiffByValuesNull() {
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNodeFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString(null);
			HibNodeFieldContainer containerB = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerB.createString("dummy").setString(null);
			List<FieldContainerChange> list = contentDao.compareTo(containerA, containerB);
			assertNoDiff(list);
		}
	}

	@Test
	@Override
	public void testDiffByValueNonNull() {
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNodeFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString(null);
			HibNodeFieldContainer containerB = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerB.createString("dummy").setString("someValue2");
			List<FieldContainerChange> list = contentDao.compareTo(containerA, containerB);
			assertChanges(list, FieldChangeTypes.UPDATED);
		}
	}

	@Test
	@Override
	public void testDiffByValueNonNull2() {
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNodeFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue2");
			HibNodeFieldContainer containerB = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerB.createString("dummy").setString(null);
			List<FieldContainerChange> list = contentDao.compareTo(containerA, containerB);
			assertChanges(list, FieldChangeTypes.UPDATED);
		}
	}

	@Test
	@Override
	public void testDiffBySchemaFieldRemoved() {
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNodeFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue");
			HibNodeFieldContainer containerB = createContainer();
			List<FieldContainerChange> list = contentDao.compareTo(containerA, containerB);
			assertChanges(list, FieldChangeTypes.REMOVED);
		}
	}

	@Test
	@Override
	public void testDiffBySchemaFieldAdded() {
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNodeFieldContainer containerA = createContainer();
			HibNodeFieldContainer containerB = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerB.createString("dummy").setString("someValue");
			List<FieldContainerChange> list = contentDao.compareTo(containerA, containerB);
			assertChanges(list, FieldChangeTypes.ADDED);
		}
	}

	@Test
	@Override
	public void testDiffBySchemaFieldTypeChanged() {
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNodeFieldContainer containerA = createContainer(FieldUtil.createStringFieldSchema("dummy"));
			containerA.createString("dummy").setString("someValue");
			HibNodeFieldContainer containerB = createContainer(FieldUtil.createHtmlFieldSchema("dummy"));
			containerB.createHTML("dummy").setHtml("someValue");
			List<FieldContainerChange> list = contentDao.compareTo(containerA, containerB);
			assertChanges(list, FieldChangeTypes.UPDATED);
		}
	}
}
