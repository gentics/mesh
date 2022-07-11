package com.gentics.mesh.example;

import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static com.gentics.mesh.example.ExampleUuids.UUID_1;

import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import io.vertx.core.json.JsonObject;

public class SchemaExamples extends AbstractExamples {

	public SchemaUpdateRequest getSchemaUpdateRequest() {
		SchemaUpdateRequest schema = new SchemaUpdateRequest();
		// TODO should i allow changing the name?
		schema.setName("extended-content");
		schema.setDescription("New description");
		return schema;
	}

	public SchemaCreateRequest getSchemaCreateRequest() {
		SchemaCreateRequest schemaCreateRequest = new SchemaCreateRequest();
		schemaCreateRequest.setContainer(true);
		schemaCreateRequest.setDescription("Some description text");
		schemaCreateRequest.setDisplayField("name");
		schemaCreateRequest.setSegmentField("name");
		schemaCreateRequest.setName("video");
		StringFieldSchema nameFieldSchema = new StringFieldSchemaImpl();
		nameFieldSchema.setName("name");
		schemaCreateRequest.addField(nameFieldSchema);
		schemaCreateRequest.validate();
		return schemaCreateRequest;
	}

	public SchemaResponse getSchemaResponse() {
		SchemaResponse schema = new SchemaResponse();
		schema.setUuid(UUID_1);
		schema.setDescription("Example schema description");
		schema.setName("ExampleSchema");
		schema.setSegmentField("name");
		schema.setDisplayField("name");
		// schema.setDescription("Description of the schema");
		// schema.setName("extended-content");
		schema.setPermissions(READ, UPDATE, DELETE, CREATE);

		StringFieldSchema nameFieldSchema = new StringFieldSchemaImpl();
		nameFieldSchema.setName("name");
		nameFieldSchema.setLabel("Name");
		schema.addField(nameFieldSchema);

		NumberFieldSchema numberFieldSchema = new NumberFieldSchemaImpl();
		numberFieldSchema.setName("number");
		numberFieldSchema.setLabel("Number");
		//		numberFieldSchema.setMin(2);
		//		numberFieldSchema.setMax(10);
		//		numberFieldSchema.setStep(0.5F);
		schema.addField(numberFieldSchema);

		HtmlFieldSchema htmlFieldSchema = new HtmlFieldSchemaImpl();
		htmlFieldSchema.setName("html");
		htmlFieldSchema.setLabel("Teaser");
		schema.addField(htmlFieldSchema);

		ListFieldSchema listFieldSchema = new ListFieldSchemaImpl();
		listFieldSchema.setAllowedSchemas(new String[] { "content", "video" });
		//		listFieldSchema.setMin(1);
		//		listFieldSchema.setMax(10);
		listFieldSchema.setLabel("List of nodes");
		listFieldSchema.setName("Nodes");
		listFieldSchema.setListType("node");
		listFieldSchema.setName("list");
		schema.addField(listFieldSchema);

		NodeFieldSchema nodeFieldSchema = new NodeFieldSchemaImpl();
		nodeFieldSchema.setAllowedSchemas(new String[] { "content", "video", "image" });
		nodeFieldSchema.setName("node");
		schema.addField(nodeFieldSchema);

		MicronodeFieldSchema micronodeFieldSchema = new MicronodeFieldSchemaImpl();
		micronodeFieldSchema.setName("location");
		micronodeFieldSchema.setLabel("Location");
		micronodeFieldSchema.setAllowedMicroSchemas(new String[] { "geolocation" });
		schema.addField(micronodeFieldSchema);

		ListFieldSchemaImpl micronodeListFieldSchema = new ListFieldSchemaImpl();
		micronodeListFieldSchema.setName("locationlist");
		micronodeListFieldSchema.setLabel("List of Locations");
		micronodeListFieldSchema.setListType("micronode");
		micronodeListFieldSchema.setAllowedSchemas(new String[] { "geolocation" });
		schema.addField(micronodeListFieldSchema);
		schema.validate();
		return schema;
	}

	public SchemaListResponse getSchemaListResponse() {
		SchemaListResponse schemaList = new SchemaListResponse();
		schemaList.getData().add(getSchemaResponse());
		schemaList.getData().add(getSchemaResponse());
		setPaging(schemaList, 1, 10, 2, 20);
		return schemaList;
	}

	public SchemaChangesListModel getSchemaChangesListModel() {
		SchemaChangesListModel model = new SchemaChangesListModel();
		// Add field
		SchemaChangeModel addFieldChange = SchemaChangeModel.createAddFieldChange("listFieldToBeAddedField", "list", "Field Label Value", new JsonObject().put("key", "value"));
		addFieldChange.setProperty(SchemaChangeModel.LIST_TYPE_KEY, "html");
		model.getChanges().add(addFieldChange);

		// Change field type
		model.getChanges().add(SchemaChangeModel.createChangeFieldTypeChange("fieldToBeUpdated", "string"));

		// Remove field
		model.getChanges().add(SchemaChangeModel.createRemoveFieldChange("fieldToBeRemoved"));

		// Update field
		SchemaChangeModel updateFieldChange = new SchemaChangeModel(UPDATEFIELD, "fieldToBeUpdated");
		updateFieldChange.setProperty(SchemaChangeModel.LABEL_KEY, "newLabel");
		model.getChanges().add(updateFieldChange);

		// Update schema
		SchemaChangeModel updateSchemaChange = SchemaChangeModel.createUpdateSchemaChange();
		updateFieldChange.setProperty(SchemaChangeModel.DISPLAY_FIELD_NAME_KEY, "newDisplayField");
		model.getChanges().add(updateSchemaChange);

		return model;
	}

}
