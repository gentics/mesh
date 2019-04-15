package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.core.rest.event.MeshElementEventModel;

import java.util.Optional;
import java.util.function.Function;

/**
 * A function that maps an event model to an element
 * @param <T>
 */
public interface EventVertexMapper<T> extends Function<MeshElementEventModel, Optional<T>> {
}
