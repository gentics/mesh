package com.gentics.mesh.core.data.node.field;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.util.DateUtils.fromISO8601;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.data.Field;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.BooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.DateFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.MicronodeFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberFieldList;
import com.gentics.mesh.core.data.node.field.list.StringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.NodeField;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.s3binary.S3Binary;
import com.gentics.mesh.core.data.s3binary.S3BinaryField;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;
import com.gentics.mesh.core.rest.node.field.BinaryFieldModel;
import com.gentics.mesh.core.rest.node.field.BooleanFieldModel;
import com.gentics.mesh.core.rest.node.field.DateFieldModel;
import com.gentics.mesh.core.rest.node.field.HtmlFieldModel;
import com.gentics.mesh.core.rest.node.field.MicronodeFieldModel;
import com.gentics.mesh.core.rest.node.field.NodeFieldModel;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.NumberFieldModel;
import com.gentics.mesh.core.rest.node.field.S3BinaryFieldModel;
import com.gentics.mesh.core.rest.node.field.StringFieldModel;
import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadataModel;
import com.gentics.mesh.core.rest.node.field.binary.LocationModel;
import com.gentics.mesh.core.rest.node.field.image.FocalPointModel;
import com.gentics.mesh.core.rest.node.field.image.Point;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldListModel;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldListModel;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryMetadataModel;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.S3BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.util.DateUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestUpdaters {

	private static Logger log = LoggerFactory.getLogger(RestUpdaters.class);

	public static FieldUpdater STRING_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		StringFieldModel stringField = fieldMap.getStringField(fieldKey);
		StringField graphStringField = container.getString(fieldKey);
		boolean isStringFieldSetToNull = fieldMap.hasField(fieldKey) && (stringField == null || stringField.getString() == null);
		Field.failOnDeletionOfRequiredField(graphStringField, isStringFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = stringField == null || stringField.getString() == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			Field.failOnMissingRequiredField(graphStringField, restIsNullOrEmpty, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion
		if (isStringFieldSetToNull && graphStringField != null) {
			container.removeField(graphStringField);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNullOrEmpty) {
			return;
		}

		// check value restrictions
		StringFieldSchema stringFieldSchema = (StringFieldSchema) fieldSchema;
		String[] allowedStrings = stringFieldSchema.getAllowedValues();
		if (allowedStrings != null && allowedStrings.length != 0) {
			if (stringField.getString() != null && !Arrays.asList(allowedStrings).contains(stringField.getString())) {
				throw error(BAD_REQUEST, "node_error_invalid_string_field_value", fieldKey, stringField.getString());
			}
		}

		// Handle Update / Create
		if (graphStringField == null) {
			graphStringField = container.createString(fieldKey);
		}
		graphStringField.setString(stringField.getString());

	};

	public static FieldUpdater STRING_LIST_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		StringFieldList graphStringList = container.getStringList(fieldKey);
		StringFieldListImpl stringList = fieldMap.getStringFieldList(fieldKey);
		boolean isStringListFieldSetToNull = fieldMap.hasField(fieldKey) && (stringList == null || stringList.getItems() == null);
		Field.failOnDeletionOfRequiredField(graphStringList, isStringListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull = stringList == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			Field.failOnMissingRequiredField(graphStringList, restIsNull, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion
		if (isStringListFieldSetToNull && graphStringList != null) {
			container.removeField(graphStringList);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNull) {
			return;
		}

		// Always create a new list.
		// This will effectively unlink the old list and create a new one.
		// Otherwise the list which is linked to old versions would be updated.
		graphStringList = container.createStringList(fieldKey);

		// Handle Update
		graphStringList.removeAll();
		for (String item : stringList.getItems()) {
			if (item == null) {
				throw error(BAD_REQUEST, "field_list_error_null_not_allowed", fieldKey);
			}
		}
		graphStringList.createStrings(stringList.getItems());
	};

	public static FieldUpdater NUMBER_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		NumberField numberGraphField = container.getNumber(fieldKey);
		NumberFieldModel numberField = fieldMap.getNumberField(fieldKey);
		boolean isNumberFieldSetToNull = fieldMap.hasField(fieldKey) && (numberField == null || numberField.getNumber() == null);
		Field.failOnDeletionOfRequiredField(numberGraphField, isNumberFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = numberField == null || numberField.getNumber() == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			Field.failOnMissingRequiredField(numberGraphField, restIsNullOrEmpty, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion
		if (isNumberFieldSetToNull && numberGraphField != null) {
			container.removeField(numberGraphField);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNullOrEmpty) {
			return;
		}

		// Handle Update / Create
		if (numberGraphField == null) {
			container.createNumber(fieldKey).setNumber(numberField.getNumber());
		} else {
			numberGraphField.setNumber(numberField.getNumber());
		}
	};

	public static FieldUpdater NUMBER_LIST_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		NumberFieldListImpl numberList = fieldMap.getNumberFieldList(fieldKey);

		NumberFieldList graphNumberFieldList = container.getNumberList(fieldKey);
		boolean isNumberListFieldSetToNull = fieldMap.hasField(fieldKey) && numberList == null;
		Field.failOnDeletionOfRequiredField(graphNumberFieldList, isNumberListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull = numberList == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			Field.failOnMissingRequiredField(graphNumberFieldList, restIsNull, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion
		if (isNumberListFieldSetToNull && graphNumberFieldList != null) {
			container.removeField(graphNumberFieldList);
			return;
		}

		// Rest model is null - Abort
		if (restIsNull) {
			return;
		}

		// Always create a new list.
		// This will effectively unlink the old list and create a new one.
		// Otherwise the list which is linked to old versions would be updated.
		graphNumberFieldList = container.createNumberList(fieldKey);

		// Handle Update
		graphNumberFieldList.removeAll();
		for (Number item : numberList.getItems()) {
			if (item == null) {
				throw error(BAD_REQUEST, "field_list_error_null_not_allowed", fieldKey);
			}
		}
		graphNumberFieldList.createNumbers(numberList.getItems());
	};

	public static FieldUpdater DATE_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		DateField dateGraphField = container.getDate(fieldKey);
		DateFieldModel dateField = fieldMap.getDateField(fieldKey);
		boolean isDateFieldSetToNull = fieldMap.hasField(fieldKey) && (dateField == null || dateField.getDate() == null);
		Field.failOnDeletionOfRequiredField(dateGraphField, isDateFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = dateField == null || dateField.getDate() == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			Field.failOnMissingRequiredField(dateGraphField, restIsNullOrEmpty, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion - The field was explicitly set to null and is currently set in the graph so we can remove the field from the given container
		if (isDateFieldSetToNull && dateGraphField != null) {
			container.removeField(dateGraphField);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNullOrEmpty) {
			return;
		}

		// Handle Update / Create
		if (dateGraphField == null) {
			container.createDate(fieldKey).setDate(fromISO8601(dateField.getDate(), true));
		} else {
			dateGraphField.setDate(fromISO8601(dateField.getDate(), true));
		}
	};

	public static FieldUpdater DATE_LIST_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		DateFieldList graphDateFieldList = container.getDateList(fieldKey);
		DateFieldListImpl dateList = fieldMap.getDateFieldList(fieldKey);
		boolean isDateListFieldSetToNull = fieldMap.hasField(fieldKey) && (dateList == null);
		Field.failOnDeletionOfRequiredField(graphDateFieldList, isDateListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull = dateList == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			Field.failOnMissingRequiredField(graphDateFieldList, restIsNull, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion
		if (isDateListFieldSetToNull && graphDateFieldList != null) {
			container.removeField(graphDateFieldList);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNull) {
			return;
		}

		// Always create a new list.
		// This will effectively unlink the old list and create a new one.
		// Otherwise the list which is linked to old versions would be updated.
		graphDateFieldList = container.createDateList(fieldKey);

		// Handle Update
		graphDateFieldList.removeAll();
		for (String item : dateList.getItems()) {
			if (item == null) {
				throw error(BAD_REQUEST, "field_list_error_null_not_allowed", fieldKey);
			}
		}
		graphDateFieldList.createDates(dateList.getItems().stream().map(DateUtils::fromISO8601).collect(Collectors.toList()));
	};

	public static FieldUpdater BOOLEAN_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		BooleanField booleanGraphField = container.getBoolean(fieldKey);
		BooleanFieldModel booleanField = fieldMap.getBooleanField(fieldKey);
		boolean isBooleanFieldSetToNull = fieldMap.hasField(fieldKey) && (booleanField == null || booleanField.getValue() == null);
		Field.failOnDeletionOfRequiredField(booleanGraphField, isBooleanFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = booleanField == null || booleanField.getValue() == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			Field.failOnMissingRequiredField(booleanGraphField, restIsNullOrEmpty, fieldSchema, fieldKey, schema);
		}

		// Handle deletion
		if (isBooleanFieldSetToNull && booleanGraphField != null) {
			container.removeField(booleanGraphField);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNullOrEmpty) {
			return;
		}

		// Handle Update / Create
		if (booleanGraphField == null) {
			container.createBoolean(fieldKey).setBoolean(booleanField.getValue());
		} else {
			booleanGraphField.setBoolean(booleanField.getValue());
		}
	};

	public static FieldUpdater BOOLEAN_LIST_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		BooleanFieldList graphBooleanFieldList = container.getBooleanList(fieldKey);
		BooleanFieldListImpl booleanList = fieldMap.getBooleanFieldList(fieldKey);
		boolean isBooleanListFieldSetToNull = fieldMap.hasField(fieldKey) && booleanList == null;
		Field.failOnDeletionOfRequiredField(graphBooleanFieldList, isBooleanListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull = booleanList == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			Field.failOnMissingRequiredField(graphBooleanFieldList, restIsNull, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion
		if (isBooleanListFieldSetToNull && graphBooleanFieldList != null) {
			container.removeField(graphBooleanFieldList);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNull) {
			return;
		}

		// Always create a new list.
		// This will effectively unlink the old list and create a new one.
		// Otherwise the list which is linked to old versions would be updated.
		graphBooleanFieldList = container.createBooleanList(fieldKey);

		// Handle Update
		// Remove all and add the listed items
		graphBooleanFieldList.removeAll();
		for (Boolean item : booleanList.getItems()) {
			if (item == null) {
				throw error(BAD_REQUEST, "field_list_error_null_not_allowed", fieldKey);
			}
		}
		graphBooleanFieldList.createBooleans(booleanList.getItems());
	};

	public static FieldUpdater HTML_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		HtmlFieldModel htmlField = fieldMap.getHtmlField(fieldKey);
		HtmlField htmlGraphField = container.getHtml(fieldKey);
		boolean isHtmlFieldSetToNull = fieldMap.hasField(fieldKey) && (htmlField == null || htmlField.getHTML() == null);
		Field.failOnDeletionOfRequiredField(htmlGraphField, isHtmlFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean isHtmlFieldNull = htmlField == null || htmlField.getHTML() == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			Field.failOnMissingRequiredField(htmlGraphField, isHtmlFieldNull, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion - The field was explicitly set to null and is currently set within the graph thus we must remove it.
		if (isHtmlFieldSetToNull && htmlGraphField != null) {
			container.removeField(htmlGraphField);
			return;
		}

		// Rest model is empty or null - Abort
		if (isHtmlFieldNull) {
			return;
		}

		// Handle Update / Create - Create new graph field if no existing one could be found
		if (htmlGraphField == null) {
			container.createHTML(fieldKey).setHtml(htmlField.getHTML());
		} else {
			htmlGraphField.setHtml(htmlField.getHTML());
		}
	};

	public static FieldUpdater HTML_LIST_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		HtmlFieldList graphHtmlFieldList = container.getHTMLList(fieldKey);
		HtmlFieldListImpl htmlList = fieldMap.getHtmlFieldList(fieldKey);
		boolean isHtmlListFieldSetToNull = fieldMap.hasField(fieldKey) && htmlList == null;
		Field.failOnDeletionOfRequiredField(graphHtmlFieldList, isHtmlListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull = htmlList == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			Field.failOnMissingRequiredField(graphHtmlFieldList, htmlList == null, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion
		if (isHtmlListFieldSetToNull && graphHtmlFieldList != null) {
			container.removeField(graphHtmlFieldList);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNull) {
			return;
		}

		// Always create a new list.
		// This will effectively unlink the old list and create a new one.
		// Otherwise the list which is linked to old versions would be updated.
		graphHtmlFieldList = container.createHTMLList(fieldKey);

		// Add items from rest model
		for (String item : htmlList.getItems()) {
			if (item == null) {
				throw error(BAD_REQUEST, "field_list_error_null_not_allowed", fieldKey);
			}
		}
		graphHtmlFieldList.createHTMLs(htmlList.getItems());
	};

	public static FieldUpdater MICRONODE_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		MicronodeField micronodeGraphField = container.getMicronode(fieldKey);
		MicronodeFieldSchema microschemaFieldSchema = (MicronodeFieldSchema) fieldSchema;
		MicronodeFieldModel micronodeRestField = fieldMap.getMicronodeField(fieldKey);
		boolean isMicronodeFieldSetToNull = fieldMap.hasField(fieldKey) && micronodeRestField == null;
		Field.failOnDeletionOfRequiredField(micronodeGraphField, isMicronodeFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = micronodeRestField == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			Field.failOnMissingRequiredField(container.getMicronode(fieldKey), restIsNullOrEmpty, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion - Remove the field if the field has been explicitly set to null
		if (isMicronodeFieldSetToNull && micronodeGraphField != null) {
			container.removeField(micronodeGraphField);
			return;
		}

		// Rest model is empty or null - Abort
		if (micronodeRestField == null) {
			return;
		}

		MicroschemaReference microschemaReference = micronodeRestField.getMicroschema();
		if (microschemaReference == null || !microschemaReference.isSet()) {
			throw error(BAD_REQUEST, "micronode_error_missing_reference", fieldKey);
		}

		Tx tx = Tx.get();
		MicroschemaDao microschemaDao = tx.microschemaDao();
		MicroschemaVersion microschemaVersion = microschemaDao.fromReference(tx.getProject(ac), microschemaReference,
				tx.getBranch(ac));

		Micronode micronode = null;

		// check whether microschema is allowed
		if (!ArrayUtils.isEmpty(microschemaFieldSchema.getAllowedMicroSchemas())
				&& !Arrays.asList(microschemaFieldSchema.getAllowedMicroSchemas()).contains(microschemaVersion.getName())) {
			throw error(BAD_REQUEST, "node_error_invalid_microschema_field_value", fieldKey, microschemaVersion.getName(), Arrays.toString(microschemaFieldSchema.getAllowedMicroSchemas()));
		}

		// Always create a new micronode field since each update must create a new field instance. The old field must be detached from the given container.
		micronodeGraphField = container.createMicronode(fieldKey, microschemaVersion);
		micronode = micronodeGraphField.getMicronode();

		micronode.updateFieldsFromRest(ac, micronodeRestField.getFields());
	};

	public static FieldUpdater MICRONODE_LIST_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		MicronodeFieldList micronodeGraphFieldList = container.getMicronodeList(fieldKey);
		MicronodeFieldListModel micronodeList = fieldMap.getMicronodeFieldList(fieldKey);
		boolean isMicronodeListFieldSetToNull = fieldMap.hasField(fieldKey) && micronodeList == null;
		Field.failOnDeletionOfRequiredField(micronodeGraphFieldList, isMicronodeListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull = micronodeList == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			Field.failOnMissingRequiredField(micronodeGraphFieldList, restIsNull, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion
		if (isMicronodeListFieldSetToNull && micronodeGraphFieldList != null) {
			container.removeField(micronodeGraphFieldList);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNull) {
			return;
		}

		// Always create a new list.
		// This will effectively unlink the old list and create a new one.
		// Otherwise the list which is linked to old versions would be updated.
		micronodeGraphFieldList = container.createMicronodeList(fieldKey);

		// Handle Update
		// TODO instead this method should also return an observable
		micronodeGraphFieldList.update(ac, micronodeList).blockingGet();
	};

	public static FieldUpdater NODE_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		Tx tx = Tx.get();
		NodeDao nodeDao = tx.nodeDao();

		NodeField nodeFieldReference = container.getNode(fieldKey);
		NodeFieldModel nodeField = fieldMap.getNodeField(fieldKey);
		boolean isNodeFieldSetToNull = fieldMap.hasField(fieldKey) && (nodeField == null);
		Field.failOnDeletionOfRequiredField(nodeFieldReference, isNodeFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = nodeField == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			Field.failOnMissingRequiredField(nodeFieldReference, restIsNullOrEmpty, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion - Remove the field if the field has been explicitly set to null
		if (nodeFieldReference != null && isNodeFieldSetToNull) {
			container.removeField(nodeFieldReference);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNullOrEmpty) {
			return;
		}

		// Check whether the request contains all required information to execute it
		if (StringUtils.isEmpty(nodeField.getUuid())) {
			throw error(BAD_REQUEST, "node_error_field_property_missing", "uuid", fieldKey);
		}

		// Handle Update / Create
		Node node = nodeDao.findByUuid(tx.getProject(ac), nodeField.getUuid());
		if (node == null) {
			// TODO We want to delete the field when the field has been explicitly set to null
			if (log.isDebugEnabled()) {
				log.debug("Node field {" + fieldKey + "} could not be populated since node {" + nodeField.getUuid() + "} could not be found.");
			}
			// TODO we need to fail here - the node could not be found.
			// throw error(NOT_FOUND, "The field {, parameters)
		} else {
			// Check whether the container already contains a node field
			// TODO check node permissions
			// TODO check whether we want to allow cross project node references

			NodeFieldSchema nodeFieldSchema = (NodeFieldSchema) fieldSchema;
			String schemaName = node.getSchemaContainer().getName();

			if (!org.apache.commons.lang.ArrayUtils.isEmpty(nodeFieldSchema.getAllowedSchemas())
					&& !Arrays.asList(nodeFieldSchema.getAllowedSchemas()).contains(schemaName)) {
				throw error(BAD_REQUEST, "node_error_invalid_schema_field_value", fieldKey, schemaName, Arrays.toString(nodeFieldSchema.getAllowedSchemas()));
			}

			// The old node edge is deleted on a new edge creation.
			container.createNode(fieldKey, node);
		}
	};

	public static FieldUpdater NODE_LIST_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		Tx tx = Tx.get();
		NodeDao nodeDao = tx.nodeDao();

		NodeFieldListModel nodeList = fieldMap.getNodeFieldList(fieldKey);
		NodeFieldList graphNodeFieldList = container.getNodeList(fieldKey);
		boolean isNodeListFieldSetToNull = fieldMap.hasField(fieldKey) && (nodeList == null);
		Field.failOnDeletionOfRequiredField(graphNodeFieldList, isNodeListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull = nodeList == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			Field.failOnMissingRequiredField(graphNodeFieldList, restIsNull, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion
		if (isNodeListFieldSetToNull && graphNodeFieldList != null) {
			container.removeField(graphNodeFieldList);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNull) {
			return;
		}

		// Always create a new list.
		// This will effectively unlink the old list and create a new one.
		// Otherwise the list which is linked to old versions would be updated.
		graphNodeFieldList = container.createNodeList(fieldKey);

		// Remove all and add the listed items
		graphNodeFieldList.removeAll();

		// Handle Update
		Project project = tx.getProject(ac);
		AtomicInteger integer = new AtomicInteger();
		for (NodeFieldListItem item : nodeList.getItems()) {
			if (item == null) {
				throw error(BAD_REQUEST, "field_list_error_null_not_allowed", fieldKey);
			}
			Node node = nodeDao.findByUuid(project, item.getUuid());
			if (node == null) {
				throw error(BAD_REQUEST, "node_list_item_not_found", item.getUuid());
			}
			ListFieldSchema listFieldSchema = (ListFieldSchema) fieldSchema;
			String schemaName = node.getSchemaContainer().getName();

			if (!org.apache.commons.lang.ArrayUtils.isEmpty(listFieldSchema.getAllowedSchemas())
					&& !Arrays.asList(listFieldSchema.getAllowedSchemas()).contains(schemaName)) {
				throw error(BAD_REQUEST, "node_error_invalid_schema_field_value", fieldKey, schemaName, Arrays.toString(listFieldSchema.getAllowedSchemas()));
			}
			int pos = integer.getAndIncrement();
			if (log.isDebugEnabled()) {
				log.debug("Adding item {" + item.getUuid() + "} at position {" + pos + "}");
			}
			NodeField nodeItem = graphNodeFieldList.createNode(pos, node);
			if (nodeItem != null) {
				graphNodeFieldList.addItem(nodeItem);
			} else {
				log.warn("The referenced node {" + item.getUuid() + "} does not exist for the field {" + fieldKey + "} of schema {" + schema.getName() + "}");
			}
		}
	};

	public static FieldUpdater BINARY_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		BinaryField graphBinaryField = container.getBinary(fieldKey);
		BinaryFieldModel binaryField = fieldMap.getBinaryField(fieldKey);
		boolean isBinaryFieldSetToNull = fieldMap.hasField(fieldKey) && binaryField == null && graphBinaryField != null;

		Field.failOnDeletionOfRequiredField(graphBinaryField, isBinaryFieldSetToNull, fieldSchema, fieldKey, schema);

		boolean restIsNull = binaryField == null;
		// The required check for binary fields is not enabled since binary fields can only be created using the field api

		// Handle Deletion
		if (isBinaryFieldSetToNull && graphBinaryField != null) {
			container.removeField(graphBinaryField);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNull) {
			return;
		}

		// The binary field does not yet exist but the update request already contains some binary field info. We can use this info to create a new binary
		// field. We locate the binary vertex by using the given hashsum. This case usually happens during schema migrations in which the binary graph field is
		// in fact initially being removed from the container.
		String hash = binaryField.getSha512sum();
		if (graphBinaryField == null && hash != null) {
			Binary binary = Tx.get().binaries().findByHash(hash).runInExistingTx(Tx.get());
			if (binary != null) {
				graphBinaryField = container.createBinary(fieldKey, binary);
			} else {
				log.debug("Could not find binary for hash {" + hash + "}");
			}
		}

		// Otherwise we can't update the binaryfield
		if (graphBinaryField == null && binaryField.hasValues()) {
			throw error(BAD_REQUEST, "field_binary_error_unable_to_set_before_upload", fieldKey);
		}

		// Handle Update - Dominant Color
		if (binaryField.getDominantColor() != null) {
			graphBinaryField.setImageDominantColor(binaryField.getDominantColor());
		}

		// Handle Update - Focal point
		FocalPointModel newFocalPoint = binaryField.getFocalPoint();
		if (newFocalPoint != null) {
			Binary binary = graphBinaryField.getBinary();
			Point imageSize = binary.getImageSize();
			if (imageSize != null) {
				if (!newFocalPoint.convertToAbsolutePoint(imageSize).isWithinBoundsOf(imageSize)) {
					throw error(BAD_REQUEST, "field_binary_error_image_focalpoint_out_of_bounds", fieldKey, newFocalPoint.toString(),
							imageSize.toString());
				}
			}
			graphBinaryField.setImageFocalPoint(newFocalPoint);
		}

		// Handle Update - Filename
		if (binaryField.getFileName() != null) {
			if (isEmpty(binaryField.getFileName())) {
				throw error(BAD_REQUEST, "field_binary_error_emptyfilename", fieldKey);
			} else {
				graphBinaryField.setFileName(binaryField.getFileName());
			}
		}

		// Handle Update - MimeType
		if (binaryField.getMimeType() != null) {
			if (isEmpty(binaryField.getMimeType())) {
				throw error(BAD_REQUEST, "field_binary_error_emptymimetype", fieldKey);
			}
			graphBinaryField.setMimeType(binaryField.getMimeType());
		}

		// Handle Update - Metadata
		BinaryMetadataModel metaData = binaryField.getMetadata();
		if (metaData != null) {
			graphBinaryField.clearMetadata();
			for (Map.Entry<String, String> entry : metaData.getMap().entrySet()) {
				graphBinaryField.setMetadata(entry.getKey(), entry.getValue());
			}
			LocationModel loc = metaData.getLocation();
			if (loc != null) {
				graphBinaryField.setLocation(loc);
			}
		}

		// Handle Update - Plain text
		String text = binaryField.getPlainText();
		if (text != null) {
			graphBinaryField.setPlainText(text);
		}

		if (graphBinaryField != null && graphBinaryField.getBinary() != null) {
			if (StringUtils.isBlank(((BinaryFieldSchema) fieldSchema).getCheckServiceUrl())) {
				graphBinaryField.getBinary().setCheckStatus(BinaryCheckStatus.ACCEPTED);
			} else {
				graphBinaryField.getBinary().setCheckStatus(BinaryCheckStatus.POSTPONED);
			}
		}
		// Don't update image width, height, SHA checksum - those are immutable
	};

	public static FieldUpdater S3_BINARY_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		S3BinaryField graphS3BinaryField = container.getS3Binary(fieldKey);
		S3BinaryFieldModel s3binaryField = fieldMap.getS3BinaryField(fieldKey);
		boolean isS3BinaryFieldSetToNull = fieldMap.hasField(fieldKey) && s3binaryField == null && graphS3BinaryField != null;

		Field.failOnDeletionOfRequiredField(graphS3BinaryField, isS3BinaryFieldSetToNull, fieldSchema, fieldKey, schema);

		boolean restIsNull = s3binaryField == null;
		// The required check for binary fields is not enabled since binary fields can only be created using the field api

		// Handle Deletion
		if (isS3BinaryFieldSetToNull && graphS3BinaryField != null) {
			container.removeField(graphS3BinaryField);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNull) {
			return;
		}

		// The S3binary field does not yet exist but the update request already contains some binary field info. We can use this info to create a new binary
		// field. We locate the binary vertex by using the given hashsum. This case usually happens during schema migrations in which the binary graph field is
		// in fact initially being removed from the container.
		String s3ObjectKey = s3binaryField.getS3ObjectKey();
		if (graphS3BinaryField == null && s3ObjectKey != null) {
			S3Binary binary = Tx.get().s3binaries().findByS3ObjectKey(s3ObjectKey).runInExistingTx(Tx.get());
			if (binary != null) {
				graphS3BinaryField = container.createS3Binary(fieldKey, binary);
			} else {
				log.debug("Could not find binary for s3ObjectKey {" + s3ObjectKey + "}");
			}
		}

		// Otherwise we can't update the s3binaryField
		if (graphS3BinaryField == null && s3binaryField.hasValues()) {
			throw error(BAD_REQUEST, "field_binary_error_unable_to_set_before_upload", fieldKey);
		}

		// Handle Update - Dominant Color
		if (s3binaryField.getDominantColor() != null) {
			graphS3BinaryField.setImageDominantColor(s3binaryField.getDominantColor());
		}

		// Handle Update - Focal point
		FocalPointModel newFocalPoint = s3binaryField.getFocalPoint();
		if (newFocalPoint != null) {
			S3Binary binary = graphS3BinaryField.getBinary();
			Point imageSize = binary.getImageSize();
			if (imageSize != null) {
				if (!newFocalPoint.convertToAbsolutePoint(imageSize).isWithinBoundsOf(imageSize)) {
					throw error(BAD_REQUEST, "field_binary_error_image_focalpoint_out_of_bounds", fieldKey, newFocalPoint.toString(),
							imageSize.toString());
				}
			}
			graphS3BinaryField.setImageFocalPoint(newFocalPoint);
		}

		// Handle Update - Filename
		if (s3binaryField.getFileName() != null) {
			if (isEmpty(s3binaryField.getFileName())) {
				throw error(BAD_REQUEST, "field_binary_error_emptyfilename", fieldKey);
			} else {
				graphS3BinaryField.setFileName(s3binaryField.getFileName());
			}
		}

		// Handle Update - MimeType
		if (s3binaryField.getMimeType() != null) {
			if (isEmpty(s3binaryField.getMimeType())) {
				throw error(BAD_REQUEST, "field_binary_error_emptymimetype", fieldKey);
			}
			graphS3BinaryField.setMimeType(s3binaryField.getMimeType());
		}

		// Handle Update - Metadata
		S3BinaryMetadataModel metaData = s3binaryField.getMetadata();
		if (metaData != null) {
			graphS3BinaryField.clearMetadata();
			for (Map.Entry<String, String> entry : metaData.getMap().entrySet()) {
				graphS3BinaryField.setMetadata(entry.getKey(), entry.getValue());
			}
			LocationModel loc = metaData.getLocation();
			if (loc != null) {
				graphS3BinaryField.setLocation(loc);
			}
		}

		// Handle Update - Plain text
		String text = s3binaryField.getPlainText();
		if (text != null) {
			graphS3BinaryField.setPlainText(text);
		}

		// Handle Update - Plain text
		String key = s3binaryField.getS3ObjectKey();
		if (s3ObjectKey != null) {
			graphS3BinaryField.setS3ObjectKey(key);
		}

		if (graphS3BinaryField != null && graphS3BinaryField.getBinary() != null) {
			if (StringUtils.isBlank(((S3BinaryFieldSchema) fieldSchema).getCheckServiceUrl())) {
				graphS3BinaryField.getBinary().setCheckStatus(BinaryCheckStatus.ACCEPTED);
			} else {
				graphS3BinaryField.getBinary().setCheckStatus(BinaryCheckStatus.POSTPONED);
			}
		}
		// Don't update image width, height, SHA checksum - those are immutable
	};
}
