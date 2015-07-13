package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.rest.schema.MicroschemaResponse;

public interface MicroschemaContainerRoot extends RootVertex<MicroschemaContainer, MicroschemaResponse> {

	void addMicroschema(MicroschemaContainer container);

	void removeMicroschema(MicroschemaContainer container);

	MicroschemaContainer create(String name);

}
