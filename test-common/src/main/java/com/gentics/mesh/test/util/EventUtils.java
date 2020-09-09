package com.gentics.mesh.test.util;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshWebsocket;

import io.reactivex.Observable;

public final class EventUtils {
	private EventUtils() {
	}

	public static Observable<MeshElementEventModelImpl> userCreated(MeshRestClient client) {
		return listenForEvent(client, MeshEvent.USER_CREATED);
	}

	public static <T> Observable<T> listenForEvent(MeshRestClient client, MeshEvent event) {
		return Observable.using(client::eventbus, ws -> {
			ws.registerEvents(event);
			return Observable.merge(
				ws.events(),
				ws.errors().flatMap(Observable::error)
			);
		}, MeshWebsocket::close)
		.map(ev -> (T) JsonUtil.getMapper().treeToValue(ev.getBodyAsJson(), event.bodyModel));
	}
}
