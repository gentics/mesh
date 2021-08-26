package com.gentics.mesh.core.data.node;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.HibTransformableElement;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.result.Result;

public interface HibMicronode extends HibFieldContainer, HibBaseElement, HibTransformableElement<MicronodeResponse> {

	/**
	 * Get the container of this micronode which can either be referenced via a micronode list or a directly to the container.
	 *
	 * @return container
	 */
	HibNodeFieldContainer getContainer();

	/**
	 * Get all nodes that are in any way referenced by this node. This includes the following cases:
	 * <ul>
	 * <li>Node fields</li>
	 * <li>Node list fields</li>
	 * <li>Micronode fields with node fields or node list fields</li>
	 * <li>Micronode list fields with node fields or node list fields</li>
	 * </ul>
	 */
	Iterable<? extends HibNode> getReferencedNodes();

	/**
	 * Get the container of this micronode which can either be referenced via a micronode list or a directly to the container.
	 *
	 * @return container
	 */
	Result<? extends HibNodeFieldContainer> getContainers();

	/**
	 * Make this micronode a clone of the given micronode. Property Vertices are reused
	 *
	 * @param micronode
	 *            micronode
	 */
	void clone(HibMicronode micronode);

	HibMicroschemaVersion getSchemaContainerVersion();

	/**
	 * Compare the micronode and return a list of changes which identify the changes.
	 * 
	 * @param micronodeB
	 *            Micronode to compare with
	 * @return
	 */
	List<FieldContainerChange> compareTo(HibMicronode micronodeB);

	/**
	 * Micronodes don't provide a dedicated API path since those can't be directly accessed via REST URI.
	 * 
	 * @param ac
	 */
	@Override
	default String getAPIPath(InternalActionContext ac) {
		// Micronodes have no public location
		return null;
	}
}
