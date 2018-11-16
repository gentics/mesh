package com.gentics.mesh.example;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.MeshServerInfoModel;

import io.vertx.core.impl.launcher.commands.VersionCommand;

public class RestInfoExamples extends AbstractExamples {

	public MeshServerInfoModel getInfoExample() {
		MeshServerInfoModel info = new MeshServerInfoModel();
		info.setDatabaseVendor("orientdb");
		info.setDatabaseVersion("2.2.16");
		info.setSearchVendor("elasticsearch");
		info.setSearchVersion("2.4.3");
		info.setMeshVersion(Mesh.getPlainVersion());
		info.setMeshNodeName("Reminiscent Tirtouga");
		info.setVertxVersion(VersionCommand.getVersion());
		return info;
	}
}
