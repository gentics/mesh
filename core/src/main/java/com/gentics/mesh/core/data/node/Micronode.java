package com.gentics.mesh.core.data.node;

import java.util.List;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.TransformableElement;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;

public interface Micronode extends GraphFieldContainer, MeshVertex, TransformableElement<MicronodeResponse> {

	public static final String TYPE = "micronode";

	/**
	 * Get the container of this micronode.
	 *
	 * @return container
	 */
	NodeGraphFieldContainer getContainer();

	/**
	 * Make this micronode a clone of the given micronode.
	 * Property Vertices are reused
	 *
	 * @param micronode micronode
	 */
	void clone(Micronode micronode);

	@Override
	MicroschemaContainerVersion getSchemaContainerVersion();

	List<FieldContainerChange> compareTo(Micronode micronodeB);

}
