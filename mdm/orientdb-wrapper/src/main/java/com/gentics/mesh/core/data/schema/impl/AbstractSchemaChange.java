package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CHANGE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.ELASTICSEARCH_KEY;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibSchemaChange;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

import io.vertx.core.json.JsonObject;

/**
 * @see SchemaChange
 */
public abstract class AbstractSchemaChange<T extends FieldSchemaContainer> extends MeshVertexImpl implements SchemaChange<T> {

	@Override
	public HibSchemaChange<?> getNextChange() {
		return (SchemaChange) out(HAS_CHANGE).nextOrDefault(null);
	}

	@Override
	public HibSchemaChange<T> setNextChange(HibSchemaChange<?> change) {
		setUniqueLinkOutTo(toGraph(change), HAS_CHANGE);
		return this;
	}

	@Override
	public SchemaChange<T> getPreviousChange() {
		return (SchemaChange) in(HAS_CHANGE).nextOrDefault(null);
	}

	@Override
	public SchemaChange<T> setPreviousChange(HibSchemaChange<?> change) {
		setUniqueLinkInTo(toGraph(change), HAS_CHANGE);
		return this;
	}

	@Override
	public <R extends HibFieldSchemaVersionElement<?, ?, ?, ?, ?>> R getPreviousContainerVersion() {
		return (R) in(HAS_SCHEMA_CONTAINER).nextOrDefault(null);
	}

	@Override
	public SchemaChange<T> setPreviousContainerVersion(HibFieldSchemaVersionElement<?, ?, ?, ?, ?> containerVersion) {
		setSingleLinkInTo(toGraph(containerVersion), HAS_SCHEMA_CONTAINER);
		return this;
	}

	@Override
	public <R extends HibFieldSchemaVersionElement<?, ?, ?, ?, ?>> R getNextContainerVersion() {
		return (R) out(HAS_SCHEMA_CONTAINER).nextOrDefault(null);
	}

	@Override
	public HibSchemaChange<T> setNextSchemaContainerVersion(HibFieldSchemaVersionElement<?, ?, ?, ?, ?> containerVersion) {
		setSingleLinkOutTo(toGraph(containerVersion), HAS_SCHEMA_CONTAINER);
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
		Map<String, R> rawMap = getProperties(REST_PROPERTY_PREFIX_KEY);
		return rawMap.entrySet().stream()
				.collect(Collectors.toMap(
						entry -> entry.getKey().replace(REST_PROPERTY_PREFIX_KEY, StringUtils.EMPTY), 
						entry -> entry.getValue()));
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

	@Override
	public void delete(BulkActionContext bc) {
		HibSchemaChange<?> next = getNextChange();
		if (next != null) {
			toGraph(next).delete(bc);
		}
		getElement().remove();
	}

}
