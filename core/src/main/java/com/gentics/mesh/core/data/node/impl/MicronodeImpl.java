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

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.AbstractGraphFieldContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

public class MicronodeImpl extends AbstractGraphFieldContainerImpl implements Micronode {
	private static final Logger log = LoggerFactory.getLogger(MicronodeImpl.class);

	public static void checkIndices(Database database) {
		database.addVertexType(MicronodeImpl.class);
	}

	@Override
	public Observable<MicronodeResponse> transformToRestSync(InternalActionContext ac, String... languageTags) {
		List<Observable<MicronodeResponse>> obs = new ArrayList<>();
		MicronodeResponse restMicronode = new MicronodeResponse();
		MicroschemaContainerVersion microschemaContainer = getMicroschemaContainerVersion();
		if (microschemaContainer == null) {
			throw error(BAD_REQUEST, "The microschema container for micronode {" + getUuid() + "} could not be found.");
		}

		Microschema microschema = microschemaContainer.getSchema();
		if (microschema == null) {
			throw error(BAD_REQUEST, "The microschema for micronode {" + getUuid() + "} could not be found.");
		}

		// Microschema Reference
		restMicronode.setMicroschema(microschemaContainer.transformToReference());

		// Uuid
		restMicronode.setUuid(getUuid());

		List<String> requestedLanguageTags = new ArrayList<>();
		if (languageTags.length == 0) {
			requestedLanguageTags.addAll(ac.getSelectedLanguageTags());
		} else {
			requestedLanguageTags.addAll(Arrays.asList(languageTags));
		}

		// Fields
		for (FieldSchema fieldEntry : microschema.getFields()) {
			Observable<MicronodeResponse> obsRestField = getRestFieldFromGraph(ac, fieldEntry.getName(), fieldEntry, requestedLanguageTags)
					.map(restField -> {
				if (fieldEntry.isRequired() && restField == null) {
					/* TODO i18n */
					// TODO no trx fail. Instead let obsRestField fail
					throw error(BAD_REQUEST, "The field {" + fieldEntry.getName()
							+ "} is a required field but it could not be found in the micronode. Please add the field using an update call or change the field schema and remove the required flag.");
				}
				if (restField == null) {
					log.info("Field for key {" + fieldEntry.getName() + "} could not be found. Ignoring the field.");
				} else {
					restMicronode.getFields().put(fieldEntry.getName(), restField);
				}
				return restMicronode;
			});
			obs.add(obsRestField);
		}

		return Observable.merge(obs).last();
	}

	@Override
	public MicroschemaContainerVersion getMicroschemaContainerVersion() {
		return out(HAS_MICROSCHEMA_CONTAINER).has(MicroschemaContainerVersionImpl.class).nextOrDefaultExplicit(MicroschemaContainerVersionImpl.class, null);
	}

	@Override
	public void setMicroschemaContainerVersion(MicroschemaContainerVersion microschema) {
		setLinkOut(microschema.getImpl(), HAS_MICROSCHEMA_CONTAINER);
	}

	@Override
	public Microschema getMicroschema() {
		return getMicroschemaContainerVersion().getSchema();
	}

	@Override
	public void delete() {
		getElement().remove();
	}

	@Override
	public NodeGraphFieldContainer getContainer() {
		// TODO this only returns ONE container, but with versioning, the micronode may have multiple containers

		// first try to get the container in case for normal fields
		NodeGraphFieldContainerImpl container = in(HAS_FIELD).has(NodeGraphFieldContainerImpl.class)
				.nextOrDefaultExplicit(NodeGraphFieldContainerImpl.class, null);

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

		if (container == null) {
			throw error(BAD_REQUEST, "error_field_container_without_node");
		} else {
			return container.getParentNode();
		}
	}

	@Override
	public Observable<? extends Field> getRestFieldFromGraph(InternalActionContext ac, String fieldKey, FieldSchema fieldSchema,
			java.util.List<String> languageTags) {

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
				return super.getRestFieldFromGraph(ac, fieldKey, fieldSchema, languageTags);
			}
		default:
			return super.getRestFieldFromGraph(ac, fieldKey, fieldSchema, languageTags);
		}

	}

	@Override
	public void clone(Micronode micronode) {
		List<GraphField> otherFields = micronode.getFields(micronode.getMicroschemaContainerVersion().getSchema());

		for (GraphField graphField : otherFields) {
			graphField.cloneTo(this);
		}
	}

	@Override
	protected void updateField(InternalActionContext ac, String key, Field restField, FieldSchema fieldSchema, FieldSchemaContainer schema) {

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
				super.updateField(ac, key, restField, fieldSchema, schema);
			}
		default:
			super.updateField(ac, key, restField, fieldSchema, schema);
		}

	}

	@Override
	public void validate() {
		Microschema microschema = getMicroschemaContainerVersion().getSchema();
		Map<String, GraphField> fieldsMap = getFields(microschema).stream()
				.collect(Collectors.toMap(GraphField::getFieldKey, Function.identity()));

		microschema.getFields().stream().forEach(fieldSchema -> {
			GraphField field = fieldsMap.get(fieldSchema.getName());
			if (fieldSchema.isRequired() && field == null) {
				throw error(CONFLICT, "node_error_missing_mandatory_field_value", fieldSchema.getName(),
						microschema.getName());
			}
			if (field != null) {
				field.validate();
			}
		});
	}
}
