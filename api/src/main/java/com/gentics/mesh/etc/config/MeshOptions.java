package com.gentics.mesh.etc.config;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.doc.GenerateDocumentation;

/**
 * Main mesh configuration POJO.
 */
@GenerateDocumentation
public class MeshOptions extends AbstractMeshOptions {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Graph database options.")
	private GraphStorageOptions storageOptions = new GraphStorageOptions();

	@JsonProperty("storage")
	public GraphStorageOptions getStorageOptions() {
		return this.storageOptions;
	}

	@Setter
	public AbstractMeshOptions setStorageOptions(GraphStorageOptions storageOptions) {
		this.storageOptions = storageOptions;
		return this;
	}

	/**
	 * Validate this and the nested options.
	 */
	public void validate() {
		if (getStorageOptions() != null) {
			getStorageOptions().validate(this);
		}
		if (getGraphQLOptions() != null) {
			getGraphQLOptions().validate(this);
		}
		Objects.requireNonNull(getNodeName(), "The node name must be specified.");
		if (getVersionPurgeMaxBatchSize() <= 0) {
			throw new IllegalArgumentException("versionPurgeMaxBatchSize must be positive.");
		}
		// TODO check for other invalid characters in node name
	}

	@Override
	public void validate(AbstractMeshOptions options) {
		validate();
	}

}
