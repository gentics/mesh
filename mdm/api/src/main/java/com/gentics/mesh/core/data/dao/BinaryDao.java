package com.gentics.mesh.core.data.dao;

import java.io.InputStream;
import java.util.Collection;
import java.util.stream.Stream;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBinaryDataElement;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.binary.HibImageVariant;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.db.Supplier;
import com.gentics.mesh.core.db.Transactional;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantRequest;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.parameter.image.ImageManipulation;
import com.gentics.mesh.util.UUIDUtil;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

/**
 * DAO for {@link HibBinary}.
 */
public interface BinaryDao extends Dao<HibBinary>, DaoTransformable<HibImageVariant, ImageVariantResponse> {

	/**
	 * Return the binary data stream.
	 *
	 * @return
	 */
	Flowable<Buffer> getStream(HibBinaryDataElement binary);

	/**
	 * Return the data as base 64 encoded string in the same thread blockingly.
	 *
	 * @return
	 */
	String getBase64ContentSync(HibBinary binary);

	/**
	 * Find all binary fields which make use of this binary.
	 *
	 * @return
	 */
	Result<? extends HibBinaryField> findFields(HibBinary binary);

	/**
	 * Opens a blocking {@link InputStream} to the binary file. This should only be used for some other blocking APIs (i.e. ImageIO)
	 *
	 * @return
	 */
	Supplier<InputStream> openBlockingStream(HibBinary binary);

	/**
	 * Find the binary with the given hashsum.
	 *
	 * @param hash
	 * @return
	 */
	Transactional<? extends HibBinary> findByHash(String hash);

	/**
	 * Find the binaries with the specified check status.
	 *
	 * @param checkStatus The check status to filter for.
	 * @return A stream of matching binaries.
	 */
	Transactional<Stream<? extends HibBinary>> findByCheckStatus(BinaryCheckStatus checkStatus);

	/**
	 * Create a new binary.
	 *
	 * @param uuid
	 *            Uuid of the binary
	 * @param hash
	 *            Hash sum of the binary
	 * @param size
	 *            Size in bytes
	 * @return
	 */
	Transactional<? extends HibBinary> create(String uuid, String hash, Long size, BinaryCheckStatus checkStatus);

	/**
	 * Create a new binary.
	 *
	 * @param hash
	 * @param size
	 * @return
	 */
	default Transactional<? extends HibBinary> create(String hash, long size, BinaryCheckStatus checkStatus) {
		return create(UUIDUtil.randomUUID(), hash, size, checkStatus);
	}

	/**
	 * Return a stream of binaries.
	 *
	 * @return
	 */
	Transactional<Stream<HibBinary>> findAll();

	/**
	 * Get the manipulation variants of this binary image representation. On any other binary data returns null.
	 * 
	 * @return
	 */
	Result<? extends HibImageVariant> getVariants(HibBinary binary, InternalActionContext ac);

	/**
	 * Get the manipulation variants of this binary image representation. On any other binary data returns null.
	 * 
	 * @return
	 */
	HibImageVariant getVariant(HibBinary binary, ImageManipulation variant, InternalActionContext ac);

	/**
	 * Create the image manipulation variant,
	 * 
	 * @param variant
	 * @return created variant
	 */
	HibImageVariant createVariant(HibBinary binary, ImageVariantRequest variant, InternalActionContext ac, boolean throwOnExisting);

	/**
	 * Delete the image manipulation variant,
	 * 
	 * @param variant
	 */
	void deleteVariant(HibBinary binary, ImageVariantRequest variant, InternalActionContext ac, boolean throwOnAbsent);

	/**
	 * Create the image manipulation variants, optionally deleting all other existing unused variants.
	 * 
	 * @param variants
	 * @param deleteOtherVariants
	 * @return image variants of the binary after the operation.
	 */
	Result<? extends HibImageVariant> createVariants(HibBinary binary, Collection<ImageVariantRequest> variants, InternalActionContext ac, boolean deleteOtherVariants);

	/**
	 * Delete the requested image manipulation variants.
	 * 
	 * @param variant
	 * @return image variants of the binary after the operation.
	 */
	Result<? extends HibImageVariant> deleteVariants(HibBinary binary, Collection<ImageVariantRequest> variant, InternalActionContext ac);

	/**
	 * Retain the requested image manipulation variants, delete all other.
	 * 
	 * @param variant
	 * @return image variants of the binary after the operation.
	 */
	Result<? extends HibImageVariant> retainVariants(HibBinary binary, Collection<ImageVariantRequest> variant, InternalActionContext ac);
}
