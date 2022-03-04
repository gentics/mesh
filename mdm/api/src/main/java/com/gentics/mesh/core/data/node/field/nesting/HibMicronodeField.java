package com.gentics.mesh.core.data.node.field.nesting;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.util.CompareUtils;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * A {@link MicronodeGraphField} is an {@link MeshEdge} which links a {@link GraphFieldContainer} to a {@link Micronode} vertex.
 */
public interface HibMicronodeField extends HibListableField {

	Logger log = LoggerFactory.getLogger(HibMicronodeField.class);

	/**
	 * Returns the micronode for this field.
	 * 
	 * @return Micronode for this field when set, otherwise null.
	 */
	HibMicronode getMicronode();

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
	default MicronodeField transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		HibMicronode micronode = getMicronode();
		if (micronode == null) {
			// TODO is this correct?
			throw error(BAD_REQUEST, "error_name_must_be_set");
		} else {
			if (languageTags != null) {
				return micronode.transformToRestSync(ac, level, languageTags.toArray(new String[languageTags.size()]));
			} else {
				return micronode.transformToRestSync(ac, level);
			}
		}
	}

	@Override
	default void validate() {
		getMicronode().validate();
	}

	/**
	 * Override the default implementation since micronode graph fields are container for other fields. We also want to catch the nested fields.
	 * 
	 * @param field
	 *            Field to compare against
	 * @return List of detected changes
	 */
	@Override
	default List<FieldContainerChange> compareTo(Object field) {
		if (field instanceof HibMicronodeField) {
			HibMicronode micronodeA = getMicronode();
			HibMicronode micronodeB = ((HibMicronodeField) field).getMicronode();
			List<FieldContainerChange> changes = micronodeA.compareTo(micronodeB);
			// Update the detected changes and prepend the fieldkey of the micronode in order to be able to identify nested changes more easy.
			changes.stream().forEach(c -> {
				c.setFieldCoordinates(getFieldKey() + "." + c.getFieldKey());
				// Reset the field key
				c.setFieldKey(getFieldKey());
			});
			return changes;
		}
		if (field instanceof MicronodeField) {
			List<FieldContainerChange> changes = new ArrayList<>();
			HibMicronode micronodeA = getMicronode();
			MicronodeField micronodeB = ((MicronodeField) field);
			// Load each field using the field schema
			MicroschemaModel schema = micronodeA.getSchemaContainerVersion().getSchema();
			for (FieldSchema fieldSchema : schema.getFields()) {
				HibField graphField = micronodeA.getField(fieldSchema);
				try {
					Field nestedRestField = micronodeB.getFields().getField(fieldSchema.getName(), fieldSchema);
					// If possible compare the graph field with the rest field
					if (graphField != null && graphField.equals(nestedRestField)) {
						continue;
					}
					// Field is not part of the request and has not been set to null. Skip it.
					if (nestedRestField == null && !micronodeB.getFields().hasField(fieldSchema.getName())) {
						continue;
					}
					if (!CompareUtils.equals(graphField, nestedRestField)) {
						FieldContainerChange change = new FieldContainerChange(getFieldKey(), FieldChangeTypes.UPDATED);
						// Set the micronode specific field coordinates
						change.setFieldCoordinates(getFieldKey() + "." + fieldSchema.getName());
						changes.add(change);

					}
				} catch (Exception e) {
					// TODO i18n
					throw error(INTERNAL_SERVER_ERROR, "Can't load rest field {" + fieldSchema.getName() + "} from micronode {" + getFieldKey() + "}",
						e);
				}
			}
			return changes;

		}
		return Collections.emptyList();

	}

	/**
	 * A default method cannot override a method from java.lang.Object.
	 * This is the common equality check implementation, that has to be reused by the HibMicronodeField implementors.
	 * 
	 * @param obj
	 * @return
	 */
	default boolean micronodeFieldEquals(Object obj) {
		if (obj instanceof HibMicronodeField) {
			HibMicronode micronodeA = getMicronode();
			HibMicronode micronodeB = ((HibMicronodeField) obj).getMicronode();
			return CompareUtils.equals(micronodeA, micronodeB);
		}
		if (obj instanceof MicronodeField) {
			HibMicronode micronodeA = getMicronode();
			MicronodeField micronodeB = ((MicronodeField) obj);

			// Load each field using the field schema
			MicroschemaModel schema = micronodeA.getSchemaContainerVersion().getSchema();
			for (FieldSchema fieldSchema : schema.getFields()) {
				HibField graphField = micronodeA.getField(fieldSchema);
				try {
					Field nestedRestField = micronodeB.getFields().getField(fieldSchema.getName(), fieldSchema);
					// If possible compare the graph field with the rest field
					if (graphField != null && graphField.equals(nestedRestField)) {
						continue;
					}
					if (!CompareUtils.equals(graphField, nestedRestField)) {
						return false;
					}
				} catch (Exception e) {
					log.error("Could not load rest field", e);
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}
}
