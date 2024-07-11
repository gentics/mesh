package com.gentics.mesh.database;

import java.util.Optional;

import javax.inject.Inject;

import com.gentics.mesh.cache.ListableFieldCache;
import com.gentics.mesh.contentoperation.ContentStorage;
import com.gentics.mesh.core.data.MeshVersion;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.data.storage.S3BinaryStorage;
import com.gentics.mesh.core.db.CommonTxData;
import com.gentics.mesh.changelog.HibernateBootstrapInitializerImpl;
import com.gentics.mesh.dagger.HibernateMeshComponent;
import com.gentics.mesh.dagger.tx.TransactionScope;
import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.hibernate.data.dao.MeshVersionImpl;
import com.gentics.mesh.hibernate.data.domain.AbstractHibListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;

import io.vertx.core.Vertx;

/**
 * Transaction-bound data.
 * 
 * @author plyhun
 *
 */
@TransactionScope
public class HibTxData implements CommonTxData {

	private final DatabaseConnector databaseConnector;
	private final HibernateBootstrapInitializerImpl boot;
	private final HibernateMeshOptions options;
	private final HibPermissionRoots permissionRoots;
	private final ContentStorage contentStorage;
	private final ListableFieldCache<AbstractHibListFieldEdgeImpl<?>> listableFieldCache;

	private Optional<EventQueueBatch> qBatch;

	@Inject
	public HibTxData(HibernateMeshOptions options, HibernateBootstrapInitializerImpl boot,
					 HibPermissionRoots permissionRoots, ContentStorage contentStorage, 
					 ListableFieldCache<AbstractHibListFieldEdgeImpl<?>> listableFieldCache, DatabaseConnector databaseConnector) {
		this.options = options;
		this.boot = boot;
		this.permissionRoots = permissionRoots;
		this.contentStorage = contentStorage;
		this.listableFieldCache = listableFieldCache;
		this.qBatch = Optional.empty();
		this.databaseConnector = databaseConnector;
	}

	@Override
	public HibernateMeshComponent mesh() {
		return boot.mesh().internal();
	}

	@Override
	public HibernateMeshOptions options() {
		return options;
	}

	@Override
	public MeshVersion meshVersion() {
		return new MeshVersionImpl(HibernateTx.get().entityManager());
	}

	@Override
	public HibPermissionRoots permissionRoots() {
		return permissionRoots;
	}

	/**
	 * Asserts that the uuid is not used already in the system.
	 * @param uuid
	 */
	public static void assertUnused(String uuid, HibPermissionRoots permissionRoots) {
		if (uuid == null) {
			return;
		}
//		// TODO HIB smells like bad performance
//		HibBaseElement element = firstOrNull(em().createQuery("select 1" +
//				" from HibBaseElement e" +
//				" where uuid = :uuid",
//			HibBaseElement.class)
//			.setMaxResults(1)
//			.setParameter("uuid", UUIDUtil.toJavaUuid(uuid)));
//		if (element != null) {
//			throw new RuntimeException(String.format("Duplicate uuid {%s} found. Already used as another base element.", uuid));
//		}
	}

	@Override
	public Vertx vertx() {
		return boot.vertx();
	}

	@Override
	public boolean isVertxReady() {
		return boot.isVertxReady();
	}

	@Override
	public ServerSchemaStorage serverSchemaStorage() {
		return mesh().serverSchemaStorage();
	}

	@Override
	public BinaryStorage binaryStorage() {
		return mesh().binaryStorage();
	}

	@Override
	public S3BinaryStorage s3BinaryStorage() {
		return mesh().s3binaryStorage();
	}

	public ContentStorage getContentStorage() {
		return contentStorage;
	}

	public DatabaseConnector getDatabaseConnector() {
		return databaseConnector;
	}

	/**
	 * Get the cache for listable field values
	 * @return cache
	 */
	public ListableFieldCache<AbstractHibListFieldEdgeImpl<?>> getListableFieldCache() {
		return listableFieldCache;
	}

	@Override
	public void setEventQueueBatch(EventQueueBatch batch) {
		this.qBatch = Optional.ofNullable(batch);
	}

	@Override
	public Optional<EventQueueBatch> maybeGetEventQueueBatch() {
		return qBatch;
	}
}
