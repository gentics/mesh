package com.gentics.mesh.plugin.export.generator;

import static org.mockito.Mockito.mock;

import org.pf4j.PluginManager;

import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.plugin.common.generator.PluginOpenApiPreparer;
import com.gentics.mesh.plugin.common.generator.PluginOpenApiSpecGenerator;
import com.gentics.mesh.plugin.export.ExportPlugin;
import com.gentics.mesh.plugin.export.dagger.DaggerExportPluginComponent;
import com.gentics.mesh.plugin.export.dagger.ExportPluginComponent;
import com.gentics.mesh.plugin.export.model.PluginConfiguration;
import com.gentics.mesh.rest.client.MeshRestClient;

/**
 * ExportPlugin#createProjectRouter() eagerly reads its Dagger-built ExportPluginComponent (normally created in
 * initialize(), which also performs role setup calls against a real Mesh server). Build the component directly
 * with mocked collaborators instead of running initialize().
 */
public class ExportOpenApiPreparer implements PluginOpenApiPreparer {

	@Override
	public void prepare(RestPlugin plugin) throws ReflectiveOperationException {
		ExportPluginComponent component = DaggerExportPluginComponent.builder()
			.setMeshClient(mock(MeshRestClient.class))
			.setPluginConfiguration(new PluginConfiguration())
			.setPluginManager(mock(PluginManager.class))
			.build();
		PluginOpenApiSpecGenerator.setField(ExportPlugin.class, plugin, "component", component);
	}
}
