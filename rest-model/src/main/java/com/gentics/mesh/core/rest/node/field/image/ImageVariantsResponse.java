package com.gentics.mesh.core.rest.node.field.image;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * A list of stored image variants.
 * 
 * @author plyhun
 *
 */
public class ImageVariantsResponse implements RestModel {

	@JsonPropertyDescription("List of image manipulation variants.")
	private List<ImageVariantResponse> variants;

	/**
	 * REST ctor.
	 */
	public ImageVariantsResponse() {
	}

	/**
	 * Parameterized ctor.
	 * 
	 * @param variants
	 */
	public ImageVariantsResponse(List<ImageVariantResponse> variants) {
		this.variants = variants;
	}

	public List<ImageVariantResponse> getVariants() {
		return variants;
	}

	public ImageVariantsResponse setVariants(List<ImageVariantResponse> variants) {
		this.variants = variants;
		return this;
	}

	/**
	 * Make a {@link ImageManipulationRequest} out of this response.
	 * 
	 * @return
	 */
	public ImageManipulationRequest toRequest() {
		return new ImageManipulationRequest()
				.setDeleteOther(false)
				.setVariants(variants == null ? null : variants.stream().map(ImageVariantResponse::toRequest).collect(Collectors.toList()));
	}
}
