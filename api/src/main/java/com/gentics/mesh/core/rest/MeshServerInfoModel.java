package com.gentics.mesh.core.rest;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Rest Model class which represents a server info response.
 */
public class MeshServerInfoModel implements RestModel {

	private String meshVersion;
	private String meshNodeId;

	private String databaseVendor;
	private String databaseVersion;

	private String searchVendor;
	private String searchVersion;

	private String vertxVersion;

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

	public String getMeshNodeId() {
		return meshNodeId;
	}

	public void setMeshNodeId(String meshNodeId) {
		this.meshNodeId = meshNodeId;
	}

}
