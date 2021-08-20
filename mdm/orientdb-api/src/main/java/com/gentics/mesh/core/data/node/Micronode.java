package com.gentics.mesh.core.data.node;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.rest.node.field.MicronodeField;

/**
 * A micronodes is similar to a node but instead of nodes these elements can't be directly accessed via the REST API. A micronode can have it's own set of
 * fields. This way a micronode acts like a container for certain fields. Micronodes can be used in combination with {@link MicronodeField}'s in a regular node.
 * It is important to note that micronodes themself can't have additional micronode fields.
 */
public interface Micronode extends HibMicronode, GraphFieldContainer, MeshVertex {

	public static final String TYPE = "micronode";
}
