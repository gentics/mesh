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
public class BinaryMetadataModel implements RestModel {

	@JsonIgnore
	Map<String, String> dynamicProperties = new HashMap<>();

	@JsonProperty(required = false)
	@JsonPropertyDescription("Geolocation information.")
	private LocationModel location;

	public BinaryMetadataModel() {
	}

	public LocationModel getLocation() {
		return location;
	}

	/**
	 * Set the location.
	 * 
	 * @param location
	 * @return Fluent API
	 */
	public BinaryMetadataModel setLocation(LocationModel location) {
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
	public BinaryMetadataModel setLocation(double lon, double lat) {
		setLocation(new LocationModel(lon, lat));
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
	public BinaryMetadataModel clear() {
		dynamicProperties.clear();
		location = null;
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BinaryMetadataModel) {
			BinaryMetadataModel metadata = (BinaryMetadataModel) obj;
			boolean sameLocation = Objects.equals(getLocation(), metadata.getLocation());
			boolean sameProperties = Objects.equals(getMap(), metadata.getMap());
			return sameLocation && sameProperties;
		} else {
			return false;
		}
	}
}
