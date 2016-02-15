package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_VERSION;
import static com.gentics.mesh.core.rest.common.GenericMessageResponse.message;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.handler.AbstractFieldSchemaContainerComparator;
import com.gentics.mesh.core.data.schema.handler.FieldSchemaContainerMutator;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaMigrationResponse;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;

/**
 * The {@link AbstractGraphFieldSchemaContainer} contains the abstract graph element implementation for {@link GraphFieldSchemaContainer} implementations (e.g.:
 * {@link SchemaContainerImpl}, {@link MicroschemaContainerImpl}).
 * 
 * @param <R>
 *            Field container rest model type
 * @param <V>
 *            Graph vertex type
 * @param <RE>
 *            Field container rest model reference type
 */
public abstract class AbstractGraphFieldSchemaContainer<R extends FieldSchemaContainer, V extends GraphFieldSchemaContainer<R, V, RE>, RE extends NameUuidReference<RE>>
		extends AbstractMeshCoreVertex<R, V> implements GraphFieldSchemaContainer<R, V, RE> {

	public static final String VERSION_PROPERTY_KEY = "version";

	/**
	 * Return the class that is used to construct new containers.
	 * 
	 * @return
	 */
	protected abstract Class<? extends V> getContainerClass();

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
	public V getNextVersion() {
		return out(HAS_VERSION).has(getContainerClass()).nextOrDefaultExplicit(getContainerClass(), null);
	}

	@Override
	public void setNextVersion(V container) {
		setSingleLinkOutTo(container.getImpl(), HAS_VERSION);
	}

	@Override
	public V getLatestVersion() {
		V latest = (V) this;
		for (V current = latest.getNextVersion(); current != null; current = current.getNextVersion()) {
			latest = current;
		}
		return latest;
	}

	@Override
	public V getPreviousVersion() {
		return in(HAS_VERSION).has(getContainerClass()).nextOrDefaultExplicit(getContainerClass(), null);
	}

	@Override
	public void setPreviousVersion(V container) {
		setSingleLinkInTo(container.getImpl(), HAS_VERSION);
	}

	@Override
	public SchemaChange<?> getNextChange() {
		return (SchemaChange) out(HAS_SCHEMA_CONTAINER).nextOrDefault(null);
	}

	@Override
	public void setNextChange(SchemaChange<?> change) {
		setSingleLinkOutTo(change.getImpl(), HAS_SCHEMA_CONTAINER);
	}

	@Override
	public SchemaChange<?> getPreviousChange() {
		return (SchemaChange) in(HAS_SCHEMA_CONTAINER).nextOrDefault(null);
	}

	@Override
	public void setPreviousChange(SchemaChange<?> change) {
		setSingleLinkInTo(change.getImpl(), HAS_SCHEMA_CONTAINER);
	}

	@Override
	public Observable<GenericMessageResponse> applyChanges(InternalActionContext ac) {
		Database db = MeshSpringConfiguration.getInstance().database();
		try {
			SchemaChangesListModel listOfChanges = JsonUtil.readValue(ac.getBodyAsString(), SchemaChangesListModel.class);

			return db.trx(() -> {
				if (getNextChange() != null) {
					throw error(INTERNAL_SERVER_ERROR, "migration_error_version_already_contains_changes", String.valueOf(getVersion()), getName());
				}

				SchemaChange current = null;
				for (SchemaChangeModel restChange : listOfChanges.getChanges()) {
					SchemaChange graphChange = createChange(restChange);
					// Set the first change to the schema container and chain all other changes to that change.
					if (current == null) {
						current = graphChange;
						setNextChange(current);
					} else {
						current.setNextChange(graphChange);
						current = graphChange;
					}
				}

				R resultingSchema = FieldSchemaContainerMutator.getInstance().apply(this);
				resultingSchema.validate();

				// Increment version of the schema
				resultingSchema.setVersion(getVersion() + 1);

				// Create and set the next version of the schema
				V nextVersion = getGraph().addFramedVertex(getContainerClass());
				nextVersion.setSchema(resultingSchema);
				setNextVersion(nextVersion);

				// Make sure to unlink the old schema container from the container root and assign the new version to the root.
				DeliveryOptions options = new DeliveryOptions();
				options.addHeader(NodeMigrationVerticle.UUID_HEADER, this.getUuid());
//				ObservableFuture<SchemaMigrationResponse> obsFut = RxHelper.observableFuture();

				Mesh.vertx().eventBus().send(getMigrationAddress(), null, options, (rh) -> {
//					if (rh.succeeded()) {
//						obsFut.toHandler().handle(Future.succeededFuture(new SchemaMigrationResponse()));
//					} else {
//						obsFut.toHandler().handle(Future.failedFuture(rh.cause()));
//					}
				});

				return ObservableFuture.just(message(ac, "migration_invoked", getName()));
			});
		} catch (Exception e) {
			return Observable.error(e);
		}
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
		case CHANGEFIELDTYPE:
			schemaChange = getGraph().addFramedVertex(FieldTypeChangeImpl.class);
			break;
		case REMOVEFIELD:
			schemaChange = getGraph().addFramedVertex(RemoveFieldChangeImpl.class);
			break;
		case UPDATEFIELD:
			schemaChange = getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			break;
		case UPDATESCHEMA:
			schemaChange = getGraph().addFramedVertex(UpdateSchemaChangeImpl.class);
			break;
		default:
			throw error(BAD_REQUEST, "error_change_operation_unknown", String.valueOf(restChange.getOperation()));
		}

		schemaChange.updateFromRest(restChange);
		return schemaChange;

	}

	@Override
	public Observable<SchemaChangesListModel> diff(InternalActionContext ac, AbstractFieldSchemaContainerComparator comparator,
			FieldSchemaContainer fieldContainerModel) {
		try {
			SchemaChangesListModel list = new SchemaChangesListModel();
			fieldContainerModel.validate();
			list.getChanges().addAll(comparator.diff(transformToRest(ac, null).toBlocking().single(), fieldContainerModel));
			return Observable.just(list);
		} catch (Exception e) {
			return Observable.error(e);
		}

	}

}
