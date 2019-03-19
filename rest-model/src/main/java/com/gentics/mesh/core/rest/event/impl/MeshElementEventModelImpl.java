package com.gentics.mesh.core.rest.event.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.AbstractElementMeshEventModel;
import com.gentics.mesh.core.rest.event.EventCauseInfo;

public class MeshElementEventModelImpl extends AbstractElementMeshEventModel {

	public static MeshElementEventModelImpl createMeshElementEventModelImpl(String origin, EventCauseInfo cause, MeshEvent event, String uuid,
		String name) {
		return new MeshElementEventModelImpl(origin, cause, event, uuid, name);
	}

	@JsonCreator
	private MeshElementEventModelImpl(String origin, EventCauseInfo cause, MeshEvent event, String uuid, String name) {
		super(origin, cause, event, uuid, name);
	}

}
