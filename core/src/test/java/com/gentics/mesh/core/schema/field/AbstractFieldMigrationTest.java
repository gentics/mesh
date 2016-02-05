package com.gentics.mesh.core.schema.field;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.handler.NodeMigrationHandler;
import com.gentics.mesh.core.data.schema.RemoveFieldChange;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.UpdateFieldChange;
import com.gentics.mesh.core.data.schema.impl.RemoveFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateFieldChangeImpl;
import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.FieldUtil;

/**
 * Base class for all field migration tests
 */
public abstract class AbstractFieldMigrationTest extends AbstractBasicDBTest implements FieldMigrationTest {
	protected final static FieldSchemaCreator CREATEBINARY = name -> FieldUtil.createBinaryFieldSchema(name);
	protected final static FieldSchemaCreator CREATEBOOLEAN = name -> FieldUtil.createBooleanFieldSchema(name);
	protected final static FieldSchemaCreator CREATEBOOLEANLIST = name -> FieldUtil.createListFieldSchema(name, "boolean");
	protected final static FieldSchemaCreator CREATEDATE = name -> FieldUtil.createDateFieldSchema(name);
	protected final static FieldSchemaCreator CREATEDATELIST = name -> FieldUtil.createListFieldSchema(name, "date");
	protected final static FieldSchemaCreator CREATEHTML = name -> FieldUtil.createHtmlFieldSchema(name);
	protected final static FieldSchemaCreator CREATEHTMLLIST = name -> FieldUtil.createListFieldSchema(name, "html");
	protected final static FieldSchemaCreator CREATEMICRONODE = name -> FieldUtil.createMicronodeFieldSchema(name);
	protected final static FieldSchemaCreator CREATEMICRONODELIST = name -> FieldUtil.createListFieldSchema(name, "micronode");
	protected final static FieldSchemaCreator CREATENODE = name -> FieldUtil.createNodeFieldSchema(name);
	protected final static FieldSchemaCreator CREATENODELIST = name -> FieldUtil.createListFieldSchema(name, "node");
	protected final static FieldSchemaCreator CREATENUMBER = name -> FieldUtil.createNumberFieldSchema(name);
	protected final static FieldSchemaCreator CREATENUMBERLIST = name -> FieldUtil.createListFieldSchema(name, "number");
	protected final static FieldSchemaCreator CREATESTRING = name -> FieldUtil.createStringFieldSchema(name);
	protected final static FieldSchemaCreator CREATESTRINGLIST = name -> FieldUtil.createListFieldSchema(name, "string");

	@Autowired
	protected NodeMigrationHandler nodeMigrationHandler;

	/**
	 * Generic method to test node migration where a field has been removed
	 * @param creator creator implementation
	 * @param dataProvider data provider implementation
	 * @param fetcher field fetcher implementation
	 */
	protected void removeField(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher) throws IOException {
		String removedFieldName = "toremove";
		String persistentFieldName = "persistent";

		// create version 1 of the schema
		SchemaContainer containerA = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		Schema schemaA = new SchemaImpl();
		schemaA.setName("migratedSchema");
		schemaA.setVersion(1);
		schemaA.addField(creator.create(persistentFieldName));
		schemaA.addField(creator.create(removedFieldName));
		schemaA.setDisplayField("name");
		schemaA.setSegmentField("name");
		containerA.setName("migratedSchema");
		containerA.setSchema(schemaA);

		// create version 2 of the schema (with one field removed)
		SchemaContainer containerB = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		Schema schemaB = new SchemaImpl();
		schemaB.setName("migratedSchema");
		schemaB.setVersion(2);
		schemaB.addField(creator.create(persistentFieldName));
		schemaB.setDisplayField("name");
		schemaB.setSegmentField("name");
		containerB.setName("migratedSchema");
		containerB.setSchema(schemaB);

		// link the schemas with the change in between
		RemoveFieldChange change = Database.getThreadLocalGraph().addFramedVertex(RemoveFieldChangeImpl.class);
		change.setFieldName(removedFieldName);
		change.setPreviousSchemaContainer(containerA);
		change.setNextSchemaContainer(containerB);
		containerA.setNextVersion(containerB);

		// create a node based on the old schema
		User user = user();
		Language english = english();
		Node parentNode = folder("2015");
		Node node = parentNode.create(user, containerA, project());
		NodeGraphFieldContainer englishContainer = node.getOrCreateGraphFieldContainer(english);
		dataProvider.set(englishContainer, persistentFieldName);
		dataProvider.set(englishContainer, removedFieldName);

		// migrate the node
		nodeMigrationHandler.migrateNodes(containerA);
		node.reload();
		node.getGraphFieldContainer("en").reload();

		// assert that migration worked
		assertThat(node).as("Migrated Node").isOf(containerB).hasTranslation("en");
		assertThat(fetcher.fetch(node.getGraphFieldContainer("en"), persistentFieldName))
				.as("Field '" + persistentFieldName + "'").isNotNull();
		assertThat(fetcher.fetch(node.getGraphFieldContainer("en"), removedFieldName))
				.as("Field '" + removedFieldName + "'").isNull();
	}

