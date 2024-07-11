package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.binary.ImageVariant;
import com.gentics.mesh.core.data.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantRequest;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.parameter.image.ImageManipulation;
import com.google.common.base.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An extension to {@link ImageVariantDao} with access to the persisting level.
 * 
 * @author plyhun
 *
 */
public interface PersistingImageVariantDao extends ImageVariantDao {

	static final Logger log = LoggerFactory.getLogger(PersistingImageVariantDao.class);

	/**
	 * Create a database entity for image variant of the given binary.
	 * 
	 * @param binary
	 * @param inflater inflates the raw image variant
	 * @return
	 */
	ImageVariant createPersistedVariant(Binary binary, ImageVariantRequest variant, Consumer<ImageVariant> inflater);

	/**
	 * Delete the database entity of the given image variant of a given binary.
	 * 
	 * @param binary
	 * @param variant
	 * @param throwOnInUse throws an exception, if the variant is in use and cannot be deleted.
	 * @return if the variant has been deleted
	 */
	boolean deletePersistedVariant(Binary binary, ImageVariant variant, boolean throwOnInUse);

	/**
	 * Get all the variants of the given binary.
	 * 
	 * @param binary
	 * @param ac
	 * @return
	 */
	Result<? extends ImageVariant> getVariants(Binary binary, InternalActionContext ac);

	/**
	 * Find the existing binary variant.
	 * 
	 * @param binary
	 * @param request
	 * @param ac
	 * @return
	 */
	ImageVariant getVariant(Binary binary, ImageManipulation request, InternalActionContext ac);

	/**
	 * Remove duplicates from the variants collection.
	 * 
	 * @param variants
	 * @return
	 */
	default Collection<ImageVariantRequest> removeDuplicates(Collection<ImageVariantRequest> variants) {
		return variants.stream().collect(Collectors.toMap(ImageManipulation::getCacheKey, Function.identity(), (a, b) -> a)).values();
	}

	@Override
	default Result<? extends ImageVariant> createVariants(BinaryField binaryField, Collection<ImageVariantRequest> variantsToAdd, InternalActionContext ac, boolean deleteOtherVariants) {
		Binary binary = binaryField.getBinary();
		variantsToAdd = removeDuplicates(variantsToAdd);
		Collection<ImageVariantRequest> variantsToDelete;
		if (deleteOtherVariants) {
			variantsToDelete = matchVariants(binary, variantsToAdd, ac, true);
			detachVariants(binaryField, variantsToDelete, ac, false);
		} else {
			variantsToDelete = Collections.emptyList();
		}
		createVariants(binary, variantsToAdd, ac, deleteOtherVariants);
		attachVariants(binaryField, variantsToAdd, ac, false);
		if (!variantsToDelete.isEmpty()) {
			deleteVariants(binaryField, variantsToDelete, ac);
		}
		return binaryField.getImageVariants();
	}

	@Override
	default Result<? extends ImageVariant> deleteVariants(BinaryField binaryField, Collection<ImageVariantRequest> variantsToDelete, InternalActionContext ac) {
		Binary binary = binaryField.getBinary();
		variantsToDelete = removeDuplicates(variantsToDelete);
		Collection<ImageVariantRequest> variantsToDetach = matchVariants(binary, variantsToDelete, ac, false);
		Result<? extends ImageVariant> ret = detachVariants(binaryField, variantsToDetach, ac, false);
		deleteVariants(binary, variantsToDetach, ac, false);
		return ret;
	}

	@Override
	default Result<? extends ImageVariant> retainVariants(BinaryField binaryField, Collection<ImageVariantRequest> variantsToRetain, InternalActionContext ac) {
		Binary binary = binaryField.getBinary();
		variantsToRetain = removeDuplicates(variantsToRetain);
		Collection<ImageVariantRequest> variantsToDetach = matchVariants(binary, variantsToRetain, ac, true);
		Result<? extends ImageVariant> ret = detachVariants(binaryField, variantsToDetach, ac, false);
		deleteVariants(binary, variantsToDetach, ac, false);
		return ret;
	}

