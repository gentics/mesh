package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.core.rest.event.MeshElementEventModel;

import java.util.Optional;
import java.util.function.Function;

public interface EventVertexMapper<T> extends Function<MeshElementEventModel, Optional<T>> {
}
