package com.gentics.mesh.core.rest.node;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.field.image.ImageManipulationRequest;

/**
 * POJO for node publish request. Currently consists of image manipulation creation parameters.
 * 
 * @author plyhun
 *
 */
public class NodePublishRequest implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("If a binary image is to be published, this field contains a set of image manipulation variants to be created immediately")
	@JsonSerialize(contentAs = ImageManipulationRequest.class, keyAs = String.class)
	@JsonDeserialize(contentAs = ImageManipulationRequest.class, keyAs = String.class)
	private final Map<String, ImageManipulationRequest> imageVariants = new HashMap<>();

	public void addImageVariant(String binaryFieldKey, ImageManipulationRequest request) {
		imageVariants.put(binaryFieldKey, request);
	}

	public ImageManipulationRequest removeImageVariant(String binaryFieldKey) {
		return imageVariants.remove(binaryFieldKey);
	}

	public Map<String, ImageManipulationRequest> getImageVariants() {
		return imageVariants;
	}

	public void clearImageVariants() {
		imageVariants.clear();
	}
}
