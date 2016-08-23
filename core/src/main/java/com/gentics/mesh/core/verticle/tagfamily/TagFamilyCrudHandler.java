package com.gentics.mesh.core.verticle.tagfamily;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;

public class TagFamilyCrudHandler extends AbstractCrudHandler<TagFamily, TagFamilyResponse> {

	@Override
	public RootVertex<TagFamily> getRootVertex(InternalActionContext ac) {
		return ac.getProject().getTagFamilyRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac, String uuid) {
		HandlerUtilities.deleteElement(ac, () -> getRootVertex(ac), uuid);
	}

}
