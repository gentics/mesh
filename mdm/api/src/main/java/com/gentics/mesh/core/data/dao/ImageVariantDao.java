package com.gentics.mesh.core.data.dao;

import java.util.Collection;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.binary.ImageVariant;
import com.gentics.mesh.core.data.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantRequest;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.parameter.image.ImageManipulation;

/**
 * Image variant data access contract.
 * 
 * @author plyhun
 *
 */
public interface ImageVariantDao extends Dao<ImageVariant>, DaoTransformable<ImageVariant, ImageVariantResponse> {

	/**
	 * Get the manipulation variants of this binary image representation. On any other binary data returns null.
	 * 
	 * @return
	 */
	Result<? extends ImageVariant> getVariants(BinaryField binaryField, InternalActionContext ac);

	/**
	 * Get the manipulation variants of this binary image representation. On any other binary data returns null.
	 * 
	 * @return
	 */
	ImageVariant getVariant(BinaryField binaryField, ImageManipulation variant, InternalActionContext ac);

	/**
	 * Create the image manipulation variant.
	 * 
	 * @param variant
	 * @param throwOnExisting throw an exception, if no requested variant exists
	 * @return created variants
	 */
	ImageVariant createVariant(BinaryField binaryField, ImageVariantRequest variant, InternalActionContext ac, boolean throwOnExisting);

	/**
	 * Delete the image manipulation variant.
	 * @param throwOnAbsent throws an exception if no requested variant found
	 * @param throwOnInUse throws an exception if the requested variant cannot be deleted due to being in use by some other field
	 * @param variant
	 */
	void deleteVariant(BinaryField binaryField, ImageVariantRequest variant, InternalActionContext ac, boolean throwOnAbsent, boolean throwOnInUse);

	/**
	 * Create the image manipulation variants, optionally deleting all other existing unused variants.
	 * 
	 * @param variants
	 * @param deleteOtherVariants
	 * @return image variants of the binary after the operation.
	 */
	Result<? extends ImageVariant> createVariants(BinaryField binaryField, Collection<ImageVariantRequest> variants, InternalActionContext ac, boolean deleteOtherVariants);

	/**
	 * Delete the requested image manipulation variants.
	 * 
	 * @param variant
	 * @return image variants of the binary after the operation.
	 */
	Result<? extends ImageVariant> deleteVariants(BinaryField binaryField, Collection<ImageVariantRequest> variant, InternalActionContext ac);

	/**
	 * Retain the requested image manipulation variants, delete all other.
	 * 
	 * @param variant
	 * @return image variants of the binary after the operation.
	 */
	Result<? extends ImageVariant> retainVariants(BinaryField binaryField, Collection<ImageVariantRequest> variant, InternalActionContext ac);
}
