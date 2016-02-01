package com.gentics.mesh.core.data.node;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.TransformableElement;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.schema.Microschema;

public interface Micronode extends GraphFieldContainer, MeshVertex, TransformableElement<MicronodeResponse> {

	public static final String TYPE = "micronode";

	/**
	 * Return the microschema container that holds the microschema that is used in combination with this micronode.
	 * 
	 * @return microschema container
	 */
	MicroschemaContainer getMicroschemaContainer();

	/**
	 * Set the microschema container that is used in combination with this micronode.
	 * 
	 * @param microschema
	 *            microschema container
	 */
	void setMicroschemaContainer(MicroschemaContainer microschema);

	/**
	 * Shortcut method for getMicroschemaContainer().getMicroschema()
	 * 
	 * @return microschema
	 */
	Microschema getMicroschema();

}
