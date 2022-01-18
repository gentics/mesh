package com.gentics.mesh.core.data.node.field;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.util.DateUtils.fromISO8601;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.list.HibBooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.HibDateFieldList;
import com.gentics.mesh.core.data.node.field.list.HibHtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.HibMicronodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNumberFieldList;
import com.gentics.mesh.core.data.node.field.list.HibStringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.S3BinaryField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadata;
import com.gentics.mesh.core.rest.node.field.binary.Location;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.image.Point;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryMetadata;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class RestUpdaters {

	private static Logger log = LoggerFactory.getLogger(RestUpdaters.class);

	public static FieldUpdater STRING_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		StringField stringField = fieldMap.getStringField(fieldKey);
		HibStringField graphStringField = container.getString(fieldKey);
		boolean isStringFieldSetToNull = fieldMap.hasField(fieldKey) && (stringField == null || stringField.getString() == null);
		HibField.failOnDeletionOfRequiredField(graphStringField, isStringFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = stringField == null || stringField.getString() == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			HibField.failOnMissingRequiredField(graphStringField, restIsNullOrEmpty, fieldSchema, fieldKey, schema);
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
		HibStringFieldList graphStringList = container.getStringList(fieldKey);
		StringFieldListImpl stringList = fieldMap.getStringFieldList(fieldKey);
		boolean isStringListFieldSetToNull = fieldMap.hasField(fieldKey) && (stringList == null || stringList.getItems() == null);
		HibField.failOnDeletionOfRequiredField(graphStringList, isStringListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull = stringList == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			HibField.failOnMissingRequiredField(graphStringList, restIsNull, fieldSchema, fieldKey, schema);
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
			graphStringList.createString(item);
		}
	};

	public static FieldUpdater NUMBER_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		HibNumberField numberGraphField = container.getNumber(fieldKey);
		NumberField numberField = fieldMap.getNumberField(fieldKey);
		boolean isNumberFieldSetToNull = fieldMap.hasField(fieldKey) && (numberField == null || numberField.getNumber() == null);
		HibField.failOnDeletionOfRequiredField(numberGraphField, isNumberFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = numberField == null || numberField.getNumber() == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			HibField.failOnMissingRequiredField(numberGraphField, restIsNullOrEmpty, fieldSchema, fieldKey, schema);
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

		HibNumberFieldList graphNumberFieldList = container.getNumberList(fieldKey);
		boolean isNumberListFieldSetToNull = fieldMap.hasField(fieldKey) && numberList == null;
		HibField.failOnDeletionOfRequiredField(graphNumberFieldList, isNumberListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull = numberList == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			HibField.failOnMissingRequiredField(graphNumberFieldList, restIsNull, fieldSchema, fieldKey, schema);
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
			graphNumberFieldList.createNumber(item);
		}

	};

	public static FieldUpdater DATE_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		HibDateField dateGraphField = container.getDate(fieldKey);
		DateField dateField = fieldMap.getDateField(fieldKey);
		boolean isDateFieldSetToNull = fieldMap.hasField(fieldKey) && (dateField == null || dateField.getDate() == null);
		HibField.failOnDeletionOfRequiredField(dateGraphField, isDateFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = dateField == null || dateField.getDate() == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			HibField.failOnMissingRequiredField(dateGraphField, restIsNullOrEmpty, fieldSchema, fieldKey, schema);
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
		HibDateFieldList graphDateFieldList = container.getDateList(fieldKey);
		DateFieldListImpl dateList = fieldMap.getDateFieldList(fieldKey);
		boolean isDateListFieldSetToNull = fieldMap.hasField(fieldKey) && (dateList == null);
		HibField.failOnDeletionOfRequiredField(graphDateFieldList, isDateListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull = dateList == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			HibField.failOnMissingRequiredField(graphDateFieldList, restIsNull, fieldSchema, fieldKey, schema);
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
			graphDateFieldList.createDate(fromISO8601(item));
		}

	};

	public static FieldUpdater BOOLEAN_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		HibBooleanField booleanGraphField = container.getBoolean(fieldKey);
		BooleanField booleanField = fieldMap.getBooleanField(fieldKey);
		boolean isBooleanFieldSetToNull = fieldMap.hasField(fieldKey) && (booleanField == null || booleanField.getValue() == null);
		HibField.failOnDeletionOfRequiredField(booleanGraphField, isBooleanFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = booleanField == null || booleanField.getValue() == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			HibField.failOnMissingRequiredField(booleanGraphField, restIsNullOrEmpty, fieldSchema, fieldKey, schema);
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
		HibBooleanFieldList graphBooleanFieldList = container.getBooleanList(fieldKey);
		BooleanFieldListImpl booleanList = fieldMap.getBooleanFieldList(fieldKey);
		boolean isBooleanListFieldSetToNull = fieldMap.hasField(fieldKey) && booleanList == null;
		HibField.failOnDeletionOfRequiredField(graphBooleanFieldList, isBooleanListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull = booleanList == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			HibField.failOnMissingRequiredField(graphBooleanFieldList, restIsNull, fieldSchema, fieldKey, schema);
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
			graphBooleanFieldList.createBoolean(item);
		}

	};

	public static FieldUpdater HTML_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		HtmlField htmlField = fieldMap.getHtmlField(fieldKey);
		HibHtmlField htmlGraphField = container.getHtml(fieldKey);
		boolean isHtmlFieldSetToNull = fieldMap.hasField(fieldKey) && (htmlField == null || htmlField.getHTML() == null);
		HibField.failOnDeletionOfRequiredField(htmlGraphField, isHtmlFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean isHtmlFieldNull = htmlField == null || htmlField.getHTML() == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			HibField.failOnMissingRequiredField(htmlGraphField, isHtmlFieldNull, fieldSchema, fieldKey, schema);
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
		HibHtmlFieldList graphHtmlFieldList = container.getHTMLList(fieldKey);
		HtmlFieldListImpl htmlList = fieldMap.getHtmlFieldList(fieldKey);
		boolean isHtmlListFieldSetToNull = fieldMap.hasField(fieldKey) && htmlList == null;
		HibField.failOnDeletionOfRequiredField(graphHtmlFieldList, isHtmlListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull = htmlList == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			HibField.failOnMissingRequiredField(graphHtmlFieldList, htmlList == null, fieldSchema, fieldKey, schema);
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
			graphHtmlFieldList.createHTML(item);
		}
	};

	public static FieldUpdater MICRONODE_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		HibMicronodeField micronodeGraphField = container.getMicronode(fieldKey);
		MicronodeFieldSchema microschemaFieldSchema = (MicronodeFieldSchema) fieldSchema;
		MicronodeField micronodeRestField = fieldMap.getMicronodeField(fieldKey);
		boolean isMicronodeFieldSetToNull = fieldMap.hasField(fieldKey) && micronodeRestField == null;
		HibField.failOnDeletionOfRequiredField(micronodeGraphField, isMicronodeFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = micronodeRestField == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			HibField.failOnMissingRequiredField(container.getMicronode(fieldKey), restIsNullOrEmpty, fieldSchema, fieldKey, schema);
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
		HibMicroschemaVersion microschemaVersion = microschemaDao.fromReference(tx.getProject(ac), microschemaReference,
				tx.getBranch(ac));

		HibMicronode micronode = null;

		// check whether microschema is allowed
		if (!ArrayUtils.isEmpty(microschemaFieldSchema.getAllowedMicroSchemas())
				&& !Arrays.asList(microschemaFieldSchema.getAllowedMicroSchemas()).contains(microschemaVersion.getName())) {
			log.error("Node update not allowed since the microschema {" + microschemaVersion.getName()
					+ "} is now allowed. Allowed microschemas {" + Arrays.toString(microschemaFieldSchema.getAllowedMicroSchemas()) + "}");
			throw error(BAD_REQUEST, "node_error_invalid_microschema_field_value", fieldKey, microschemaVersion.getName());
		}

		// Always create a new micronode field since each update must create a new field instance. The old field must be detached from the given container.
		micronodeGraphField = container.createMicronode(fieldKey, microschemaVersion);
		micronode = micronodeGraphField.getMicronode();

		micronode.updateFieldsFromRest(ac, micronodeRestField.getFields());
	};

	public static FieldUpdater MICRONODE_LIST_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		HibMicronodeFieldList micronodeGraphFieldList = container.getMicronodeList(fieldKey);
		MicronodeFieldList micronodeList = fieldMap.getMicronodeFieldList(fieldKey);
		boolean isMicronodeListFieldSetToNull = fieldMap.hasField(fieldKey) && micronodeList == null;
		HibField.failOnDeletionOfRequiredField(micronodeGraphFieldList, isMicronodeListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull = micronodeList == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			HibField.failOnMissingRequiredField(micronodeGraphFieldList, restIsNull, fieldSchema, fieldKey, schema);
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

		HibNodeField nodeFieldReference = container.getNode(fieldKey);
		NodeField nodeField = fieldMap.getNodeField(fieldKey);
		boolean isNodeFieldSetToNull = fieldMap.hasField(fieldKey) && (nodeField == null);
		HibField.failOnDeletionOfRequiredField(nodeFieldReference, isNodeFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = nodeField == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			HibField.failOnMissingRequiredField(nodeFieldReference, restIsNullOrEmpty, fieldSchema, fieldKey, schema);
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
		HibNode node = nodeDao.findByUuid(tx.getProject(ac), nodeField.getUuid());
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
				log.error("Node update not allowed since the schema {" + schemaName
						+ "} is not allowed. Allowed schemas {" + Arrays.toString(nodeFieldSchema.getAllowedSchemas()) + "}");
				throw error(BAD_REQUEST, "node_error_invalid_schema_field_value", fieldKey, schemaName);
			}
			if (nodeFieldReference != null) {
				// We can't update the graphNodeField since it is in fact an edge.
				// We need to delete it and create a new one.
				container.deleteFieldEdge(fieldKey);				
			}
			container.createNode(fieldKey, node);
		}
	};

	public static FieldUpdater NODE_LIST_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		Tx tx = Tx.get();
		NodeDao nodeDao = tx.nodeDao();

		NodeFieldList nodeList = fieldMap.getNodeFieldList(fieldKey);
		HibNodeFieldList graphNodeFieldList = container.getNodeList(fieldKey);
		boolean isNodeListFieldSetToNull = fieldMap.hasField(fieldKey) && (nodeList == null);
		HibField.failOnDeletionOfRequiredField(graphNodeFieldList, isNodeListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull = nodeList == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			HibField.failOnMissingRequiredField(graphNodeFieldList, restIsNull, fieldSchema, fieldKey, schema);
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
		HibProject project = tx.getProject(ac);
		AtomicInteger integer = new AtomicInteger();
		for (NodeFieldListItem item : nodeList.getItems()) {
			if (item == null) {
				throw error(BAD_REQUEST, "field_list_error_null_not_allowed", fieldKey);
			}
			HibNode node = nodeDao.findByUuid(project, item.getUuid());
			if (node == null) {
				throw error(BAD_REQUEST, "node_list_item_not_found", item.getUuid());
			}
			ListFieldSchema listFieldSchema = (ListFieldSchema) fieldSchema;
			String schemaName = node.getSchemaContainer().getName();

			if (!org.apache.commons.lang.ArrayUtils.isEmpty(listFieldSchema.getAllowedSchemas())
					&& !Arrays.asList(listFieldSchema.getAllowedSchemas()).contains(schemaName)) {
				log.error("Node update not allowed since the schema {" + schemaName
						+ "} is not allowed. Allowed schemas {" + Arrays.toString(listFieldSchema.getAllowedSchemas()) + "}");
				throw error(BAD_REQUEST, "node_error_invalid_schema_field_value", fieldKey, schemaName);
			}
			int pos = integer.getAndIncrement();
			if (log.isDebugEnabled()) {
				log.debug("Adding item {" + item.getUuid() + "} at position {" + pos + "}");
			}
			graphNodeFieldList.addItem(graphNodeFieldList.createNode(String.valueOf(pos), node));
		}

	};

	public static FieldUpdater BINARY_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		HibBinaryField graphBinaryField = container.getBinary(fieldKey);
		BinaryField binaryField = fieldMap.getBinaryField(fieldKey);
		boolean isBinaryFieldSetToNull = fieldMap.hasField(fieldKey) && binaryField == null && graphBinaryField != null;

		HibField.failOnDeletionOfRequiredField(graphBinaryField, isBinaryFieldSetToNull, fieldSchema, fieldKey, schema);

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
			HibBinary binary = Tx.get().binaries().findByHash(hash).runInExistingTx(Tx.get());
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
		FocalPoint newFocalPoint = binaryField.getFocalPoint();
		if (newFocalPoint != null) {
			HibBinary binary = graphBinaryField.getBinary();
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
		BinaryMetadata metaData = binaryField.getMetadata();
		if (metaData != null) {
			graphBinaryField.clearMetadata();
			for (Map.Entry<String, String> entry : metaData.getMap().entrySet()) {
				graphBinaryField.setMetadata(entry.getKey(), entry.getValue());
			}
			Location loc = metaData.getLocation();
			if (loc != null) {
				graphBinaryField.setLocation(loc);
			}
		}

		// Handle Update - Plain text
		String text = binaryField.getPlainText();
		if (text != null) {
			graphBinaryField.setPlainText(text);
		}

		// Don't update image width, height, SHA checksum - those are immutable
	};

	public static FieldUpdater S3_BINARY_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		S3HibBinaryField graphS3BinaryField = container.getS3Binary(fieldKey);
		S3BinaryField s3binaryField = fieldMap.getS3BinaryField(fieldKey);
		boolean isS3BinaryFieldSetToNull = fieldMap.hasField(fieldKey) && s3binaryField == null && graphS3BinaryField != null;

		HibField.failOnDeletionOfRequiredField(graphS3BinaryField, isS3BinaryFieldSetToNull, fieldSchema, fieldKey, schema);

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
			S3HibBinary binary = Tx.get().s3binaries().findByS3ObjectKey(s3ObjectKey).runInExistingTx(Tx.get());
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
		FocalPoint newFocalPoint = s3binaryField.getFocalPoint();
		if (newFocalPoint != null) {
			S3HibBinary binary = graphS3BinaryField.getS3Binary();
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
		S3BinaryMetadata metaData = s3binaryField.getMetadata();
		if (metaData != null) {
			graphS3BinaryField.clearMetadata();
			for (Map.Entry<String, String> entry : metaData.getMap().entrySet()) {
				graphS3BinaryField.setMetadata(entry.getKey(), entry.getValue());
			}
			Location loc = metaData.getLocation();
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


		// Don't update image width, height, SHA checksum - those are immutable
	};
}
