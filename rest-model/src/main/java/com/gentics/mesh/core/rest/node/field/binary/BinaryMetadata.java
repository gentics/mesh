package com.gentics.mesh.core.rest.node.field.binary;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * RestModel for Metadata of binaries.
 */
public class BinaryMetadata implements RestModel {

	@JsonIgnore
	Map<String, String> dynamicProperties = new HashMap<>();

	@JsonProperty(required = false)
	@JsonPropertyDescription("Geolocation information.")
	private Location location;

	public BinaryMetadata() {
		// TODO Auto-generated constructor stub
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	@JsonAnySetter
	public void add(String key, String value) {
		dynamicProperties.put(key, value);
	}

	@JsonAnyGetter
	public Map<String, String> getMap() {
		return dynamicProperties;
	}

	@JsonIgnore
	public void setLocation(double lon, double lat) {
		setLocation(new Location(lon, lat));
	}
}
