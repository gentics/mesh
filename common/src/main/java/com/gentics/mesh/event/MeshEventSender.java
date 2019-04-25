package com.gentics.mesh.event;

import com.gentics.mesh.core.rest.MeshEvent;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.eventbus.MessageConsumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.gentics.mesh.core.rest.MeshEvent.IS_SEARCH_IDLE;
import static com.gentics.mesh.core.rest.MeshEvent.SEARCH_FLUSH_REQUEST;
import static com.gentics.mesh.core.rest.MeshEvent.SEARCH_REFRESH_REQUEST;

/**
 * Makes it easier to emit certain events and to handle their replies.
 */
@Singleton
public final class MeshEventSender {

	private final Vertx vertx;

	@Inject
	public MeshEventSender(Vertx vertx) {
		this.vertx = vertx;
	}

	/**
	 * Sends the IS_SEARCH_IDLE event and emits the reply.
	 * @return
	 */
	public Single<Boolean> isSearchIdle() {
		return vertx.eventBus().rxSend(IS_SEARCH_IDLE.address, null)
			.map(msg -> (Boolean) msg.body());
	}

	public Completable refreshSearch() {
		return vertx.eventBus().rxSend(SEARCH_REFRESH_REQUEST.address, null).toCompletable();
	}

	public void flushSearch() {
		vertx.eventBus().publish(SEARCH_FLUSH_REQUEST.address, null);
	}

	/**
	 * Completes when the event has been emitted.
	 * @return
	 */
	public Completable waitForEvent(MeshEvent event) {
		return Completable.create(sub -> {
			MessageConsumer<Object> consumer = vertx.eventBus().consumer(event.address, result -> {
				if (!sub.isDisposed()) {
					sub.onComplete();
				}
			});
			sub.setCancellable(consumer::unregister);
		});
	}
}
