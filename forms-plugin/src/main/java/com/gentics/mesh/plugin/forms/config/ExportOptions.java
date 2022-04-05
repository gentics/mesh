package com.gentics.mesh.plugin.forms.config;

/**
 * Options for fine-grained control over exported data.
 */
public class ExportOptions {

	private boolean includeUuid = false;
	private boolean includeCreationTimestamp = false;

	/**
	 * Whether to include the node UUID in the export.
	 *
	 * @return Whether to include the node UUID in the export
	 */
	public boolean isIncludeUuid() {
		return includeUuid;
	}

	/**
	 * Set whether to include the node UUID in the export.
	 *
	 * @param includeUuid Whether to include the node UUID in the export
	 * @return Reference to this for fluent API access
	 */
	public ExportOptions setIncludeUuid(boolean includeUuid) {
		this.includeUuid = includeUuid;

		return this;
	}

	/**
	 * Whether to include the creation timestamp in the export.
	 *
	 * <p>
	 *     The timestamp will be added as a UNIX timestamp.
	 * </p>
	 *
	 * @return Whether to include the creation timestamp in the export
	 */
	 public boolean isIncludeCreationTimestamp() {
		return includeCreationTimestamp;
	}
	/**
	 * Set whether to include the creation timestamp of the data node.
	 *
	 * @param includeCreationTimestamp Whether to include the creation timestamp
	 * @return Reference to this for fluent API access
	 */
	public ExportOptions setIncludeCreationTimestamp(boolean includeCreationTimestamp) {
		this.includeCreationTimestamp = includeCreationTimestamp;

		return this;
	}
}
