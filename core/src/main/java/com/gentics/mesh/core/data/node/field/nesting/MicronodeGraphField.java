package com.gentics.mesh.core.data.node.field.nesting;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.field.FieldTransformator;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.rest.micronode.NullMicronodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

import rx.Observable;

public interface MicronodeGraphField extends ListableReferencingGraphField {

	FieldTransformator MICRONODE_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		MicronodeGraphField micronodeGraphField = container.getMicronode(fieldKey);
		if (micronodeGraphField == null) {
			return Observable.just(new NullMicronodeResponse());
		} else {
			return micronodeGraphField.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldUpdater MICRONODE_UPDATER = (container, ac, fieldKey, restField, fieldSchema, schema) -> {
		BootstrapInitializer boot = BootstrapInitializer.getBoot();
		MicronodeFieldSchema microschemaFieldSchema = (MicronodeFieldSchema) fieldSchema;
		GraphField.failOnMissingMandatoryField(ac, container.getMicronode(fieldKey), restField, fieldSchema, fieldKey, schema);
		if (restField == null) {
			return;
		}
		MicronodeField micronodeRestField = (MicronodeField) restField;
		MicroschemaReference microschemaReference = micronodeRestField.getMicroschema();
		// TODO check for null
		if (microschemaReference == null) {
			return;
		}
		String microschemaName = microschemaReference.getName();
		String microschemaUuid = microschemaReference.getUuid();
		MicroschemaContainer microschemaContainer = null;

		if (isEmpty(microschemaName) && isEmpty(microschemaUuid)) {
			//TODO i18n
			throw error(BAD_REQUEST, "No valid microschema reference could be found for field {" + fieldKey + "}");
		}
		// 1. Load microschema by uuid
		if (isEmpty(microschemaUuid)) {
			microschemaContainer = boot.microschemaContainerRoot().findByUuid(microschemaUuid).toBlocking().single();
		}
		// 2. Load microschema by name
		if (microschemaContainer == null && !isEmpty(microschemaName)) {
			microschemaContainer = boot.microschemaContainerRoot().findByName(microschemaName).toBlocking().single();
		}

		if (microschemaContainer == null) {
			throw error(BAD_REQUEST, "microschema_reference_invalid", fieldKey);
		}

		//TODO versioning: Use released schema version instead of latest
		MicroschemaContainerVersion microschemaContainerVersion = microschemaContainer.getLatestVersion();
		Micronode micronode = null;
		MicronodeGraphField micronodeGraphField = container.getMicronode(fieldKey);

		// check whether microschema is allowed
		if (ArrayUtils.isEmpty(microschemaFieldSchema.getAllowedMicroSchemas())
				|| !Arrays.asList(microschemaFieldSchema.getAllowedMicroSchemas()).contains(microschemaContainer.getName())) {
			throw error(BAD_REQUEST, "node_error_invalid_microschema_field_value", fieldKey, microschemaContainer.getName());
		}

		// create a new micronode field, that will be filled
		micronodeGraphField = container.createMicronode(fieldKey, microschemaContainerVersion);
		micronode = micronodeGraphField.getMicronode();

		micronode.updateFieldsFromRest(ac, micronodeRestField.getFields(), micronode.getMicroschema());
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
}
