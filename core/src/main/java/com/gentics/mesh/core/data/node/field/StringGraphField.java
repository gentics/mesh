package com.gentics.mesh.core.data.node.field;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Arrays;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.parameter.impl.LinkType;

import rx.Single;

/**
 * The StringField Domain Model interface.
 */
public interface StringGraphField extends ListableGraphField, BasicGraphField<StringField> {

	FieldTransformator STRING_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		// TODO validate found fields has same type as schema
		// StringGraphField graphStringField = new com.gentics.mesh.core.data.node.field.impl.basic.StringGraphFieldImpl(
		// fieldKey, this);
		StringGraphField graphStringField = container.getString(fieldKey);
		if (graphStringField == null) {
			return Single.just(null);
		} else {
			return graphStringField.transformToRest(ac).map(stringField -> {
				if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {
					Project project = ac.getProject();
					if (project == null) {
						project = parentNode.getProject();
					}
					stringField.setString(WebRootLinkReplacer.getInstance().replace(ac.getRelease(null).getUuid(),
							ContainerType.forVersion(ac.getVersioningParameters().getVersion()), stringField.getString(),
							ac.getNodeParameters().getResolveLinks(), project.getName(), languageTags));
				}
				return stringField;
			});
		}
	};

	FieldUpdater STRING_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		StringField stringField = fieldMap.getStringField(fieldKey);
		StringGraphField graphStringField = container.getString(fieldKey);
		boolean isStringFieldSetToNull = fieldMap.hasField(fieldKey) && (stringField == null || stringField.getString() == null);
		GraphField.failOnDeletionOfRequiredField(graphStringField, isStringFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = stringField == null || stringField.getString() == null;
		GraphField.failOnMissingRequiredField(graphStringField, restIsNullOrEmpty, fieldSchema, fieldKey, schema);

		// Handle Deletion
		if (isStringFieldSetToNull && graphStringField != null) {
			graphStringField.removeField(container);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNullOrEmpty) {
			return;
		}

		// check value restrictions
		StringFieldSchema stringFieldSchema = (StringFieldSchema) fieldSchema;
		if (stringFieldSchema.getAllowedValues() != null) {
			if (stringField.getString() != null && !Arrays.asList(stringFieldSchema.getAllowedValues()).contains(stringField.getString())) {
				throw error(BAD_REQUEST, "node_error_invalid_string_field_value", fieldKey, stringField.getString());
			}
		}

		// Handle Update / Create
		if (graphStringField == null) {
			graphStringField = container.createString(fieldKey);
		}
		graphStringField.setString(stringField.getString());

	};

	FieldGetter STRING_GETTER = (container, fieldSchema) -> {
		return container.getString(fieldSchema.getName());
	};

	/**
	 * Return the graph string value.
	 * 
	 * @return
	 */
	String getString();

	/**
	 * Set the string graph field value.
	 * 
	 * @param string
	 */
	void setString(String string);

}
