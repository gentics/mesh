package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PARENT_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_VERSION;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraphContainer;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.schema.HibFieldSchemaElement;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibSchemaChange;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * Abstract implementation for a graph field container version.
 * 
 * @param <R>
 *            Rest model type
 * @param <RE>
 *            Reference model type
 * @param <SCV>
 *            Schema container version type
 * @param <SC>
 *            Schema container type
 */
public abstract class AbstractGraphFieldSchemaContainerVersion<
			R extends FieldSchemaContainer, 
			RM extends FieldSchemaContainerVersion, 
			RE extends NameUuidReference<RE>, 
			SCV extends HibFieldSchemaVersionElement<R, RM, RE, SC, SCV>, 
			SC extends HibFieldSchemaElement<R, RM, RE, SC, SCV>
	> extends AbstractMeshCoreVertex<R> implements GraphFieldSchemaContainerVersion<R, RM, RE, SCV, SC>, HibFieldSchemaVersionElement<R, RM, RE, SC, SCV> {

	@Override
	public void setName(String name) {
		property("name", name);
	}

	@Override
	public String getName() {
		return property("name");
	}

	@Override
	public String getVersion() {
		return property(VERSION_PROPERTY_KEY);
	}

	@Override
	public void setVersion(String version) {
		property(VERSION_PROPERTY_KEY, version);
	}

	@Override
	public SchemaChange<?> getNextChange() {
		return (SchemaChange<?>) out(HAS_SCHEMA_CONTAINER).nextOrDefault(null);
	}

	@Override
	public void setNextChange(HibSchemaChange<?> change) {
		setSingleLinkOutTo(toGraph(change), HAS_SCHEMA_CONTAINER);
	}

	@Override
	public SchemaChange<?> getPreviousChange() {
		return (SchemaChange<?>) in(HAS_SCHEMA_CONTAINER).nextOrDefault(null);
	}

	@Override
	public void setPreviousChange(HibSchemaChange<?> change) {
		setSingleLinkInTo(toGraph(change), HAS_SCHEMA_CONTAINER);
	}

	@Override
	public SCV getNextVersion() {
		return out(HAS_VERSION).has(getContainerVersionClass()).nextOrDefaultExplicit(getContainerVersionClass(), null);
	}

	@Override
	public SCV getPreviousVersion() {
		return in(HAS_VERSION).has(getContainerVersionClass()).nextOrDefaultExplicit(getContainerVersionClass(), null);
	}

	@Override
	public void setPreviousVersion(SCV container) {
		setSingleLinkInTo(toGraph(container), HAS_VERSION);
	}

	@Override
	public void setNextVersion(SCV container) {
		setSingleLinkOutTo(toGraph(container), HAS_VERSION);
	}

	@Override
	public Boolean getNoIndex() {
		return property("noindex");
	}

	@Override
	public void setNoIndex(Boolean noIndex) {
		if (noIndex != null && noIndex) {
			property("noindex", noIndex);
		} else {
			removeProperty("noindex");
		}
	}

	@Override
	public String getJson() {
		return property("json");
	}

	@Override
	public void setJson(String json) {
		property("json", json);
	}

	@Override
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		throw new NotImplementedException("Updating is not directly supported for schemas. Please start a schema migration");
	}

	@Override
	public void setSchemaContainer(SC container) {
		setUniqueLinkInTo(toGraphContainer(container), HAS_PARENT_CONTAINER);
	}

	@Override
	public SC getSchemaContainer() {
		return in(HAS_PARENT_CONTAINER).nextOrDefaultExplicit(getContainerClass(), null);
	}

	/**
	 * Debug information.
	 */
	public String toString() {
		return "handler:" + getTypeInfo().getType() + "_name:" + getName() + "_uuid:" + getUuid() + "_version:" + getVersion();
	}

}
