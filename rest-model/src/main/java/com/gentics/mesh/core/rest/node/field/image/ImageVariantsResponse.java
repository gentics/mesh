package com.gentics.mesh.core.rest.node.field.image;

import java.util.List;

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
	private List<ImageVariant> variants;

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
	public ImageVariantsResponse(List<ImageVariant> variants) {
		this.variants = variants;
	}

	public List<ImageVariant> getVariants() {
		return variants;
	}

	public ImageVariantsResponse setVariants(List<ImageVariant> variants) {
		this.variants = variants;
		return this;
	}
}
