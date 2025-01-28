package com.gentics.mesh.hibernate.data.dao;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.File;
import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.binary.HibImageVariant;
import com.gentics.mesh.core.data.dao.PersistingImageVariantDao;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.image.ImageManipulator;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantRequest;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.data.dao.util.CommonDaoHelper;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibBinaryImpl;
import com.gentics.mesh.hibernate.data.domain.HibImageVariantImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibBinaryFieldBase;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.hibernate.event.EventFactory;
import com.gentics.mesh.parameter.image.ImageManipulation;
import com.gentics.mesh.util.UUIDUtil;

import dagger.Lazy;
import io.vertx.core.Vertx;
import jakarta.persistence.TypedQuery;

@Singleton
public class ImageVariantDaoImpl extends AbstractImageDataHibDao<HibImageVariant> implements PersistingImageVariantDao {

	private static final Logger log = LoggerFactory.getLogger(ImageVariantDaoImpl.class);

	private final Lazy<ImageManipulator> imageManipulator;
	private final Lazy<BinaryStorage> binaryStorage;

	@Inject
	public ImageVariantDaoImpl(
			Lazy<BinaryStorage> binaryStorage,
			Lazy<ImageManipulator> imageManipulator,
			HibPermissionRoots permissionRoots,
			CommonDaoHelper commonDaoHelper,
			CurrentTransaction currentTransaction,
			EventFactory eventFactory,
			Lazy<Vertx> vertx) {
		super(permissionRoots, commonDaoHelper, currentTransaction, eventFactory, vertx);
		this.imageManipulator = imageManipulator;
		this.binaryStorage = binaryStorage;
	}

	@Override
	public HibImageVariant findByUuid(String uuid) {
		return em().find(HibImageVariantImpl.class, UUIDUtil.toJavaUuid(uuid));
	}

	@Override
	public Result<? extends HibImageVariant> getVariants(HibBinaryField binaryField, InternalActionContext ac) {
		return binaryField.getImageVariants();
	}

	@Override
	public HibImageVariant getVariant(HibBinaryField binaryField, ImageManipulation variant, InternalActionContext ac) {
		String queryName = "imagevariant_find_by_manipulation_field_no_auto";
		boolean noAuto = true;
		if (variant.getHeight() == null) {
			queryName = "imagevariant_find_by_manipulation_field_no_auto_height";
		} else if ("auto".equals(variant.getHeight())) {
			queryName = "imagevariant_find_by_manipulation_field_auto_height";
			noAuto = false;
		}
		if (variant.getWidth() == null) {
			queryName = "imagevariant_find_by_manipulation_field_no_auto_width";
		} else if ("auto".equals(variant.getWidth())) {
			queryName = "imagevariant_find_by_manipulation_field_auto_width";
			noAuto = false;
		}
		return queryFromManipulationRequest(variant, queryName, noAuto).setParameter("field", ((HibBinaryFieldBase) binaryField).getEdge()).getResultList().stream().findAny().orElse(null);
	}

	@Override
	public HibImageVariant createPersistedVariant(HibBinary binary, ImageVariantRequest request, Consumer<HibImageVariant> inflater) {
		HibernateTx hibTx = HibernateTx.get();
		HibImageVariantImpl variant = hibTx.create(HibImageVariantImpl.class);
		inflater.accept(variant);
		
		String variantUuid = variant.getUuid();
		
		long filesize = imageManipulator.get().handleResize(binary, request)
				.flatMap(cachePath -> {
					long size = new File(cachePath).length();
					return binaryStorage.get().moveInPlace(variantUuid, cachePath, false).toSingleDefault(size);
				})
				.blockingGet();

		variant.setSize(filesize);
		variant.setBinary(binary);
		((HibBinaryImpl) binary).getVariants().add(variant);
		return em().merge(variant);
	}

	@Override
	public boolean deletePersistedVariant(HibBinary binary, HibImageVariant variant, boolean throwOnInUse) {
		if (variant.findFields().hasNext()) {
			if (throwOnInUse) {
				throw error(BAD_REQUEST, "image_error_variant_in_use", variant.getKey());
			} else {
				log.info("The variant {} is in use and cannot be deleted", variant.getKey());
				return false;
			}
		}
		String variantUuid = variant.getUuid();
		binaryStorage.get().deleteOnTxSuccess(variantUuid, currentTransaction.getTx());
		return true;
	}

	@Override
	public Result<? extends HibImageVariant> getVariants(HibBinary binary, InternalActionContext ac) {
		Set<HibImageVariant> variants = ((HibBinaryImpl) binary).getVariants();
		if (variants != null) {
			return new TraversalResult<>(variants);
		} else {
			return TraversalResult.empty();
		}
	}

	@Override
	public HibImageVariant getVariant(HibBinary binary, ImageManipulation variant, InternalActionContext ac) {
		String queryName = "imagevariant_find_by_manipulation_binary_no_auto";
		boolean noAuto = true;
		if (variant.getHeight() == null) {
			queryName = "imagevariant_find_by_manipulation_binary_no_auto_height";
		} else if ("auto".equals(variant.getHeight())) {
			queryName = "imagevariant_find_by_manipulation_binary_auto_height";
			noAuto = false;
		}
		if (variant.getWidth() == null) {
			queryName = "imagevariant_find_by_manipulation_binary_no_auto_width";
		} else if ("auto".equals(variant.getWidth())) {
			queryName = "imagevariant_find_by_manipulation_binary_auto_width";
			noAuto = false;
		}
		return queryFromManipulationRequest(variant, queryName, noAuto).setParameter("binary", binary).getResultList().stream().findAny().orElse(null);
	}

