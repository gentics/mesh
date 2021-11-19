package com.gentics.mesh.test.context;

import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.util.SearchWaitUtil;

public class MeshTestContextOverride extends MeshTestContext {

	private SearchWaitUtil waitUtil;

	@Override
	public MeshComponent createMeshComponent(MeshOptions options, MeshTestSetting settings) {
		return getMeshDaggerBuilder()
			.configuration(options)
			.searchProviderType(settings.elasticsearch().toSearchProviderType())
			.searchWaitUtilSupplier(this::getWaitUtil)
			.build();
	}

	@Override
	public MeshOptions init(MeshTestSetting settings) throws Exception {
		MeshOptions options = super.init(settings);
		options.getSearchOptions().setWaitForIdle(false);

		return options;
	}

	public SearchWaitUtil getWaitUtil() {
		return waitUtil;
	}

	public MeshTestContextOverride setWaitUtil(SearchWaitUtil waitUtil) {
		this.waitUtil = waitUtil;
		return this;
	}
}
