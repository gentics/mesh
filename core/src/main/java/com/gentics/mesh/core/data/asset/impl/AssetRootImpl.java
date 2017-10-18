package com.gentics.mesh.core.data.asset.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ASSET;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.asset.Asset;
import com.gentics.mesh.core.data.asset.AssetRoot;
import com.gentics.mesh.core.data.root.impl.AbstractRootVertex;
import com.gentics.mesh.core.data.search.SearchQueueBatch;

public class AssetRootImpl extends AbstractRootVertex<Asset> implements AssetRoot {

	@Override
	public Asset create(InternalActionContext ac, SearchQueueBatch batch, String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<? extends Asset> getPersistanceClass() {
		return Asset.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_ASSET;
	}

}
