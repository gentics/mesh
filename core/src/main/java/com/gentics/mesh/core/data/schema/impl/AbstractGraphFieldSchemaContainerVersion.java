package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PARENT_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_VERSION;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraphContainer;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.schema.HibFieldSchemaElement;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibSchemaChange;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.handler.FieldSchemaContainerComparator;
import com.gentics.mesh.core.data.schema.handler.FieldSchemaContainerMutator;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;

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
public abstract class AbstractGraphFieldSchemaContainerVersion<R extends FieldSchemaContainer, RM extends FieldSchemaContainerVersion, RE extends NameUuidReference<RE>, SCV extends HibFieldSchemaVersionElement<R, RM, SC, SCV>, SC extends HibFieldSchemaElement<R, RM, SC, SCV>>
	extends AbstractMeshCoreVertex<R> implements GraphFieldSchemaContainerVersion<R, RM, RE, SCV, SC>, HibFieldSchemaVersionElement<R, RM, SC, SCV> {

	@Override
	public void setName(String name) {
		property("name", name);
	}

	@Override
	public String getName() {
		return property("name");
	}

	/**
	 * Return the class that is used for container versions.
	 * 
	 * @return Class of the container version
	 */
	protected abstract Class<? extends SCV> getContainerVersionClass();

	/**
	 * Return the class that is used for containers.
	 * 
	 * @return Class of the container
	 */
	protected abstract Class<? extends SC> getContainerClass();

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

	/**
	 * Create a new graph change from the given rest change.
	 * 
	 * @param restChange
	 * @return
	 */
	private SchemaChange<?> createChange(SchemaChangeModel restChange) {

		SchemaChange<?> schemaChange = null;
		switch (restChange.getOperation()) {
		case ADDFIELD:
			schemaChange = getGraph().addFramedVertex(AddFieldChangeImpl.class);
			break;
		case REMOVEFIELD:
			schemaChange = getGraph().addFramedVertex(RemoveFieldChangeImpl.class);
			break;
		case UPDATEFIELD:
			schemaChange = getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			break;
		case CHANGEFIELDTYPE:
			schemaChange = getGraph().addFramedVertex(FieldTypeChangeImpl.class);
			break;
		case UPDATESCHEMA:
			schemaChange = getGraph().addFramedVertex(UpdateSchemaChangeImpl.class);
			break;
		case UPDATEMICROSCHEMA:
			schemaChange = getGraph().addFramedVertex(UpdateMicroschemaChangeImpl.class);
			break;
		default:
			throw error(BAD_REQUEST, "error_change_operation_unknown", String.valueOf(restChange.getOperation()));
		}
		// Set properties from rest model
		schemaChange.updateFromRest(restChange);
		return schemaChange;

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
	public SCV applyChanges(InternalActionContext ac, SchemaChangesListModel listOfChanges, EventQueueBatch batch) {
		if (listOfChanges.getChanges().isEmpty()) {
			throw error(BAD_REQUEST, "schema_migration_no_changes_specified");
		}
		SchemaChange<?> current = null;
		for (SchemaChangeModel restChange : listOfChanges.getChanges()) {
			SchemaChange<?> graphChange = createChange(restChange);
			// Set the first change to the schema container and chain all other changes to that change.
			if (current == null) {
				current = graphChange;
				setNextChange(current);
			} else {
				current.setNextChange(graphChange);
				current = graphChange;
			}
		}

		RM resultingSchema = new FieldSchemaContainerMutator().apply(this);
		resultingSchema.validate();

		// Increment version of the schema
		resultingSchema.setVersion(String.valueOf(Double.valueOf(resultingSchema.getVersion()) + 1));

		// Create and set the next version of the schema
		SCV nextVersion = getGraph().addFramedVertex(getContainerVersionClass());
		nextVersion.setSchema(resultingSchema);

		// Check for conflicting container names
		String newName = resultingSchema.getName();
		SC foundContainer = toGraphContainer(getSchemaContainer()).getRoot().findByName(resultingSchema.getName());
		if (foundContainer != null && !foundContainer.getUuid().equals(getSchemaContainer().getUuid())) {
			throw conflict(foundContainer.getUuid(), newName, "schema_conflicting_name", newName);
		}

		nextVersion.setSchemaContainer(getSchemaContainer());
		nextVersion.setName(resultingSchema.getName());
		getSchemaContainer().setName(resultingSchema.getName());
		setNextVersion(nextVersion);

		// Update the latest version of the schema container
		getSchemaContainer().setLatestVersion(nextVersion);

		// Update the search index
		batch.add(getSchemaContainer().onUpdated());
		return nextVersion;
	}

	@Override
	public SCV applyChanges(InternalActionContext ac, EventQueueBatch batch) {
		SchemaChangesListModel listOfChanges = JsonUtil.readValue(ac.getBodyAsString(), SchemaChangesListModel.class);

		if (getNextChange() != null) {
			throw error(INTERNAL_SERVER_ERROR, "migration_error_version_already_contains_changes", String.valueOf(getVersion()), getName());
		}
		return applyChanges(ac, listOfChanges, batch);
	}

	@Override
	public SchemaChangesListModel diff(InternalActionContext ac, FieldSchemaContainerComparator comparator,
		FieldSchemaContainer fieldContainerModel) {
		SchemaChangesListModel list = new SchemaChangesListModel();
		fieldContainerModel.validate();
		list.getChanges().addAll(comparator.diff(transformToRestSync(ac, 0), fieldContainerModel));
		return list;
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
