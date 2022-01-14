package com.gentics.mesh.core.data.node.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.MICROSCHEMA_VERSION_KEY_PROPERTY;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.madl.type.VertexTypeDefinition.vertexType;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.AbstractGraphFieldContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.madl.field.FieldType;
import com.gentics.mesh.madl.index.VertexIndexDefinition;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see Micronode
 */
public class MicronodeImpl extends AbstractGraphFieldContainerImpl implements Micronode {

	private static final Logger log = LoggerFactory.getLogger(MicronodeImpl.class);

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createType(vertexType(MicronodeImpl.class, MeshVertexImpl.class)
			.withField(MICROSCHEMA_VERSION_KEY_PROPERTY, FieldType.STRING));
		index.createIndex(VertexIndexDefinition.vertexIndex(MicronodeImpl.class)
			.withField(MICROSCHEMA_VERSION_KEY_PROPERTY, FieldType.STRING));
	}

	@Override
	public HibMicroschemaVersion getSchemaContainerVersion() {
		return db().index().findByUuid(MicroschemaContainerVersionImpl.class, property(MICROSCHEMA_VERSION_KEY_PROPERTY));
	}

	@Override
	public void setSchemaContainerVersion(HibFieldSchemaVersionElement version) {
		property(MICROSCHEMA_VERSION_KEY_PROPERTY, version.getUuid());
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
		NodeGraphFieldContainerImpl container = in(HAS_FIELD, NodeGraphFieldContainerImpl.class).nextOrNull();

		if (container == null) {
			// the micronode may be part of a list field
			container = in(HAS_ITEM).in(HAS_LIST).has(NodeGraphFieldContainerImpl.class).nextOrDefaultExplicit(NodeGraphFieldContainerImpl.class,
				null);
		}

		return container;
	}

	@Override
	public Result<? extends NodeGraphFieldContainer> getContainers() {
		// First try to get the container in case for normal fields
		Iterable<? extends NodeGraphFieldContainerImpl> containers = in(HAS_FIELD).frameExplicit(NodeGraphFieldContainerImpl.class);

		// The micronode may be part of a list field
		if (!containers.iterator().hasNext()) {
			containers = in(HAS_ITEM).in(HAS_LIST).has(NodeGraphFieldContainerImpl.class).frameExplicit(NodeGraphFieldContainerImpl.class);
		}

		return new TraversalResult<>(containers);
	}

	@Override
	public HibNode getNode() {
		return Micronode.super.getNode();
	}

	@Override
	public Field getRestField(InternalActionContext ac, String fieldKey, FieldSchema fieldSchema, java.util.List<String> languageTags,
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
				return super.getRestField(ac, fieldKey, fieldSchema, languageTags, level);
			}
		default:
			return super.getRestField(ac, fieldKey, fieldSchema, languageTags, level);
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
	public boolean equals(Object obj) {
		return micronodeEquals(obj);
	}
}
