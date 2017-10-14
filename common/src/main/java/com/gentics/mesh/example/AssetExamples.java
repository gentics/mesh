package com.gentics.mesh.example;

import com.gentics.mesh.core.rest.asset.AssetListResponse;
import com.gentics.mesh.core.rest.asset.AssetResponse;

public class AssetExamples extends AbstractExamples {

	public AssetResponse createAssetResponse(String filename) {
		AssetResponse response = new AssetResponse();
		response.setFilename(filename);
		return response;
	}

	public AssetListResponse createAssetListResponse() {
		AssetListResponse response = new AssetListResponse();
		response.getData().add(createAssetResponse("flower.jpg"));
		return response;
	}

}
