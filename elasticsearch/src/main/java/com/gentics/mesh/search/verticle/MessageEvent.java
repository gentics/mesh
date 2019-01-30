package com.gentics.mesh.search.verticle;

import com.gentics.mesh.event.MeshEventModel;
import io.vertx.core.eventbus.Message;

public class MessageEvent {
	public final MeshEventModel message;
	public final String event;

	public MessageEvent(String event, MeshEventModel message) {
		this.event = event;
		this.message = message;
	}
}
