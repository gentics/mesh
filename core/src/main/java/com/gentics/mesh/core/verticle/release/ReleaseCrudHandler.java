package com.gentics.mesh.core.verticle.release;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;

/**
 * CRUD Handler for Releases
 */
@Component
public class ReleaseCrudHandler extends AbstractCrudHandler<Release, ReleaseResponse> {

	@Override
	public RootVertex<Release> getRootVertex(InternalActionContext ac) {
		return ac.getProject().getReleaseRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		// TODO Auto-generated method stub
		
	}

}
