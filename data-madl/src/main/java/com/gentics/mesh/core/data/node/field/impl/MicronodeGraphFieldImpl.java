package com.gentics.mesh.core.data.node.field.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformer;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.util.CompareUtils;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * See {@link MicronodeGraphField}
 */
public class MicronodeGraphFieldImpl extends MeshEdgeImpl implements MicronodeGraphField {

	private static final Logger log = LoggerFactory.getLogger(MicronodeGraphFieldImpl.class);

	public static FieldTransformer<MicronodeField> MICRONODE_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
		parentNode) -> {
		MicronodeGraphField micronodeGraphField = container.getMicronode(fieldKey);
		if (micronodeGraphField == null) {
			return null;
		} else {
			return micronodeGraphField.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	public static FieldUpdater MICRONODE_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		MicronodeGraphField micronodeGraphField = container.getMicronode(fieldKey);
		MicronodeFieldSchema microschemaFieldSchema = (MicronodeFieldSchema) fieldSchema;
		MicronodeField micronodeRestField = fieldMap.getMicronodeField(fieldKey);
		boolean isMicronodeFieldSetToNull = fieldMap.hasField(fieldKey) && micronodeRestField == null;
		GraphField.failOnDeletionOfRequiredField(micronodeGraphField, isMicronodeFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = micronodeRestField == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			GraphField.failOnMissingRequiredField(container.getMicronode(fieldKey), restIsNullOrEmpty, fieldSchema, fieldKey, schema);
		}

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
		if (microschemaReference == null || !microschemaReference.isSet()) {
			throw error(BAD_REQUEST, "micronode_error_missing_reference", fieldKey);
		}

		MicroschemaContainerVersion microschemaContainerVersion = ac.getProject().getMicroschemaContainerRoot().fromReference(microschemaReference,
			ac.getBranch());

		Micronode micronode = null;

		// check whether microschema is allowed
		if (!ArrayUtils.isEmpty(microschemaFieldSchema.getAllowedMicroSchemas())
			&& !Arrays.asList(microschemaFieldSchema.getAllowedMicroSchemas()).contains(microschemaContainerVersion.getName())) {
			log.error("Node update not allowed since the microschema {" + microschemaContainerVersion.getName()
				+ "} is now allowed. Allowed microschemas {" + Arrays.toString(microschemaFieldSchema.getAllowedMicroSchemas()) + "}");
			throw error(BAD_REQUEST, "node_error_invalid_microschema_field_value", fieldKey, microschemaContainerVersion.getName());
		}

		// Always create a new micronode field since each update must create a new field instance. The old field must be detached from the given container.
		micronodeGraphField = container.createMicronode(fieldKey, microschemaContainerVersion);
		micronode = micronodeGraphField.getMicronode();

		micronode.updateFieldsFromRest(ac, micronodeRestField.getFields());
	};

	public static FieldGetter MICRONODE_GETTER = (container, fieldSchema) -> {
		return container.getMicronode(fieldSchema.getName());
	};

	public static void init(TypeHandler type, IndexHandler index) {
		type.createType(edgeType(MicronodeGraphFieldImpl.class.getSimpleName()));
		type.createType(edgeType(HAS_FIELD).withSuperClazz(MicronodeGraphFieldImpl.class));
	}

	@Override
	public void setFieldKey(String key) {
		setProperty(GraphField.FIELD_KEY_PROPERTY_KEY, key);
	}

	@Override
	public String getFieldKey() {
		return getProperty(GraphField.FIELD_KEY_PROPERTY_KEY);
	}

	@Override
	public Micronode getMicronode() {
		return inV().has(MicronodeImpl.class).nextOrDefaultExplicit(MicronodeImpl.class, null);
	}

	@Override
	public MicronodeField transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		Micronode micronode = getMicronode();
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
	public void removeField(BulkActionContext bac, GraphFieldContainer container) {
		Micronode micronode = getMicronode();
		// Remove the edge to get rid of the reference
		remove();
		if (micronode != null) {
			// Remove the micronode if this was the last edge to the micronode
			if (!micronode.in(HAS_FIELD).hasNext()) {
				micronode.delete(bac);
			}
		}
	}

	@Override
	public GraphField cloneTo(GraphFieldContainer container) {
		Micronode micronode = getMicronode();

		MicronodeGraphField field = getGraph().addFramedEdge(container, micronode, HAS_FIELD, MicronodeGraphFieldImpl.class);
		field.setFieldKey(getFieldKey());
		return field;
	}

	@Override
	public void validate() {
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
	public List<FieldContainerChange> compareTo(Object field) {
		if (field instanceof MicronodeGraphField) {
			Micronode micronodeA = getMicronode();
			Micronode micronodeB = ((MicronodeGraphField) field).getMicronode();
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
			Micronode micronodeA = getMicronode();
			MicronodeField micronodeB = ((MicronodeField) field);
			// Load each field using the field schema
			Microschema schema = micronodeA.getSchemaContainerVersion().getSchema();
			for (FieldSchema fieldSchema : schema.getFields()) {
				GraphField graphField = micronodeA.getField(fieldSchema);
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

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MicronodeGraphField) {
			Micronode micronodeA = getMicronode();
			Micronode micronodeB = ((MicronodeGraphField) obj).getMicronode();
			return CompareUtils.equals(micronodeA, micronodeB);
		}
		if (obj instanceof MicronodeField) {
			Micronode micronodeA = getMicronode();
			MicronodeField micronodeB = ((MicronodeField) obj);

			// Load each field using the field schema
			Microschema schema = micronodeA.getSchemaContainerVersion().getSchema();
			for (FieldSchema fieldSchema : schema.getFields()) {
				GraphField graphField = micronodeA.getField(fieldSchema);
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
