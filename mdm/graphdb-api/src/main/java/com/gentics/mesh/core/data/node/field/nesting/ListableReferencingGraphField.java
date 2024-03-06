package com.gentics.mesh.core.data.node.field.nesting;

import com.gentics.mesh.core.data.MeshEdge;

/**
 * Listable referencing graph fields are fields which are stored using a graph edge instead of a vertex. Typical example fields are {@link NodeGraphField} and
 * {@link MicronodeGraphField} which are fields that use a edge to store the field data / reference.
 */
public interface ListableReferencingGraphField extends ListableGraphField, MeshEdge {

}
