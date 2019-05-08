package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CHANGE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.ELASTICSEARCH_KEY;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.util.Tuple;

import io.vertx.core.json.JsonObject;

/**
 * @see SchemaChange
 */
public abstract class AbstractSchemaChange<T extends FieldSchemaContainer> extends MeshVertexImpl implements SchemaChange<T> {

	private static String MIGRATION_SCRIPT_PROPERTY_KEY = "migrationScript";

	public static final String REST_PROPERTY_PREFIX_KEY = "fieldProperty_";

	@Override
	public SchemaChange<?> getNextChange() {
		return (SchemaChange) out(HAS_CHANGE).nextOrDefault(null);
	}

	@Override
	public SchemaChange<T> setNextChange(SchemaChange<?> change) {
		setUniqueLinkOutTo(change, HAS_CHANGE);
		return this;
	}

	@Override
	public SchemaChange<?> getPreviousChange() {
		return (SchemaChange) in(HAS_CHANGE).nextOrDefault(null);
	}

	@Override
	public SchemaChange<T> setPreviousChange(SchemaChange<?> change) {
		setUniqueLinkInTo(change, HAS_CHANGE);
		return this;
	}

	@Override
	abstract public SchemaChangeOperation getOperation();

	@Override
	public <R extends GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?>> R getPreviousContainerVersion() {
		return (R) in(HAS_SCHEMA_CONTAINER).nextOrDefault(null);
	}

	@Override
	public SchemaChange<T> setPreviousContainerVersion(GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> containerVersion) {
		setSingleLinkInTo(containerVersion, HAS_SCHEMA_CONTAINER);
		return this;
	}

	@Override
	public <R extends GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?>> R getNextContainerVersion() {
		return (R) out(HAS_SCHEMA_CONTAINER).nextOrDefault(null);
	}

	@Override
	public SchemaChange<T> setNextSchemaContainerVersion(GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> containerVersion) {
		setSingleLinkOutTo(containerVersion, HAS_SCHEMA_CONTAINER);
		return this;
	}

	@Override
	public void setRestProperty(String key, Object value) {
		if (value instanceof List) {
			value = ((List) value).toArray();
		}
		if (value instanceof JsonObject) {
			value = ((JsonObject) value).encode();
		}
		property(REST_PROPERTY_PREFIX_KEY + key, value);
	}

	@Override
	public <R> R getRestProperty(String key) {
		return property(REST_PROPERTY_PREFIX_KEY + key);
	}

	@Override
	public <R> Map<String, R> getRestProperties() {
		return getProperties(REST_PROPERTY_PREFIX_KEY);
	}

	@Override
	public JsonObject getIndexOptions() {
		Object obj = getRestProperty(ELASTICSEARCH_KEY);
		if (obj != null) {
			if (obj instanceof String) {
				return new JsonObject((String) obj);
			} else if (obj instanceof JsonObject) {
				return (JsonObject) obj;
			} else {
				throw error(INTERNAL_SERVER_ERROR, "Type was not expected {" + obj.getClass().getName() + "}");
			}
		}
		return null;
	}

	@Override
	public void setIndexOptions(JsonObject options) {
		setRestProperty(ELASTICSEARCH_KEY, options.encode());
	}

	@Override
	public void updateFromRest(SchemaChangeModel restChange) {
		for (String key : restChange.getProperties().keySet()) {
			setRestProperty(key, restChange.getProperties().get(key));
		}
	}

	@Override
	public SchemaChangeModel transformToRest() throws IOException {
		SchemaChangeModel model = new SchemaChangeModel();
		// Strip away the prefix
		for (String key : getRestProperties().keySet()) {
			Object value = getRestProperties().get(key);
			key = key.replace(REST_PROPERTY_PREFIX_KEY, "");
			model.getProperties().put(key, value);
		}
		model.setOperation(getOperation());
		model.setUuid(getUuid());
		return model;
	}

}
