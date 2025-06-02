package com.gentics.mesh.hibernate.data.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.contentoperation.ContentFieldKey;
import com.gentics.mesh.contentoperation.ContentKey;
import com.gentics.mesh.contentoperation.ContentStorage;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.binary.HibImageVariant;
import com.gentics.mesh.core.data.dao.PersistingBinaryDao;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.image.ImageManipulator;
import com.gentics.mesh.core.rest.common.ReferenceType;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.data.dao.util.CommonDaoHelper;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.binary.impl.HibBinariesImpl;
import com.gentics.mesh.hibernate.data.domain.HibBinaryFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibBinaryImpl;
import com.gentics.mesh.hibernate.data.domain.HibImageVariantImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibBinaryFieldImpl;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.hibernate.event.EventFactory;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.hibernate.util.SplittingUtils;
import com.gentics.mesh.util.UUIDUtil;

import dagger.Lazy;
import io.vertx.core.Vertx;
import jakarta.persistence.EntityGraph;

/**
 * Binary DAO implementation for Gentics Mesh.
 *
 * @author plyhun
 *
 */
@Singleton
public class BinaryDaoImpl extends AbstractImageDataHibDao<HibBinary> implements PersistingBinaryDao {

	private final HibBinariesImpl binaries;
	private final ContentStorage contentStorage;

	@Inject
	public BinaryDaoImpl(
			Lazy<BinaryStorage> binaryStorage,
			Lazy<ImageManipulator> imageManipulator,
			HibPermissionRoots permissionRoots,
			CommonDaoHelper commonDaoHelper,
			CurrentTransaction currentTransaction,
			EventFactory eventFactory,
			Lazy<Vertx> vertx,
			HibBinariesImpl binaries,
			ContentStorage contentStorage) {
		super(permissionRoots, commonDaoHelper, currentTransaction, eventFactory, vertx);
		this.binaries = binaries;
		this.contentStorage = contentStorage;
	}

	@Override
	public Result<HibBinaryField> findFields(HibBinary binary) {
		Set<ContentFieldKey> contentFieldKeys = em().createNamedQuery("binaryfieldref.findByBinaryUuid", HibBinaryFieldEdgeImpl.class)
			.setParameter("dbUuid", binary.getId())
			.getResultStream()
			.map(edge -> new ContentFieldKey(edge.getContainerUuid(), edge.getContainerVersionUuid(), ReferenceType.FIELD, edge.getFieldKey()))
			.collect(Collectors.toSet());
		Set<ContentKey> contentKeys = contentFieldKeys.stream()
			.map(ContentKey.class::cast)
			.collect(Collectors.toSet());
		Map<UUID, List<String>> fieldKeys = contentFieldKeys.stream()
			.collect(Collectors.groupingBy(key -> key.getContentUuid(), Collectors.mapping(key -> key.getFieldKey(), Collectors.toList())));
		Stream<HibBinaryFieldImpl> result = contentStorage.findMany(contentKeys).stream()
			.flatMap(content -> fieldKeys.get(content.getDbUuid()).stream().map(fieldKey -> content.getBinary(fieldKey)));

		return new TraversalResult<>(result);
	}

	@Override
	public Binaries binaries() {
		return binaries;
	}

	/**
	 * Find a binary field in the container with given UUID, by a field key.
	 *
	 * @param contentUuid
	 * @param key
	 * @return entity or null
	 */
	public HibBinaryField getField(UUID contentUuid, String key) {
		return em().createNamedQuery("binaryfieldref.findByContentAndKey", HibBinaryFieldImpl.class)
				.setParameter("contentUuid", contentUuid)
				.setParameter("key", key)
				.getResultStream()
				.findFirst()
				.orElse(null);
	}

