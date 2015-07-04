package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.MicroschemaContainer;

public interface MicroschemaContainerRoot extends RootVertex<MicroschemaContainer> {

	void addMicroschema(MicroschemaContainer container);

	void removeMicroschema(MicroschemaContainer container);
	
	MicroschemaContainer create(String name);

}
