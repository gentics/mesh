package com.gentics.mesh.core.data.node.field.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Arrays;
import java.util.Objects;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.field.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformer;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.LinkType;
import com.syncleus.ferma.AbstractVertexFrame;

public class StringGraphFieldImpl extends AbstractBasicField<StringField> implements StringGraphField {

	public static FieldTransformer<StringField> STRING_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		MeshComponent mesh = container.getGraph().getAttribute("meshComponent");
		// TODO validate found fields has same type as schema
		// StringGraphField graphStringField = new com.gentics.mesh.core.data.node.field.impl.basic.StringGraphFieldImpl(
		// fieldKey, this);
		StringGraphField graphStringField = container.getString(fieldKey);
		if (graphStringField == null) {
			return null;
		} else {
			StringField field = graphStringField.transformToRest(ac);
			if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {
				Project project = ac.getProject();
				if (project == null) {
					project = parentNode.get().getProject();
				}
				field.setString(mesh.webRootLinkReplacer().replace(ac, ac.getBranch().getUuid(),
						ContainerType.forVersion(ac.getVersioningParameters().getVersion()), field.getString(),
						ac.getNodeParameters().getResolveLinks(), project.getName(), languageTags));
			}
			return field;

		}
	};

	public static FieldUpdater STRING_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		StringField stringField = fieldMap.getStringField(fieldKey);
		StringGraphField graphStringField = container.getString(fieldKey);
		boolean isStringFieldSetToNull = fieldMap.hasField(fieldKey) && (stringField == null || stringField.getString() == null);
		GraphField.failOnDeletionOfRequiredField(graphStringField, isStringFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = stringField == null || stringField.getString() == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			GraphField.failOnMissingRequiredField(graphStringField, restIsNullOrEmpty, fieldSchema, fieldKey, schema);
		}

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

	public static FieldGetter STRING_GETTER = (container, fieldSchema) -> {
		return container.getString(fieldSchema.getName());
	};

	public StringGraphFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}

	@Override
	public void setString(String string) {
		setFieldProperty("string", string);
	}

	@Override
	public String getString() {
		return getFieldProperty("string");
	}

	@Override
	public String getDisplayName() {
		return getString();
	}

	@Override
	public StringField transformToRest(ActionContext ac) {
		StringFieldImpl stringField = new StringFieldImpl();
		String text = getString();
		stringField.setString(text == null ? "" : text);
		return stringField;
	}

	@Override
	public void removeField(BulkActionContext bac, GraphFieldContainer container) {
		setFieldProperty("string", null);
		setFieldKey(null);
	}

	@Override
	public GraphField cloneTo(GraphFieldContainer container) {
		StringGraphField clone = container.createString(getFieldKey());
		clone.setString(getString());
		return clone;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof StringGraphField) {
			String valueA = getString();
			String valueB = ((StringGraphField) obj).getString();
			return Objects.equals(valueA, valueB);
		}
		if (obj instanceof StringField) {
			String valueA = getString();
			String valueB = ((StringField) obj).getString();
			return Objects.equals(valueA, valueB);
		}
		return false;
	}
}
