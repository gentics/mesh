package com.gentics.mesh.core.binary.impl;

import java.util.Map;
import java.util.Optional;

import com.gentics.mesh.core.rest.node.field.binary.LocationModel;

/**
 * POJO for the result of a Tika parsing operation.
 */
public class TikaResult {

	private Optional<String> plainText;
	private Map<String, String> metadata;
	private LocationModel loc;

	public TikaResult(Map<String, String> metadata, Optional<String> plainText, LocationModel loc) {
		this.metadata = metadata;
		this.plainText = plainText;
		this.loc = loc;
	}

	public LocationModel getLoc() {
		return loc;
	}

	public Optional<String> getPlainText() {
		return plainText;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}
}
