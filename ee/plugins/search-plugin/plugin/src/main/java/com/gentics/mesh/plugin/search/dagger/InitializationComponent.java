package com.gentics.mesh.plugin.search.dagger;

import com.gentics.mesh.plugin.search.config.SearchConfiguration;
import com.gentics.mesh.plugin.search.service.BranchService;
import com.gentics.mesh.plugin.search.service.QueryService;
import com.gentics.mesh.rest.client.MeshRestClient;
import dagger.BindsInstance;
import dagger.Component;
import org.pf4j.PluginManager;

import javax.inject.Singleton;
import java.io.File;

@Singleton
@Component(modules = SearchPluginModule.class)
public interface InitializationComponent {

	QueryService queryService();

	BranchService branchService();

	@Component.Builder
	interface Builder {
		@BindsInstance
		Builder configuration(SearchConfiguration configuration);

		@BindsInstance
		Builder adminClient(MeshRestClient adminClient);

		@BindsInstance
		Builder storageDir(File storageDir);

		@BindsInstance
		Builder pluginManager(PluginManager pluginManager);

		InitializationComponent build();
	}
}
