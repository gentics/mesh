package com.gentics.mesh.core.rest.node.field.binary;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
	}

	public Location getLocation() {
		return location;
	}

	/**
	 * Set the location.
	 * 
	 * @param location
	 * @return Fluent API
	 */
	public BinaryMetadata setLocation(Location location) {
		this.location = location;
		return this;
	}

	@JsonAnySetter
	public void add(String key, String value) {
		dynamicProperties.put(key, value);
	}

	@JsonAnyGetter
	public Map<String, String> getMap() {
		return dynamicProperties;
	}

	/**
	 * Set the location information.
	 * 
	 * @param lon
	 * @param lat
	 * @return Fluent API
	 */
	@JsonIgnore
	public BinaryMetadata setLocation(double lon, double lat) {
		setLocation(new Location(lon, lat));
		return this;
	}

	@JsonIgnore
	public String get(String key) {
		return dynamicProperties.get(key);
	}

	/**
	 * Remove all metadata information.
	 * 
	 * @return Fluent API
	 */
	@JsonIgnore
	public BinaryMetadata clear() {
		dynamicProperties.clear();
		location = null;
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BinaryMetadata) {
			BinaryMetadata metadata = (BinaryMetadata) obj;
			boolean sameLocation = Objects.equals(getLocation(), metadata.getLocation());
			boolean sameProperties = Objects.equals(getMap(), metadata.getMap());
			return sameLocation && sameProperties;
		} else {
			return false;
		}
	}
}
