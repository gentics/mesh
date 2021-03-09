package com.gentics.mesh.core.data.node.field.nesting;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.MeshEdge;
import com.gentics.mesh.core.data.node.Micronode;

/**
 * A {@link MicronodeGraphField} is an {@link MeshEdge} which links a {@link GraphFieldContainer} to a {@link Micronode} vertex.
 */
public interface MicronodeGraphField extends HibMicronodeField, ListableReferencingGraphField, MeshEdge {

}
