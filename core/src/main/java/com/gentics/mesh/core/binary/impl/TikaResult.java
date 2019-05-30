package com.gentics.mesh.core.binary.impl;

import java.util.Map;
import java.util.Optional;

import com.gentics.mesh.core.rest.node.field.binary.Location;

public class TikaResult {

	private Optional<String> plainText;
	private Map<String, String> metadata;
	private Location loc;

	public TikaResult(Map<String, String> metadata, Optional<String> plainText, Location loc) {
		this.metadata = metadata;
		this.plainText = plainText;
		this.loc = loc;
	}

	public Location getLoc() {
		return loc;
	}

	public Optional<String> getPlainText() {
		return plainText;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}
}
