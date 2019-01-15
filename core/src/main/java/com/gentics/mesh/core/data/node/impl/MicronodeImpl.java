package com.gentics.mesh.core.data.node.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_CONTAINER;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.AbstractGraphFieldContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.util.CompareUtils;
import com.gentics.mesh.util.ETag;

import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MicronodeImpl extends AbstractGraphFieldContainerImpl implements Micronode {
	private static final Logger log = LoggerFactory.getLogger(MicronodeImpl.class);

	public static void init(Database database) {
		database.addVertexType(MicronodeImpl.class, MeshVertexImpl.class);
	}

	@Override
	public MicronodeResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {

		NodeParametersImpl parameters = new NodeParametersImpl(ac);
		MicronodeResponse restMicronode = new MicronodeResponse();
		MicroschemaContainerVersion microschemaContainer = getSchemaContainerVersion();
		if (microschemaContainer == null) {
			throw error(BAD_REQUEST, "The microschema container for micronode {" + getUuid() + "} could not be found.");
		}

		Microschema microschema = microschemaContainer.getSchema();
		if (microschema == null) {
			throw error(BAD_REQUEST, "The microschema for micronode {" + getUuid() + "} could not be found.");
		}

		restMicronode.setMicroschema(microschemaContainer.transformToReference());
		restMicronode.setUuid(getUuid());

		List<String> requestedLanguageTags = new ArrayList<>();
		if (languageTags.length == 0) {
			requestedLanguageTags.addAll(parameters.getLanguageList());
		} else {
			requestedLanguageTags.addAll(Arrays.asList(languageTags));
		}

		// Fields
		for (FieldSchema fieldEntry : microschema.getFields()) {
			Field restField = getRestFieldFromGraph(ac, fieldEntry.getName(), fieldEntry, requestedLanguageTags, level);
			if (restField != null) {
				restMicronode.getFields().put(fieldEntry.getName(), restField);
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Field for key {" + fieldEntry.getName() + "} could not be found. Ignoring the field.");
				}
			}
		}

		return restMicronode;
	}

	@Override
	public MicroschemaContainerVersion getSchemaContainerVersion() {
		return out(HAS_MICROSCHEMA_CONTAINER).has(MicroschemaContainerVersionImpl.class).nextOrDefaultExplicit(MicroschemaContainerVersionImpl.class,
				null);
	}

	@Override
	public void setSchemaContainerVersion(GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> version) {
		setLinkOut(version, HAS_MICROSCHEMA_CONTAINER);
	}

	@Override
	public void delete(BulkActionContext bac) {
		super.delete(bac);
		getElement().remove();
	}

	@Override
	public NodeGraphFieldContainer getContainer() {
		// TODO this only returns ONE container, but with versioning, the micronode may have multiple containers

		// first try to get the container in case for normal fields
		NodeGraphFieldContainerImpl container = in(HAS_FIELD).nextOrDefaultExplicit(NodeGraphFieldContainerImpl.class, null);

		if (container == null) {
			// the micronode may be part of a list field
			container = in(HAS_ITEM).in(HAS_LIST).has(NodeGraphFieldContainerImpl.class).nextOrDefaultExplicit(NodeGraphFieldContainerImpl.class,
					null);
		}

		return container;
	}

	@Override
	public Node getParentNode() {
		NodeGraphFieldContainer container = getContainer();
		while (container.getPreviousVersion() != null) {
			container = container.getPreviousVersion();
		}

		return container.getParentNode();
	}

	/**
	 * Returns the language of the container, since the micronode itself does not have an edge to the language
	 */
	@Override
	public String getLanguageTag() {
		return getContainer().getLanguageTag();
	}

	@Override
	public Field getRestFieldFromGraph(InternalActionContext ac, String fieldKey, FieldSchema fieldSchema, java.util.List<String> languageTags,
			int level) {

		// Filter out unsupported field types
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
		switch (type) {
		case BINARY:
		case MICRONODE:
			throw error(BAD_REQUEST, "error_unsupported_fieldtype", type.name());
		case LIST:
			ListFieldSchema listFieldSchema = (ListFieldSchema) fieldSchema;
			switch (listFieldSchema.getListType()) {
			case MicronodeGraphFieldList.TYPE:
				throw error(BAD_REQUEST, "error_unsupported_fieldtype", type + ":" + listFieldSchema.getListType());
			default:
				return super.getRestFieldFromGraph(ac, fieldKey, fieldSchema, languageTags, level);
			}
		default:
			return super.getRestFieldFromGraph(ac, fieldKey, fieldSchema, languageTags, level);
		}

	}

	@Override
	public void clone(Micronode micronode) {
		List<GraphField> otherFields = micronode.getFields();

		for (GraphField graphField : otherFields) {
			graphField.cloneTo(this);
		}
	}

	@Override
	protected void updateField(InternalActionContext ac, FieldMap fieldMap, String key, FieldSchema fieldSchema, FieldSchemaContainer schema) {

		// Filter out unsupported field types
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
		switch (type) {
		case BINARY:
		case MICRONODE:
			throw error(BAD_REQUEST, "error_unsupported_fieldtype", type.name());
		case LIST:
			ListFieldSchema listFieldSchema = (ListFieldSchema) fieldSchema;
			switch (listFieldSchema.getListType()) {
			case MicronodeGraphFieldList.TYPE:
				throw error(BAD_REQUEST, "error_unsupported_fieldtype", type + ":" + listFieldSchema.getListType());
			default:
				super.updateField(ac, fieldMap, key, fieldSchema, schema);
			}
		default:
			super.updateField(ac, fieldMap, key, fieldSchema, schema);
		}

	}

	@Override
	public void validate() {
		Microschema microschema = getSchemaContainerVersion().getSchema();
		Map<String, GraphField> fieldsMap = getFields().stream().collect(Collectors.toMap(GraphField::getFieldKey, Function.identity()));

		microschema.getFields().stream().forEach(fieldSchema -> {
			GraphField field = fieldsMap.get(fieldSchema.getName());
			if (fieldSchema.isRequired() && field == null) {
				throw error(CONFLICT, "node_error_missing_mandatory_field_value", fieldSchema.getName(), microschema.getName());
			}
			if (field != null) {
				field.validate();
			}
		});
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Micronode) {
			Micronode micronode = (Micronode) obj;
			List<GraphField> fieldsA = getFields();
			List<GraphField> fieldsB = micronode.getFields();
			return CompareUtils.equals(fieldsA, fieldsB);
		}
		if (obj instanceof MicronodeField) {
			MicronodeField restMicronode = (MicronodeField) obj;
			Microschema schema = getSchemaContainerVersion().getSchema();
			// Iterate over all field schemas and compare rest and graph with eachother
			for (FieldSchema fieldSchema : schema.getFields()) {
				GraphField graphField = getField(fieldSchema);
				Field restField = restMicronode.getFields().getField(fieldSchema.getName(), fieldSchema);
				if (!CompareUtils.equals(graphField, restField)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public List<FieldContainerChange> compareTo(Micronode micronode) {
		List<FieldContainerChange> changes = new ArrayList<>();
		for (FieldSchema fieldSchema : getSchemaContainerVersion().getSchema().getFields()) {
			GraphField fieldA = getField(fieldSchema);
			GraphField fieldB = micronode.getField(fieldSchema);
			if (!CompareUtils.equals(fieldA, fieldB)) {
				changes.add(new FieldContainerChange(fieldSchema.getName(), FieldChangeTypes.UPDATED));
			}
		}
		return changes;
	}

	@Override
	public String getETag(InternalActionContext ac) {
		// TODO check whether the uuid remains static for micronode updates
		return ETag.hash(getUuid());
	}

	public String getAPIPath(InternalActionContext ac) {
		// Micronodes have no public location
		return null;
	}

	@Override
	public Single<MicronodeResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return MeshInternal.get().database().asyncTx(() -> {
			return Single.just(transformToRestSync(ac, level, languageTags));
		});
	}
}
