package com.gentics.mesh.search.verticle;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;

public class MessageEvent {
	public final MeshElementEventModel message;
	public final MeshEvent event;

	public MessageEvent(MeshEvent event, MeshElementEventModel message) {
		this.event = event;
		this.message = message;
	}
}
