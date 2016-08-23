package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PARENT_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_VERSION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.core.rest.common.GenericMessageResponse.message;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainer;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.handler.AbstractFieldSchemaContainerComparator;
import com.gentics.mesh.core.data.schema.handler.FieldSchemaContainerMutator;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.UUIDUtil;

import rx.Single;

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
public abstract class AbstractGraphFieldSchemaContainerVersion<R extends FieldSchemaContainer, RE extends NameUuidReference<RE>, SCV extends GraphFieldSchemaContainerVersion<R, RE, SCV, SC>, SC extends GraphFieldSchemaContainer<R, RE, SC, SCV>>
		extends AbstractMeshCoreVertex<R, SCV> implements GraphFieldSchemaContainerVersion<R, RE, SCV, SC> {

	public static final String VERSION_PROPERTY_KEY = "version";

	@Override
	public void setName(String name) {
		setProperty("name", name);
	}

	@Override
	public String getName() {
		return getProperty("name");
	}

	/**
	 * Return the class that is used for container versions.
	 * 
	 * @return
	 */
	protected abstract Class<? extends SCV> getContainerVersionClass();

	/**
	 * Return the class that is used for containers.
	 * 
	 * @return
	 */
	protected abstract Class<? extends SC> getContainerClass();

	/**
	 * Return the eventbus migration verticle address.
	 * 
	 * @return
	 */
	protected abstract String getMigrationAddress();

	@Override
	public int getVersion() {
		return getProperty(VERSION_PROPERTY_KEY);
	}

	@Override
	public SchemaChange<?> getNextChange() {
		return (SchemaChange<?>) out(HAS_SCHEMA_CONTAINER).nextOrDefault(null);
	}

	@Override
	public void setNextChange(SchemaChange<?> change) {
		setSingleLinkOutTo(change.getImpl(), HAS_SCHEMA_CONTAINER);
	}

	@Override
	public SchemaChange<?> getPreviousChange() {
		return (SchemaChange<?>) in(HAS_SCHEMA_CONTAINER).nextOrDefault(null);
	}

	@Override
	public void setPreviousChange(SchemaChange<?> change) {
		setSingleLinkInTo(change.getImpl(), HAS_SCHEMA_CONTAINER);
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
		setSingleLinkInTo(container.getImpl(), HAS_VERSION);
	}

	@Override
	public void setNextVersion(SCV container) {
		setSingleLinkOutTo(container.getImpl(), HAS_VERSION);
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

	/**
	 * Load the stored schema JSON data.
	 * 
	 * @return
	 */
	protected String getJson() {
		return getProperty("json");
	}

	/**
	 * Update the stored schema JSON data.
	 * 
	 * @param json
	 */
	protected void setJson(String json) {
		setProperty("json", json);
	}

	@Override
	public Single<? extends SCV> update(InternalActionContext ac) {
		throw new NotImplementedException("Updating is not directly supported for schemas. Please start a schema migration");
	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
		// TODO Auto-generated method stub

	}

	@Override
	public Single<GenericMessageResponse> applyChanges(InternalActionContext ac, SchemaChangesListModel listOfChanges) {
		if (listOfChanges.getChanges().isEmpty()) {
			throw error(BAD_REQUEST, "schema_migration_no_changes_specified");
		}
		Database db = MeshSpringConfiguration.getInstance().database();
		return db.tx(() -> {
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

			R resultingSchema = new FieldSchemaContainerMutator().apply(this);
			resultingSchema.validate();

			// Increment version of the schema
			resultingSchema.setVersion(resultingSchema.getVersion() + 1);

			// Create and set the next version of the schema
			SCV nextVersion = getGraph().addFramedVertex(getContainerVersionClass());
			nextVersion.setSchema(resultingSchema);

			// Check for conflicting container names
			String newName = resultingSchema.getName();
			SC foundContainer = getSchemaContainer().getRoot().findByName(resultingSchema.getName()).toBlocking().value();
			if (foundContainer != null && !foundContainer.getUuid().equals(getSchemaContainer().getUuid())) {
				throw conflict(foundContainer.getUuid(), newName, "schema_conflicting_name", newName);
			}

			nextVersion.setSchemaContainer(getSchemaContainer());
			nextVersion.setName(resultingSchema.getName());
			// TODO avoid updates of schema names - See https://jira.gentics.com/browse/CL-348
			getSchemaContainer().setName(resultingSchema.getName());
			setNextVersion(nextVersion);

			// Update the latest version of the schema container
			getSchemaContainer().setLatestVersion(nextVersion);

			// Update the search index
			return createIndexBatch(STORE_ACTION);
		}).process().toSingle(() -> {
			return db.noTx(() -> {
				return message(ac, "migration_invoked", getName());
			});
		});
	}

	/**
	 * Overwrite default implementation since we need to add the parent container of all versions to the index and not the current version.
	 */
	@Override
	public SearchQueueBatch createIndexBatch(SearchQueueEntryAction action) {
		SearchQueue queue = BootstrapInitializer.getBoot().meshRoot().getSearchQueue();
		SearchQueueBatch batch = queue.createBatch(UUIDUtil.randomUUID());
		batch.addEntry(this.getSchemaContainer(), action);
		addRelatedEntries(batch, action);
		return batch;
	}

	@Override
	public Single<GenericMessageResponse> applyChanges(InternalActionContext ac) {
		Database db = MeshSpringConfiguration.getInstance().database();
		try {
			SchemaChangesListModel listOfChanges = JsonUtil.readValue(ac.getBodyAsString(), SchemaChangesListModel.class);

			return db.tx(() -> {
				if (getNextChange() != null) {
					throw error(INTERNAL_SERVER_ERROR, "migration_error_version_already_contains_changes", String.valueOf(getVersion()), getName());
				}

				return applyChanges(ac, listOfChanges);

			});
		} catch (Exception e) {
			return Single.error(e);
		}
	}

	@Override
	public Single<SchemaChangesListModel> diff(InternalActionContext ac, AbstractFieldSchemaContainerComparator comparator,
			FieldSchemaContainer fieldContainerModel) {
		try {
			SchemaChangesListModel list = new SchemaChangesListModel();
			fieldContainerModel.validate();
			list.getChanges().addAll(comparator.diff(transformToRest(ac, 0, null).toBlocking().value(), fieldContainerModel));
			return Single.just(list);
		} catch (Exception e) {
			return Single.error(e);
		}

	}

	@Override
	public void setSchemaContainer(SC container) {
		setUniqueLinkInTo(container.getImpl(), HAS_PARENT_CONTAINER);
	}

	@Override
	public SC getSchemaContainer() {
		return in(HAS_PARENT_CONTAINER).nextOrDefaultExplicit(getContainerClass(), null);
	}

	public String toString() {
		return "type:" + getType() + "_name:" + getName() + "_uuid:" + getUuid() + "_version:" + getVersion();
	}

}
