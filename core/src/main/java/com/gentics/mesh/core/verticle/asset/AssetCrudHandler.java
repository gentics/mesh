package com.gentics.mesh.core.verticle.asset;

import javax.inject.Inject;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.asset.Asset;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.asset.AssetResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.graphdb.spi.Database;

public class AssetCrudHandler extends AbstractCrudHandler<Asset, AssetResponse> {

	@Inject
	public AssetCrudHandler(Database db, HandlerUtilities utils) {
		super(db, utils);
	}

	@Override
	public RootVertex<Asset> getRootVertex(InternalActionContext ac) {
		return ac.getProject().getAssetRoot();
	}

	public void handleDownload(InternalActionContext ac, String uuid) {
		// TODO Auto-generated method stub
		
	}

	public void handleCreate(InternalActionContext ac, String assetUuid) {
		// TODO Auto-generated method stub
	}

}
