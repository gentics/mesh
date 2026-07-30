package com.gentics.mesh.example;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.util.RxUtil;

public class RestInfoExamples extends AbstractExamples {

	public MeshServerInfoModel getInfoExample() {
		MeshServerInfoModel info = new MeshServerInfoModel();
		info.setDatabaseVendor("mariadb");
		info.setDatabaseVersion("10.7");
		info.setSearchVendor("elasticsearch");
		info.setSearchVersion("2.4.3");
		info.setMeshVersion(Mesh.getPlainVersion());
		info.setMeshNodeName("Reminiscent Tirtouga");
		info.setVertxVersion(RxUtil.getVertxVersion());
		return info;
	}
}