	default Collection<ImageVariantRequest> matchVariants(Binary binary, Collection<ImageVariantRequest> requests, InternalActionContext ac, boolean exclude) {
		if (!isImage(binary)) {
			throw error(BAD_REQUEST, "image_error_not_an_image");
		}
		Result<? extends ImageVariant> oldVariants = getVariants(binary, ac);
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

	/**
	 * Create image variants for the given binary.
	 * 
	 * @param binary
	 * @param requests
	 * @param ac
	 * @param deleteOtherVariants
	 * @return
	 */
	@SuppressWarnings("unchecked")
	default Result<? extends ImageVariant> createVariants(Binary binary, Collection<ImageVariantRequest> requests, InternalActionContext ac, boolean deleteOtherVariants) {
		if (!isImage(binary)) {
			throw error(BAD_REQUEST, "image_error_not_an_image");
		}
		Result<? extends ImageVariant> oldVariants = getVariants(binary, ac);
		Map<ImageVariantRequest, Optional<ImageVariant>> requestExistence = requests.stream()
				.map(request -> Pair.of(request, oldVariants.stream().filter(variant -> doesVariantMatchRequest(variant, request)).map(ImageVariant.class::cast).findAny()))
				.collect(Collectors.toMap(Pair::getKey, Pair::getValue, (a,b) -> a));

		List<ImageVariant> newVariants = requestExistence.entrySet().stream()
				.map(pair -> pair.getValue().orElseGet(() -> createVariant(binary, pair.getKey(), ac, false)))
				//.map(newVariant -> transformToRestSync(newVariant, ac, level))
				.collect(Collectors.toList());
		
		if (deleteOtherVariants) {
			List<ImageVariantRequest> toDelete = ((List<ImageVariant>) ListUtils.subtract(oldVariants.list(), newVariants)).stream().map(deletable -> transformToRestSync(deletable, ac, 0).toRequest()).collect(Collectors.toList());
			return deleteVariants(binary, toDelete, ac, false);
		} else {
			return new TraversalResult<>(ListUtils.sum(newVariants, oldVariants.list()));
		}
	}

	/**
	 * Delete image variants from the given binary.
	 * 
	 * @param binary
	 * @param requests
	 * @param ac
	 * @param throwOnInUse throws an exception if the requested variant cannot be deleted due to being in use by some other field
	 * @return
	 */
	default Result<? extends ImageVariant> deleteVariants(Binary binary, Collection<ImageVariantRequest> requests, InternalActionContext ac, boolean throwOnInUse) {
		if (!isImage(binary)) {
			throw error(BAD_REQUEST, "image_error_not_an_image");
		}
		requests.stream().forEach(request -> deleteVariant(binary, request, ac, false, throwOnInUse));
		return getVariants(binary, ac);
	}

	/**
	 * Retain image variants from the given binary, delete all other.
	 * 
	 * @param binary
	 * @param requests
	 * @param ac
	 * @param throwOnInUse throws an exception if the requested variant to delete cannot be deleted due to being in use by some other field
	 * @return
	 */
	default Result<? extends ImageVariant> retainVariants(Binary binary, Collection<ImageVariantRequest> requests, InternalActionContext ac, boolean throwOnInUse) {
		if (!isImage(binary)) {
			throw error(BAD_REQUEST, "image_error_not_an_image");
		}
		Result<? extends ImageVariant> oldVariants = getVariants(binary, ac);
		List<ImageVariantRequest> finalRequests = new ArrayList<>(requests);
		requests = oldVariants.stream()
				.filter(oldVariant -> finalRequests.stream().noneMatch(request -> doesVariantMatchRequest(oldVariant, request)))
				.map(oldVariant -> transformToRestSync(oldVariant, ac, 0).toRequest())
				.collect(Collectors.toList());
		requests.stream().forEach(request -> deleteVariant(binary, request, ac, false, throwOnInUse));
		return getVariants(binary, ac);
	}

	/**
	 * Transform an image variant to its REST representation.
	 */
	@Override
	default ImageVariantResponse transformToRestSync(ImageVariant element, InternalActionContext ac, int level,	String... languageTags) {
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

	/**
	 * Create a new image variant for the binary.
	 * 
	 * @param binary
	 * @param variant
	 * @param ac
	 * @param throwOnExisting
	 * @return
	 */
	default ImageVariant createVariant(Binary binary, ImageVariantRequest variant, InternalActionContext ac, boolean throwOnExisting) {
		return createPersistedVariant(binary, variant, entity -> {
			entity.fillFromManipulation(binary, variant);
		});
	}

	/**
	 * Delete unused image variant from the binary.
	 * 
	 * @param binary
	 * @param request
	 * @param ac
	 * @param throwOnAbsent
	 */
	default void deleteVariant(Binary binary, ImageVariantRequest request, InternalActionContext ac, boolean throwOnAbsent, boolean throwOnInUse) {
		Optional<? extends ImageVariant> maybeToDelete = getVariants(binary, ac).stream().filter(variant -> doesVariantMatchRequest(variant, request)).findAny();
		ImageVariant toDelete;
		if (throwOnAbsent) {
			toDelete = maybeToDelete.orElseThrow(() -> error(BAD_REQUEST, "image_error_no_variant", request.getCacheKey(), binary.getUuid()));
		} else {
			toDelete = maybeToDelete.orElse(null);
		}
		if (null != toDelete) {
			deletePersistedVariant(binary, toDelete, throwOnInUse);
		} else {
			log.warn("No image variant found for binary #" + binary.getUuid() + " / " + request.getCacheKey());
		}
	}

	/**
	 * Create image variant for the binary field.
	 */
	@Override
	default ImageVariant createVariant(BinaryField binaryField, ImageVariantRequest request, InternalActionContext ac, boolean throwOnExisting) {
		ImageVariant variant = createVariant(binaryField.getBinary(), request, ac, throwOnExisting);
		attachVariant(binaryField, request, ac, throwOnExisting);
		return variant;
	}

	/**
	 * Delete the image variant, used by this binary field and unused by everyone else.
	 */
	@Override
	default void deleteVariant(BinaryField binaryField, ImageVariantRequest request, InternalActionContext ac, boolean throwOnAbsent, boolean throwOnInUse) {
		detachVariant(binaryField, request, ac, throwOnAbsent);
		deleteVariant(binaryField.getBinary(), request, ac, throwOnAbsent, throwOnInUse);
	}

	/**
	 * Attach image variants to this field.
	 * 
	 * @param binaryField
	 * @param variants
	 * @param ac
	 * @param throwOnExisting
	 * @return
	 */
	default Result<? extends ImageVariant> attachVariants(BinaryField binaryField, Collection<ImageVariantRequest> variants, InternalActionContext ac, boolean throwOnExisting) {
		variants.stream().forEach(variant -> attachVariant(binaryField, variant, ac, throwOnExisting));
		return binaryField.getImageVariants();
	}

	/**
	 * Attach the image variant to this field.
	 * 
	 * @param binaryField
	 * @param variant
	 * @param ac
	 * @param throwOnExisting
	 */
	void attachVariant(BinaryField binaryField, ImageVariantRequest variant, InternalActionContext ac, boolean throwOnExisting);

	/**
	 * Detach image variants from this field. The variants are not deleted.
	 * 
	 * @param binaryField
	 * @param variants
	 * @param ac
	 * @param throwOnAbsent
	 * @return
	 */
	default Result<? extends ImageVariant> detachVariants(BinaryField binaryField, Collection<ImageVariantRequest> variants, InternalActionContext ac, boolean throwOnAbsent) {
		variants.stream().forEach(variant -> detachVariant(binaryField, variant, ac, throwOnAbsent));
		return binaryField.getImageVariants();
	}

	/**
	 * Detach the image variant from this field. The variant is not deleted.
	 * 
	 * @param binaryField
	 * @param variant
	 * @param ac
	 * @param throwOnAbsent
	 */
	void detachVariant(BinaryField binaryField, ImageVariantRequest variant, InternalActionContext ac, boolean throwOnAbsent);

	/**
	 * Check if an existing image variant matches the variant creation request.
	 * 
	 * @param variant
	 * @param request
	 * @return
	 */
	default boolean doesVariantMatchRequest(ImageVariant variant, ImageManipulation request) {
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
		} else if (!Objects.equal(variant.getWidth(), request.getWidth())) {
			return false;
		}
		if (request.getHeight() != null && variant.getHeight() != null) {
			if (!"auto".equals(request.getHeight())) {
				if (!request.getHeight().equals(String.valueOf(variant.getHeight()))) {
					return false;
				}
			} else if (!variant.isAuto()) {
				return false;
			}
		} else if (!Objects.equal(variant.getHeight(), request.getHeight())) {
			return false;
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
	default ImageVariantResponse transformBinaryToRestVariantSync(Binary binary, InternalActionContext ac, boolean fillFilesize) {
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
	static boolean isImage(Binary binary) {
		return binary.getImageSize() != null;
	}

}