	@Override
	public void attachVariant(HibBinaryField binaryField, ImageVariantRequest request, InternalActionContext ac, boolean throwOnExisting) {
		HibBinary binary = binaryField.getBinary();
		HibImageVariantImpl variant = (HibImageVariantImpl) getVariant(binaryField, request, ac);
		if (variant != null) {
			if (throwOnExisting) {
				throw error(BAD_REQUEST, "Requested variant `{}` of field `{}` already exists", request.getCacheKey(), binaryField.getFieldKey());
			} else {
				log.info("Requested variant {} of binary {} is already attached to the binary field {}", request.getCacheKey(), binary.getBinaryDataId(), binaryField.getFieldKey());
				return;
			}
		} else {
			variant = (HibImageVariantImpl) getVariant(binary, request, ac);
		}
		attachVariant(binaryField, variant, throwOnExisting);
	}

	public void attachVariant(HibBinaryField binaryField, HibImageVariantImpl variant, boolean throwOnExisting) {
		HibBinaryFieldBase base = (HibBinaryFieldBase) binaryField;
		variant.addField(base.getEdge(), throwOnExisting);
		em().merge(variant);
		em().flush();
		em().refresh(((HibBinaryFieldBase)binaryField).getEdge());
	}

	@Override
	public void detachVariant(HibBinaryField binaryField, ImageVariantRequest request, InternalActionContext ac, boolean throwOnAbsent) {
		HibImageVariantImpl variant = (HibImageVariantImpl) getVariant(binaryField, request, ac);
		detachVariant(binaryField, variant, request.getCacheKey(), ac, throwOnAbsent);
	}

	@Override
	public String[] getHibernateEntityName(Object... arg) {
		return new String[] {currentTransaction.getTx().data().getDatabaseConnector().maybeGetDatabaseEntityName(HibImageVariantImpl.class).get()};
	}

	@Override
	public String mapGraphQlFilterFieldName(String gqlName) {
		switch (gqlName) {
		case "value": return "dbUuid";
		}
		return super.mapGraphQlFilterFieldName(gqlName);
	}

	/**
	 * Detach the binary field from the given variant.
	 * 
	 * @param binaryField
	 * @param variant
	 * @param ac
	 * @param throwOnAbsent
	 */
	public void detachVariant(HibBinaryField binaryField, HibImageVariantImpl variant, String keyForLogging, InternalActionContext ac, boolean throwOnAbsent) {
		HibBinary binary = binaryField.getBinary();
		if (variant == null) {
			if (throwOnAbsent) {
				throw error(BAD_REQUEST, "Requested variant `{}` of field `{}` not found", keyForLogging, binaryField.getFieldKey());
			} else {
				log.info("Requested variant {} of binary {} is not found at the binary field {}", keyForLogging, binary.getBinaryDataId(), binaryField.getFieldKey());
				return;
			}
		}
		HibBinaryFieldBase base = (HibBinaryFieldBase) binaryField;
		variant.removeField(base.getEdge(), throwOnAbsent);
		em().merge(variant);
		em().refresh(((HibBinaryFieldBase)binaryField).getEdge());
	}

	/**
	 * Make a HQL retrieval query from the image manipulation request.
	 * 
	 * @param variant
	 * @param queryName
	 * @return
	 */
	protected TypedQuery<HibImageVariantImpl> queryFromManipulationRequest(ImageManipulation variant, String queryName, boolean noAuto) {
		Integer width = null;
		Integer height = null;
		boolean auto = false;
		if (StringUtils.isNotBlank(variant.getWidth())) {
			if ("auto".equals(variant.getWidth())) {
				auto = true;
			} else {
				width = Integer.parseInt(variant.getWidth());
			}
		}
		if (StringUtils.isNotBlank(variant.getHeight())) {
			if ("auto".equals(variant.getHeight())) {
				auto = true;
			} else {
				height = Integer.parseInt(variant.getHeight());
			}
		}
		Float fpx = null;
		Float fpy = null;
		if (variant.hasFocalPoint()) {
			FocalPoint point = variant.getFocalPoint();
			fpx = point.getX();
			fpy = point.getY();
		}
		Integer cropX = null;
		Integer cropY = null;
		Integer cropW = null;
		Integer cropH = null;
		if (variant.getRect() != null) {
			cropX = variant.getRect().getStartX();
			cropY = variant.getRect().getStartY();
		}
		TypedQuery<HibImageVariantImpl> query = em().createNamedQuery(queryName, HibImageVariantImpl.class)
				.setParameter("fpx", fpx)
				.setParameter("fpy", fpy)
				.setParameter("fpz", variant.getFocalPointZoom())
				.setParameter("cropX", cropX)
				.setParameter("cropY", cropY)
				.setParameter("cropWidth", cropW)
				.setParameter("cropHeight", cropH)
				.setParameter("cropMode", variant.getCropMode())
				.setParameter("resizeMode", variant.getResizeMode());
		if (width != null & height != null) {
			query.setParameter("width", width).setParameter("height", height);
		} else if (width != null) {
			query.setParameter("width", width);
		} else if (height != null) {
			query.setParameter("height", height);
		}
		if (!noAuto) {
			query.setParameter("auto", auto);
		}
		return query.setMaxResults(1);
	}
}
