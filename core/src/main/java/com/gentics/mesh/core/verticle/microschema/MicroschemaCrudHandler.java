package com.gentics.mesh.core.verticle.microschema;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.schema.MicroschemaResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;

@Component
public class MicroschemaCrudHandler extends AbstractCrudHandler<MicroschemaContainer, MicroschemaResponse> {

	@Override
	public RootVertex<MicroschemaContainer> getRootVertex(InternalActionContext ac) {
		return boot.microschemaContainerRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		deleteElement(ac, () -> getRootVertex(ac), "uuid", "microschema_deleted");
	}

}
