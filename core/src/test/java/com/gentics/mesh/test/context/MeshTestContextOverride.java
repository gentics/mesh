package com.gentics.mesh.test.context;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.dagger.DaggerMeshComponent;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.test.DaggerMeshTestComponent;
import com.gentics.mesh.util.SearchWaitUtil;

public class MeshTestContextOverride extends MeshTestContext {

	private SearchWaitUtil waitUtil;

	public void initDagger(MeshOptions options, MeshTestSetting settings) throws Exception {
		options.getSearchOptions().setBulkLimit(1);
		super.initDagger(options, settings);
	}

	@Override
	public MeshComponent createMeshComponent(Mesh mesh, MeshOptions options, MeshTestSetting settings) {
		return DaggerMeshTestComponent.builder()
			.configuration(options)
			.searchProviderType(settings.elasticsearch().toSearchProviderType())
			.mesh(mesh)
			.waitUtil(waitUtil)
			.build();
	}

	public SearchWaitUtil getWaitUtil() {
		return waitUtil;
	}

	public MeshTestContextOverride setWaitUtil(SearchWaitUtil waitUtil) {
		this.waitUtil = waitUtil;
		return this;
	}
}
