package com.gentics.mesh.core.data.node.field.nesting;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformator;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

public interface MicronodeGraphField extends ListableReferencingGraphField {

	static final Logger log = LoggerFactory.getLogger(MicronodeGraphField.class);

	FieldTransformator MICRONODE_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		MicronodeGraphField micronodeGraphField = container.getMicronode(fieldKey);
		if (micronodeGraphField == null) {
			return Observable.just(null);
		} else {
			return micronodeGraphField.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldUpdater MICRONODE_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		MicronodeGraphField micronodeGraphField = container.getMicronode(fieldKey);
		MicronodeFieldSchema microschemaFieldSchema = (MicronodeFieldSchema) fieldSchema;
		MicronodeField micronodeRestField = fieldMap.getMicronodeField(fieldKey);
		boolean isMicronodeFieldSetToNull = fieldMap.hasField(fieldKey) && micronodeRestField == null;
		GraphField.failOnDeletionOfRequiredField(micronodeGraphField, isMicronodeFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = micronodeRestField == null;
		GraphField.failOnMissingRequiredField(container.getMicronode(fieldKey), restIsNullOrEmpty, fieldSchema, fieldKey, schema);

		// Handle Deletion - Remove the field if the field has been explicitly set to null
		if (isMicronodeFieldSetToNull && micronodeGraphField != null) {
			micronodeGraphField.removeField(container);
			return;
		}

		// Rest model is empty or null - Abort
		if (micronodeRestField == null) {
			return;
		}

		MicroschemaReference microschemaReference = micronodeRestField.getMicroschema();
		MicroschemaContainerVersion microschemaContainerVersion = ac.getProject().getMicroschemaContainerRoot()
				.fromReference(microschemaReference, ac.getRelease(null)).toBlocking().single();

		Micronode micronode = null;

		// check whether microschema is allowed
		//TODO should we allow all microschemas if the list is empty?
		if (ArrayUtils.isEmpty(microschemaFieldSchema.getAllowedMicroSchemas())
				|| !Arrays.asList(microschemaFieldSchema.getAllowedMicroSchemas()).contains(microschemaContainerVersion.getName())) {
			log.error("Node update not allowed since the microschema {" + microschemaContainerVersion.getName()
					+ "} is now allowed. Allowed microschemas {" + microschemaFieldSchema.getAllowedMicroSchemas() + "}");
			throw error(BAD_REQUEST, "node_error_invalid_microschema_field_value", fieldKey, microschemaContainerVersion.getName());
		}

		// create a new micronode field, that will be filled
		micronodeGraphField = container.createMicronode(fieldKey, microschemaContainerVersion);
		micronode = micronodeGraphField.getMicronode();

		micronode.updateFieldsFromRest(ac, micronodeRestField.getFields());
	};

	FieldGetter MICRONODE_GETTER = (container, fieldSchema) -> {
		return container.getMicronode(fieldSchema.getName());
	};

	/**
	 * Returns the micronode for this field.
	 * 
	 * @return Micronode for this field when set, otherwise null.
	 */
	Micronode getMicronode();

	/**
	 * Transform the graph field into a rest field.
	 * 
	 * @param ac
	 * @param fieldKey
	 * @param languageTags
	 *            language tags
	 * @param level
	 *            Level of transformation
	 */
	Observable<? extends Field> transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level);
	
	@Override
	default List<FieldContainerChange> compareTo(GraphField field) {
		// TODO Auto-generated method stub
		return ListableReferencingGraphField.super.compareTo(field);
	}
}
