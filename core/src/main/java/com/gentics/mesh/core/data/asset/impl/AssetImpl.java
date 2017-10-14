package com.gentics.mesh.core.data.asset.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BINARY;
import static com.gentics.mesh.util.URIUtils.encodeFragment;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.asset.Asset;
import com.gentics.mesh.core.data.asset.AssetBinary;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.asset.AssetResponse;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * @see Asset
 */
public class AssetImpl extends AbstractMeshCoreVertex<AssetResponse, Asset> implements Asset {

	public static void init(Database database) {
		database.addVertexType(AssetImpl.class, MeshVertexImpl.class);
	}

	public Iterable<? extends Asset> findAssets() {
		return in(HAS_BINARY).frameExplicit(AssetImpl.class);
	}

	@Override
	public void setAssetBinary(AssetBinary assetBinary) {
		setUniqueLinkOutTo(assetBinary, HAS_BINARY);
	}

	@Override
	public AssetBinary getAssetBinary() {
		return out(HAS_BINARY).nextOrDefaultExplicit(AssetBinaryImpl.class, null);
	}

	@Override
	public Asset update(InternalActionContext ac, SearchQueueBatch batch) {
		// TODO Auto-generated method stub
		return this;
	}


	@Override
	public String getAPIPath(InternalActionContext ac) {
		return "/api/v1/" + encodeFragment(ac.getProject().getName()) + "/assets/" + getUuid();
	}

	@Override
	public AssetResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		AssetResponse model = new AssetResponse();

		model.setFilename(getFilename());
		model.setSha512sum(getAssetBinary().getSHA512Sum());

		fillCommonRestFields(ac, model);
		setRolePermissions(ac, model);

		return model;

	}

}
