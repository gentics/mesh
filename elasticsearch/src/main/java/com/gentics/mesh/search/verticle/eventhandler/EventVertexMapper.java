package com.gentics.mesh.search.verticle.eventhandler;

import java.util.Optional;
import java.util.function.Function;

import com.gentics.mesh.core.rest.event.MeshElementEventModel;

interface EventVertexMapper<T> extends Function<MeshElementEventModel, Optional<T>> {
}
