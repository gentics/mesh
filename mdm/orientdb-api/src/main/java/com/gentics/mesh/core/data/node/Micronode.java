package com.gentics.mesh.core.data.node;

import java.util.List;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.TransformableElement;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.result.Result;

/**
 * A micronodes is similar to a node but instead of nodes these elements can't be directly accessed via the REST API. A micronode can have it's own set of
 * fields. This way a micronode acts like a container for certain fields. Micronodes can be used in combination with {@link MicronodeField}'s in a regular node.
 * It is important to note that micronodes themself can't have additional micronode fields.
 */
public interface Micronode extends GraphFieldContainer, MeshVertex, TransformableElement<MicronodeResponse> {

	public static final String TYPE = "micronode";

	/**
	 * Get the container of this micronode which can either be referenced via a micronode list or a directly to the container.
	 *
	 * @return container
	 */
	NodeGraphFieldContainer getContainer();

	
	/**
	 * Get the container of this micronode which can either be referenced via a micronode list or a directly to the container.
	 *
	 * @return container
	 */
	Result<? extends NodeGraphFieldContainer> getContainers();

	/**
	 * Make this micronode a clone of the given micronode. Property Vertices are reused
	 *
	 * @param micronode
	 *            micronode
	 */
	void clone(Micronode micronode);

	@Override
	MicroschemaVersion getSchemaContainerVersion();

	/**
	 * Compare the micronode and return a list of changes which identify the changes.
	 * 
	 * @param micronodeB
	 *            Micronode to compare with
	 * @return
	 */
	List<FieldContainerChange> compareTo(Micronode micronodeB);

}
