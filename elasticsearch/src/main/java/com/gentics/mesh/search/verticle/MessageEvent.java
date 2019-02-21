package com.gentics.mesh.search.verticle;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshEventModel;

public class MessageEvent {
	public final MeshEventModel message;
	public final MeshEvent event;

	public MessageEvent(MeshEvent event, MeshEventModel message) {
		this.event = event;
		this.message = message;
	}
}