	/**
	 * Generic method to test node migration where the type of a field is changed
	 * @param oldField creator for the old field
	 * @param dataProvider data provider for the old field
	 * @param newField creator for the new field
	 * @param asserter
	 */
	protected void changeType(FieldSchemaCreator oldField, DataProvider dataProvider, FieldSchemaCreator newField,
			DataAsserter asserter) throws IOException {
		String fieldName = "changedfield";

		// create version 1 of the schema
		SchemaContainer containerA = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		Schema schemaA = new SchemaImpl();
		schemaA.setName("migratedSchema");
		schemaA.setVersion(1);
		schemaA.addField(oldField.create(fieldName));
		schemaA.setDisplayField("name");
		schemaA.setSegmentField("name");
		containerA.setName("migratedSchema");
		containerA.setSchema(schemaA);

		// create version 2 of the schema (with the field modified)
		SchemaContainer containerB = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		Schema schemaB = new SchemaImpl();
		schemaB.setName("migratedSchema");
		schemaB.setVersion(2);
		FieldSchema newFieldSchema = newField.create(fieldName);
		schemaB.addField(newFieldSchema);
		schemaB.setDisplayField("name");
		schemaB.setSegmentField("name");
		containerB.setName("migratedSchema");
		containerB.setSchema(schemaB);

		// link the schemas with the change in between
		UpdateFieldChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		change.setFieldName(fieldName);
		change.setFieldProperty("type", newFieldSchema.getType());
		if (newFieldSchema instanceof ListFieldSchema) {
			change.setFieldProperty("listType", ((ListFieldSchema) newFieldSchema).getListType());
		}
		change.setPreviousSchemaContainer(containerA);
		change.setNextSchemaContainer(containerB);
		containerA.setNextVersion(containerB);

		// create a node based on the old schema
		User user = user();
		Language english = english();
		Node parentNode = folder("2015");
		Node node = parentNode.create(user, containerA, project());
		NodeGraphFieldContainer englishContainer = node.getOrCreateGraphFieldContainer(english);
		dataProvider.set(englishContainer, fieldName);

		// migrate the node
		nodeMigrationHandler.migrateNodes(containerA);
		node.reload();
		node.getGraphFieldContainer("en").reload();

		// assert that migration worked
		assertThat(node).as("Migrated Node").isOf(containerB).hasTranslation("en");
		asserter.assertThat(englishContainer, fieldName);
	}

	@FunctionalInterface
	protected interface FieldSchemaCreator {
		FieldSchema create(String name);
	}

	@FunctionalInterface
	protected interface DataProvider {
		void set(NodeGraphFieldContainer container, String name);
	}

	@FunctionalInterface
	protected interface FieldFetcher {
		GraphField fetch(NodeGraphFieldContainer container, String name);
	}

	@FunctionalInterface
	protected interface DataAsserter {
		void assertThat(NodeGraphFieldContainer container, String name);
	}
}
