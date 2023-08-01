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
	@JsonPropertyDescription("If a binary image is to be published, this field contains a set of image manipulation variants to be created immediately")
	private List<ImageManipulationCreationVariant> variants;

	@JsonProperty(required = false)
	@JsonPropertyDescription(
			"If a binary image is to be published, and variants provided, this flag states that all the existing variants, except the abovementioned, should be dropped."
			+ "If no variants are provided along with the flag, all the image manipulation variants are to be dropped.")
	private boolean deleteOther = false;

	public ImageManipulationRequest() {}

	/**
	 * Get the image manipulation variants.
	 * 
	 * @return
	 */
	public List<ImageManipulationCreationVariant> getVariants() {
		return variants;
	}

	/**
	 * Set the image manipulation variants.
	 * 
	 * @param variants
	 * @return
	 */
	public ImageManipulationRequest setVariants(List<ImageManipulationCreationVariant> variants) {
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
