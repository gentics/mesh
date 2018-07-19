package com.gentics.mesh.core.rest.node.field.binary;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Rest model which holds geolocation information.
 */
public class Location implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("GPS Longitude")
	private Double lon;

	@JsonProperty(required = true)
	@JsonPropertyDescription("GPS Latitude")
	private Double lat;

	@JsonProperty(required = false)
	@JsonPropertyDescription("GPS Altitude in meters over sea level.")
	private Integer alt;

	public Location() {
	}

	public Location(double lon, double lat) {
		this.lon = lon;
		this.lat = lat;
	}

	public Double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public Double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public Integer getAlt() {
		return alt;
	}

	public void setAlt(int alt) {
		this.alt = alt;
	}

	@Override
	public String toString() {
		return "[lon:" + lon + ", lat:" + lat + ", alt: " + alt + "]";
	}

	/**
	 * Check whether all needed fields (lon,lat) have been set.
	 * 
	 * @return
	 */
	@JsonIgnore
	public boolean isPresent() {
		return lon != null && lat != null;
	}

}
