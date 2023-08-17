package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBinaryDataElement;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.binary.HibImageVariant;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.db.Supplier;
import com.gentics.mesh.core.db.Transactional;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantRequest;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.parameter.image.ImageManipulation;
import com.google.common.base.Objects;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Persistence-aware extension to {@link BinaryDao}
 *
 * @author plyhun
 *
 */
public interface PersistingBinaryDao extends BinaryDao {

	static final Logger log = LoggerFactory.getLogger(PersistingBinaryDao.class);

	Base64.Encoder BASE64 = Base64.getEncoder();

	/**
	 * Get a binary storage implementation.
	 *
	 * @return
	 */
	Binaries binaries();

	/**
	 * Create a database entity for image variant of the given binary.
	 * 
	 * @param binary
	 * @param inflater inflates the raw image variant
	 * @return
	 */
	HibImageVariant createPersistedVariant(HibBinary binary, ImageVariantRequest variant, Consumer<HibImageVariant> inflater);

	/**
	 * Delete the database entity of the given image variant of a given binary.
	 * 
	 * @param binary
	 * @param variant
	 */
	void deletePersistedVariant(HibBinary binary, HibImageVariant variant);

	/**
	 * Get all the variants of the given binary.
	 * 
	 * @param binary
	 * @param ac
	 * @return
	 */
	Result<? extends HibImageVariant> getVariants(HibBinary binary, InternalActionContext ac);

	/**
	 * Find the existing binary variant.
	 * 
	 * @param binary
	 * @param request
	 * @param ac
	 * @return
	 */
	HibImageVariant getVariant(HibBinary binary, ImageManipulation request, InternalActionContext ac);

	@Override
	default Result<? extends HibImageVariant> createVariants(HibBinaryField binaryField, Collection<ImageVariantRequest> variantsToAdd, InternalActionContext ac, boolean deleteOtherVariants) {
		HibBinary binary = binaryField.getBinary();
		if (deleteOtherVariants) {
			Collection<ImageVariantRequest> variantsToDetach = matchVariants(binary, variantsToAdd, ac, true);
			detachVariants(binaryField, variantsToDetach, ac, false);
		}
		createVariants(binary, variantsToAdd, ac, deleteOtherVariants);
		attachVariants(binaryField, variantsToAdd, ac, false);
		return binaryField.getImageVariants();
	}

	@Override
	default Result<? extends HibImageVariant> deleteVariants(HibBinaryField binaryField, Collection<ImageVariantRequest> variantsToDelete, InternalActionContext ac) {
		HibBinary binary = binaryField.getBinary();
		Collection<ImageVariantRequest> variantsToDetach = matchVariants(binary, variantsToDelete, ac, false);
		detachVariants(binaryField, variantsToDetach, ac, false);
		return deleteVariants(binary, variantsToDetach, ac);
	}

	@Override
	default Result<? extends HibImageVariant> retainVariants(HibBinaryField binaryField, Collection<ImageVariantRequest> variantsToRetain, InternalActionContext ac) {
		HibBinary binary = binaryField.getBinary();
		Collection<ImageVariantRequest> variantsToDetach = matchVariants(binary, variantsToRetain, ac, true);
		detachVariants(binaryField, variantsToDetach, ac, false);
		return retainVariants(binary, variantsToDetach, ac);
	}

	default Collection<ImageVariantRequest> matchVariants(HibBinary binary, Collection<ImageVariantRequest> requests, InternalActionContext ac, boolean exclude) {
		if (!isImage(binary)) {
			throw error(BAD_REQUEST, "image_error_not_an_image");
		}
		Result<? extends HibImageVariant> oldVariants = getVariants(binary, ac);
		List<ImageVariantRequest> finalRequests = new ArrayList<>(requests);
		if (exclude) {
			return oldVariants.stream()
					.filter(oldVariant -> finalRequests.stream().noneMatch(request -> doesVariantMatchRequest(oldVariant, request)))
					.map(oldVariant -> transformToRestSync(oldVariant, ac, 0).toRequest())
					.collect(Collectors.toList());
		} else {
			return oldVariants.stream()
					.filter(oldVariant -> finalRequests.stream().anyMatch(request -> doesVariantMatchRequest(oldVariant, request)))
					.map(oldVariant -> transformToRestSync(oldVariant, ac, 0).toRequest())
					.collect(Collectors.toList());
		}
	}

	@Override
	default Transactional<? extends HibBinary> findByHash(String hash) {
		return binaries().findByHash(hash);
	}

