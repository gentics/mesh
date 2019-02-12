package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.event.MeshEventModel;

import java.util.Optional;
import java.util.function.Function;

interface EventVertexMapper<T extends MeshCoreVertex<? extends RestModel, T>> extends Function<MeshEventModel, Optional<T>> {
}
