package com.gentics.mesh.core.endpoint.tagfamily;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.GlobalLock;
import com.gentics.mesh.graphdb.spi.Database;

@Singleton
public class TagFamilyCrudHandler extends AbstractCrudHandler<TagFamily, TagFamilyResponse> {

	@Inject
	public TagFamilyCrudHandler(Database db, HandlerUtilities utils, GlobalLock writeLock) {
		super(db, utils, writeLock);
	}

	@Override
	public RootVertex<TagFamily> getRootVertex(InternalActionContext ac) {
		return ac.getProject().getTagFamilyRoot();
	}

}