	@Override
	default Transactional<Stream<? extends HibBinary>> findByCheckStatus(BinaryCheckStatus checkStatus) {
		return binaries().findByCheckStatus(checkStatus);
	}

	@Override
	default Transactional<? extends HibBinary> create(String uuid, String hash, Long size, BinaryCheckStatus checkStatus) {
		return binaries().create(uuid, hash, size, checkStatus);
	}

	@Override
	default Transactional<Stream<HibBinary>> findAll() {
		return binaries().findAll();
	}

	@Override
	default Supplier<InputStream> openBlockingStream(HibBinary binary) {
		return binary.openBlockingStream();
	}

	@Override
	default Flowable<Buffer> getStream(HibBinaryDataElement binary) {
		BinaryStorage storage = Tx.get().data().binaryStorage();
		return storage.read(binary.getUuid());
	}

	@Override
	default String getBase64ContentSync(HibBinary binary) {
		Buffer buffer = Tx.get().data().binaryStorage().readAllSync(binary.getUuid());
		return BASE64.encodeToString(buffer.getBytes());
	}

	@SuppressWarnings("unchecked")
	default Result<? extends HibImageVariant> createVariants(HibBinary binary, Collection<ImageVariantRequest> requests, InternalActionContext ac, boolean deleteOtherVariants) {
		if (!isImage(binary)) {
			throw error(BAD_REQUEST, "image_error_not_an_image");
		}
		Result<? extends HibImageVariant> oldVariants = getVariants(binary, ac);
		Map<ImageVariantRequest, Optional<HibImageVariant>> requestExistence = requests.stream()
				.map(request -> Pair.of(request, oldVariants.stream().filter(variant -> doesVariantMatchRequest(variant, request)).map(HibImageVariant.class::cast).findAny()))
				.collect(Collectors.toMap(Pair::getKey, Pair::getValue, (a,b) -> a));

		List<HibImageVariant> newVariants = requestExistence.entrySet().stream()
				.map(pair -> pair.getValue().orElseGet(() -> createVariant(binary, pair.getKey(), ac, false)))
				//.map(newVariant -> transformToRestSync(newVariant, ac, level))
				.collect(Collectors.toList());
		
		if (deleteOtherVariants) {
			List<ImageVariantRequest> toDelete = ((List<HibImageVariant>) ListUtils.subtract(oldVariants.list(), newVariants)).stream().map(deletable -> transformToRestSync(deletable, ac, 0).toRequest()).collect(Collectors.toList());
			return deleteVariants(binary, toDelete, ac);
		} else {
			return new TraversalResult<>(ListUtils.sum(newVariants, oldVariants.list()));
		}
	}

	default Result<? extends HibImageVariant> deleteVariants(HibBinary binary, Collection<ImageVariantRequest> requests, InternalActionContext ac) {
		if (!isImage(binary)) {
			throw error(BAD_REQUEST, "image_error_not_an_image");
		}
		requests.stream().forEach(request -> deleteVariant(binary, request, ac, false));
		return getVariants(binary, ac);
	}

	default Result<? extends HibImageVariant> retainVariants(HibBinary binary, Collection<ImageVariantRequest> requests, InternalActionContext ac) {
		if (!isImage(binary)) {
			throw error(BAD_REQUEST, "image_error_not_an_image");
		}
		Result<? extends HibImageVariant> oldVariants = getVariants(binary, ac);
		List<ImageVariantRequest> finalRequests = new ArrayList<>(requests);
		requests = oldVariants.stream()
				.filter(oldVariant -> finalRequests.stream().noneMatch(request -> doesVariantMatchRequest(oldVariant, request)))
				.map(oldVariant -> transformToRestSync(oldVariant, ac, 0).toRequest())
				.collect(Collectors.toList());
		requests.stream().forEach(request -> deleteVariant(binary, request, ac, false));
		return getVariants(binary, ac);
	}

	default ImageVariantResponse transformToRestSync(HibImageVariant element, InternalActionContext ac, int level,	String... languageTags) {
		ImageVariantResponse response = new ImageVariantResponse()
			.setWidth(element.getWidth())
			.setHeight(element.getHeight())
			.setAuto(element.isAuto())
			.setCropMode(element.getCropMode())
			.setFocalPoint(element.getFocalPoint())
			.setFocalZoom(element.getFocalPointZoom())
			.setOrigin(false)
			.setRect(element.getCropRect())
			.setResizeMode(element.getResizeMode());

		if (level > 0) {
			response.setFileSize(element.getSize());
		}
		return response;
	}

