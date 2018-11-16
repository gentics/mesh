package com.gentics.mesh.core.rest;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Rest Model class which represents a server info response.
 */
public class MeshServerInfoModel implements RestModel {

	@JsonPropertyDescription("Gentics Mesh Version string.")
	private String meshVersion;

	@JsonPropertyDescription("Node name of the Gentics Mesh instance.")
	private String meshNodeName;

	@JsonPropertyDescription("Used database implementation vendor name.")
	private String databaseVendor;

	@JsonPropertyDescription("Used database implementation version.")
	private String databaseVersion;

	@JsonPropertyDescription("Used search implementation vendor name.")
	private String searchVendor;

	@JsonPropertyDescription("Used search implementation version.")
	private String searchVersion;

	@JsonPropertyDescription("Used Vert.x version.")
	private String vertxVersion;

	@JsonPropertyDescription("Database structure revision hash.")
	private String databaseRevision;

	public MeshServerInfoModel() {
	}

	public String getMeshVersion() {
		return meshVersion;
	}

	public void setMeshVersion(String meshVersion) {
		this.meshVersion = meshVersion;
	}

	public String getDatabaseVendor() {
		return databaseVendor;
	}

	public void setDatabaseVendor(String databaseVendor) {
		this.databaseVendor = databaseVendor;
	}

	public String getDatabaseVersion() {
		return databaseVersion;
	}

	public void setDatabaseVersion(String databaseVersion) {
		this.databaseVersion = databaseVersion;
	}

	public String getSearchVendor() {
		return searchVendor;
	}

	public void setSearchVendor(String searchVendor) {
		this.searchVendor = searchVendor;
	}

	public String getSearchVersion() {
		return searchVersion;
	}

	public void setSearchVersion(String searchVersion) {
		this.searchVersion = searchVersion;
	}

	public String getVertxVersion() {
		return vertxVersion;
	}

	public void setVertxVersion(String vertxVersion) {
		this.vertxVersion = vertxVersion;
	}

	public String getMeshNodeName() {
		return meshNodeName;
	}

	public void setMeshNodeName(String meshNodeName) {
		this.meshNodeName = meshNodeName;
	}

	public String getDatabaseRevision() {
		return databaseRevision;
	}

	public void setDatabaseRevision(String databaseRevision) {
		this.databaseRevision = databaseRevision;
	}

}
