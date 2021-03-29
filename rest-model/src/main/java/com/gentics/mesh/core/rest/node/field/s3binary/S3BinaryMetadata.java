package com.gentics.mesh.core.rest.node.field.s3binary;

import com.fasterxml.jackson.annotation.*;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.field.binary.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * RestModel for Metadata of s3binaries.
 */
public class S3BinaryMetadata implements RestModel {

	@JsonIgnore
	Map<String, String> dynamicProperties = new HashMap<>();

	@JsonProperty(required = false)
	@JsonPropertyDescription("Geolocation information.")
	private Location location;

	public S3BinaryMetadata() {
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
	public S3BinaryMetadata setLocation(Location location) {
		this.location = location;
		return this;
	}

	/**
	 * Add the metadata pair
	 * 
	 * @param key
	 * @param value
	 */
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
	public S3BinaryMetadata setLocation(double lon, double lat) {
		setLocation(new Location(lon, lat));
		return this;
	}

	/**
	 * Return the metadata value for the key.
	 * 
	 * @param key
	 * @return
	 */
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
	public S3BinaryMetadata clear() {
		dynamicProperties.clear();
		location = null;
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof S3BinaryMetadata) {
			S3BinaryMetadata metadata = (S3BinaryMetadata) obj;
			boolean sameLocation = Objects.equals(getLocation(), metadata.getLocation());
			boolean sameProperties = Objects.equals(getMap(), metadata.getMap());
			return sameLocation && sameProperties;
		} else {
			return false;
		}
	}
}
