package com.gentics.mesh.core.rest.event.tagfamily;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.AbstractProjectEventModel;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.project.ProjectReference;

public class TagFamilyMeshEventModel extends AbstractProjectEventModel {

	public TagFamilyMeshEventModel(String origin, EventCauseInfo cause, MeshEvent event, String uuid, String name, ProjectReference project) {
		super(origin, cause, event, uuid, name, project);
	}


}
