package com.gentics.mesh.core.data.service;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.ASSIGNED_TO_PROJECT;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.generic.MeshVertex;

@Component
public class MeshDatabaseService extends AbstractMeshService {

	public <T extends MeshVertex> T findByUUID(String projectName, String uuid, Class<T> classOfT) {
		return fg.v().has("uuid", uuid).mark().out(ASSIGNED_TO_PROJECT).has("name", projectName).back().nextOrDefault(classOfT, null);
	}

	public <T extends MeshVertex> T findByUUID(String uuid, Class<T> classOfT) {
		return fg.v().has("uuid", uuid).nextOrDefault(classOfT, null);
	}

}
