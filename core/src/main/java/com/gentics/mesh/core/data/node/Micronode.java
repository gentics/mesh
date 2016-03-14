package com.gentics.mesh.core.data.node;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.TransformableElement;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.schema.Microschema;

public interface Micronode extends GraphFieldContainer, MeshVertex, TransformableElement<MicronodeResponse> {

	public static final String TYPE = "micronode";

	/**
	 * Return the microschema container version that holds the microschema that is used in combination with this micronode.
	 * 
	 * @return microschema container version
	 */
	MicroschemaContainerVersion getMicroschemaContainerVersion();

	/**
	 * Set the microschema container version that is used in combination with this micronode.
	 * 
	 * @param microschema
	 *            microschema container
	 */
	void setMicroschemaContainerVersion(MicroschemaContainerVersion microschema);

	/**
	 * Shortcut method for getMicroschemaContainerVersion().getMicroschema()
	 * 
	 * @return microschema
	 */
	@Deprecated
	Microschema getMicroschema();

	/**
	 * Get the container of this micronode.
	 *
	 * @return container
	 */
	NodeGraphFieldContainer getContainer();
}
