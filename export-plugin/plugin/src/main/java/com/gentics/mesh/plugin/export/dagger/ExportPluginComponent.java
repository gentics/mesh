package com.gentics.mesh.plugin.export.dagger;

import com.gentics.mesh.plugin.export.WebrootConfigValueProvider;
import com.gentics.mesh.plugin.export.handler.CsvHandler;
import com.gentics.mesh.plugin.export.model.PluginConfiguration;
import com.gentics.mesh.rest.client.MeshRestClient;
import dagger.BindsInstance;
import dagger.Component;
import org.pf4j.PluginManager;

import javax.inject.Singleton;

@Singleton
@Component(modules = { ExportPluginModule.class })
public interface ExportPluginComponent {

	CsvHandler getCsvHandler();

	WebrootConfigValueProvider getWebrootMappingProvider();

	MeshRestClient getMeshClient();

	PluginConfiguration getConfiguration();

	@Component.Builder
	interface Builder {
		@BindsInstance
		Builder setMeshClient(MeshRestClient client);

		@BindsInstance
		Builder setPluginConfiguration(PluginConfiguration config);

		@BindsInstance
		Builder setPluginManager(PluginManager manager);

		ExportPluginComponent build();
	}
}
