package com.gentics.mesh.plugin.search.dagger;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.search.config.SearchConfiguration;
import com.gentics.mesh.plugin.search.handler.AutocompleteHandler;
import com.gentics.mesh.plugin.search.extension.CustomHandler;
import com.gentics.mesh.plugin.search.handler.SearchHandler;
import com.gentics.mesh.plugin.search.service.BranchService;
import com.gentics.mesh.plugin.search.service.QueryService;
import com.gentics.mesh.rest.client.MeshRestClient;
import dagger.BindsInstance;
import dagger.Component;
import org.pf4j.PluginManager;

import javax.annotation.Nullable;
import javax.inject.Singleton;

@Singleton
@Component(modules = SearchPluginModule.class)
public interface SearchPluginComponent {

	SearchHandler searchHandler();
	AutocompleteHandler autocompleteHandler();

	@Component.Builder
	interface Builder {
		@BindsInstance
		Builder adminClient(MeshRestClient adminClient);

		@BindsInstance
		Builder configuration(SearchConfiguration config);

		@BindsInstance
		Builder pluginContext(PluginContext context);

		@BindsInstance
		Builder pluginManager(PluginManager pluginManager);

		@BindsInstance
		Builder queryService(QueryService queryService);

		@BindsInstance
		Builder branchService(BranchService branchService);

		@BindsInstance
		Builder customHandler(@Nullable CustomHandler customHandler);

		SearchPluginComponent build();
	}
}