	default HibImageVariant createVariant(HibBinary binary, ImageVariantRequest variant, InternalActionContext ac, boolean throwOnExisting) {
		return createPersistedVariant(binary, variant, entity -> {
			entity.fillFromManipulation(binary, variant);
		});
	}

	default void deleteVariant(HibBinary binary, ImageVariantRequest request, InternalActionContext ac, boolean throwOnAbsent) {
		Optional<? extends HibImageVariant> maybeToDelete = getVariants(binary, ac).stream().filter(variant -> doesVariantMatchRequest(variant, request)).findAny();
		HibImageVariant toDelete;
		if (throwOnAbsent) {
			// TODO own error
			toDelete = maybeToDelete.orElseThrow(() -> error(BAD_REQUEST, "No image variant found for binary #" + binary.getUuid() + " / " + request.getCacheKey()));
		} else {
			toDelete = maybeToDelete.orElse(null);
		}
		if (null != toDelete) {
			deletePersistedVariant(binary, toDelete);
		} else {
			log.warn("No image variant found for binary #" + binary.getUuid() + " / " + request.getCacheKey());
		}
	}

	default Result<? extends HibImageVariant> attachVariants(HibBinaryField binaryField, Collection<ImageVariantRequest> variants, InternalActionContext ac, boolean throwOnExisting) {
		variants.stream().forEach(variant -> attachVariant(binaryField, variant, ac, throwOnExisting));
		return binaryField.getImageVariants();
	}

	void attachVariant(HibBinaryField binaryField, ImageVariantRequest variant, InternalActionContext ac, boolean throwOnExisting);

	default Result<? extends HibImageVariant> detachVariants(HibBinaryField binaryField, Collection<ImageVariantRequest> variants, InternalActionContext ac, boolean throwOnAbsent) {
		variants.stream().forEach(variant -> detachVariant(binaryField, variant, ac, throwOnAbsent));
		return binaryField.getImageVariants();
	}

	void detachVariant(HibBinaryField binaryField, ImageVariantRequest variant, InternalActionContext ac, boolean throwOnAbsent);

	/**
	 * Check if an existing image variant matches the variant creation request.
	 * 
	 * @param variant
	 * @param request
	 * @return
	 */
	default boolean doesVariantMatchRequest(HibImageVariant variant, ImageManipulation request) {
		if (!Objects.equal(variant.getFocalPointZoom(), request.getFocalPointZoom())) {
			return false;
		}
		if (!Objects.equal(variant.getCropMode(), request.getCropMode())) {
			return false;
		}
		if (!Objects.equal(variant.getResizeMode(), request.getResizeMode())) {
			return false;
		}
		if (!Objects.equal(variant.hasFocalPoint(), request.hasFocalPoint())) {
			return false;
		} else if (variant.hasFocalPoint() && request.hasFocalPoint() && !Objects.equal(variant.getFocalPoint(), request.getFocalPoint())) {
			return false;
		}
		if (request.getRect() != null && !Objects.equal(variant.getCropRect(), request.getRect())) {
			return false;
		}
		if (request.getWidth() != null && variant.getWidth() != null) {
			if (!"auto".equals(request.getWidth())) {
				if (!request.getWidth().equals(String.valueOf(variant.getWidth()))) {
					return false;
				}
			} else if (!variant.isAuto()) {
				return false;
			}
		}
		if (request.getHeight() != null && variant.getHeight() != null) {
			if (!"auto".equals(request.getHeight())) {
				if (!request.getHeight().equals(String.valueOf(variant.getHeight()))) {
					return false;
				}
			} else if (!variant.isAuto()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Transform an original binary into its variant REST model
	 * 
	 * @param binary
	 * @param ac
	 * @param fillFilesize
	 * @return
	 */
	default ImageVariantResponse transformBinaryToRestVariantSync(HibBinary binary, InternalActionContext ac, boolean fillFilesize) {
		ImageVariantResponse response = new ImageVariantResponse()
				.setWidth(binary.getImageWidth())
				.setHeight(binary.getImageHeight())
				.setAuto(false)
				.setCropMode(null)
				.setFocalPoint(null)
				.setFocalZoom(null)
				.setOrigin(true)
				.setRect(null)
				.setResizeMode(null);

			if (fillFilesize) {
				response.setFileSize(binary.getSize());
			}
			return response;
	}

	/**
	 * Check if binary is a graphic image.
	 * 
	 * @param binary
	 * @return
	 */
	static boolean isImage(HibBinary binary) {
		return binary.getImageSize() != null;
	}
}
