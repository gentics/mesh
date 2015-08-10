package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.User;

public interface MicroschemaContainerRoot extends RootVertex<MicroschemaContainer> {

	public static final String TYPE = "microschemas";

	void addMicroschema(MicroschemaContainer container);

	void removeMicroschema(MicroschemaContainer container);

	MicroschemaContainer create(String name, User user);

}
