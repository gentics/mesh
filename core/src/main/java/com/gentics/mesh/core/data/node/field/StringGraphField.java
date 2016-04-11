package com.gentics.mesh.core.data.node.field;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Arrays;

import org.aspectj.weaver.ast.FieldGet;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;

import rx.Observable;

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
			return Observable.just(new StringFieldImpl());
		} else {
			return graphStringField.transformToRest(ac).map(stringField -> {
				if (ac.getResolveLinksType() != WebRootLinkReplacer.Type.OFF) {
					Project project = ac.getProject();
					if (project == null) {
						project = parentNode.getProject();
					}
					stringField.setString(WebRootLinkReplacer.getInstance().replace(stringField.getString(), ac.getResolveLinksType(),
							project.getName(), languageTags));
				}
				return stringField;
			});
		}
	};

	FieldUpdater STRING_UPDATER = (container, ac, fieldKey, restField, fieldSchema, schema) -> {
		StringGraphField graphStringField = container.getString(fieldKey);
		GraphField.failOnMissingMandatoryField(ac, graphStringField, restField, fieldSchema, fieldKey, schema);
		StringField stringField = (StringFieldImpl) restField;
		if (restField == null) {
			return;
		}

		// check value restrictions
		StringFieldSchema stringFieldSchema = (StringFieldSchema) fieldSchema;
		if (stringFieldSchema.getAllowedValues() != null) {
			if (stringField.getString() != null && !Arrays.asList(stringFieldSchema.getAllowedValues()).contains(stringField.getString())) {
				throw error(BAD_REQUEST, "node_error_invalid_string_field_value", fieldKey, stringField.getString());
			}
		}

		// Create new graph field if no existing one could be found
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