	/**
	 * Remove a binary field from the database.
	 *
	 * @param contentUuid
	 * @param key
	 * @return entity or null
	 */
	public void removeField(HibField field) {
		ImageVariantDaoImpl imageVariantDao = HibernateTx.get().imageVariantDao();
		HibBinaryField binaryRef = (HibBinaryField) field;
		HibBinaryImpl binary = (HibBinaryImpl) binaryRef.getBinary();

		for (HibImageVariant variant : imageVariantDao.getVariants(binaryRef, null)) {
			imageVariantDao.detachVariant(binaryRef, (HibImageVariantImpl) variant, variant.getKey(), null, false);
		};
		em().remove(binaryRef);
		long fieldCount = ((Number) em().createNamedQuery("binary.getFieldCount").setParameter("uuid", binary.getId()).getSingleResult()).longValue();
		if (fieldCount == 0) {
			for (HibImageVariant variant : imageVariantDao.getVariants(binary, null)) {
				imageVariantDao.deletePersistedVariant(binary, variant, true);
			};
			currentTransaction.getTx().data().binaryStorage().deleteOnTxSuccess(binary.getUuid(), currentTransaction.getTx());
			currentTransaction.getTx().delete(binary);
		}
	}

	/**
	 * Remove all binary field references connected to the provided container uuids.
	 * After that, remove all binaries that are not referenced anymore
	 * @param containerUuids
	 */
	public void removeField(List<UUID> containerUuids) {
		SplittingUtils.splitAndConsume(containerUuids, HibernateUtil.inQueriesLimitForSplitting(1), slice -> em().createNamedQuery("binaryfieldref.removeByContainerUuids")
				.setParameter("containerUuids", slice)
				.executeUpdate());

		List<UUID> uuids = currentTransaction.getEntityManager().createNamedQuery("binary.findUnreferencedBinaryUuids", UUID.class)
				.getResultList();
		uuids.forEach(uuid -> {
			currentTransaction.getTx().data().binaryStorage().deleteOnTxSuccess(UUIDUtil.toShortUuid(uuid), currentTransaction.getTx());
		});

		SplittingUtils.splitAndConsume(uuids, HibernateUtil.inQueriesLimitForSplitting(1), slice -> {
			currentTransaction.getEntityManager().createQuery("delete from binary where dbUuid in :uuids")
					.setParameter("uuids", slice)
					.executeUpdate();
		});
	}

	/**
	 * Load the binary fields and binaries, which are referenced in the given list of containers
	 * @param containers containers, which probably contain binary fields
	 */
	public void loadBinaryFields(List<? extends HibNodeFieldContainer> containers) {
		// collect the refUuids from the binary fields
		Set<UUID> refUuids = containers
			.stream()
			.flatMap(container -> container.getFields().stream())
			.filter(field -> field instanceof HibBinaryFieldImpl)
			.map(field -> ((HibBinaryFieldImpl)field).valueOrNull())
			.filter(value -> value != null)
			.collect(Collectors.toSet());

		// early exit, if not binary fields found
		if (refUuids.isEmpty()) {
			return;
		}

		// load the fields
		EntityGraph<?> entityGraph = em().getEntityGraph("binaryfieldref.metadataProperties");
		List<HibBinaryFieldEdgeImpl> fields = SplittingUtils.splitAndMergeInList(refUuids, HibernateUtil.inQueriesLimitForSplitting(1), slice -> em().createNamedQuery("binaryfieldref.findByUuids", HibBinaryFieldEdgeImpl.class)
			.setParameter("uuids", slice)
			.setHint("jakarta.persistence.fetchgraph", entityGraph)
			.getResultList());

		// collect the binary uuids
		Set<UUID> binaryUuids = fields.stream().map(fieldEdge -> fieldEdge.getValueOrUuid())
				.collect(Collectors.toSet());

		// early exit, if no binaries need to be loaded
		if (binaryUuids.isEmpty()) {
			return;
		}

		// load the binaries
		SplittingUtils.splitAndMergeInList(binaryUuids, HibernateUtil.inQueriesLimitForSplitting(1), slice -> em().createNamedQuery("binary.findByUuids", HibBinary.class)
			.setParameter("uuids", slice)
			.getResultList());
	}

	@Override
	public String[] getHibernateEntityName(Object... unused) {
		return new String[] {currentTransaction.getTx().data().getDatabaseConnector().maybeGetDatabaseEntityName(HibBinaryImpl.class).get()};
	}

	@Override
	public String mapGraphQlFilterFieldName(String gqlName) {
		switch (gqlName) {
		case "checksum": return "SHA512Sum";
		}
		return super.mapGraphQlFilterFieldName(gqlName);
	}
}
