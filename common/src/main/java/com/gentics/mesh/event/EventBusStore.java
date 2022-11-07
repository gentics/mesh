package com.gentics.mesh.event;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.vertx.core.eventbus.EventBus;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Wraps the event bus in an observable.
 */
@Singleton
public class EventBusStore {
    private BehaviorSubject<EventBus> eventBus = BehaviorSubject.create();
    @Inject
    public EventBusStore() {
    }

    /**
     * The EventBus observable
     * @return
     */
    public Observable<EventBus> eventBus() {
        return eventBus;
    }

    /**
     * Set the event bus
     * @param eventBus
     */
    public void setEventBus(EventBus eventBus) {
        this.eventBus.onNext(eventBus);
    }

    /**
     * Get the current event bus (can be null)
     * @return
     */
    @Nullable
    public EventBus current() {
        return eventBus.getValue();
    }
}
