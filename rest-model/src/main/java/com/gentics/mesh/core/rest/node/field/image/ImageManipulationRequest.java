package com.gentics.mesh.core.rest.node.field.image;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for image manipulation creation request.
 * 
 * @author plyhun
 *
 */
public class ImageManipulationRequest implements RestModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("This field contains a set of image manipulation variants to be manipulated upon.")
	private List<ImageVariantRequest> variants;

	@JsonProperty(required = false)
	@JsonPropertyDescription(
			"This flag states that all the existing variants, except the mentioned in `variants` field, should be dropped."
			+ "If no variants are provided along with the flag, all the image manipulation variants are to be dropped.")
	private boolean deleteOther = false;

	public ImageManipulationRequest() {}

	/**
	 * Get the image manipulation variants.
	 * 
	 * @return
	 */
	public List<ImageVariantRequest> getVariants() {
		return variants;
	}

	/**
	 * Set the image manipulation variants.
	 * 
	 * @param variants
	 * @return
	 */
	public ImageManipulationRequest setVariants(List<ImageVariantRequest> variants) {
		this.variants = variants;
		return this;
	}

	/**
	 * Get the flag to delete other image manipulation variants.
	 * 
	 * @return
	 */
	public boolean isDeleteOther() {
		return deleteOther;
	}

	/**
	 * Set the flag to delete other image manipulation variants.
	 * 
	 * @param deleteOther
	 * @return
	 */
	public ImageManipulationRequest setDeleteOther(boolean deleteOther) {
		this.deleteOther = deleteOther;
		return this;
	}
}
