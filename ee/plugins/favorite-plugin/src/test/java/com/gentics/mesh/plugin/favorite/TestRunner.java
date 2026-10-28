package com.gentics.mesh.plugin.favorite;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Completable;
/**
 * Starts a MESH Server with the port 8092 with the plugin deployed. It creates a user admin and a project test
 */
public class TestRunner {
	public static void main(String[] args) throws Throwable {
		final int httpPort = 8092;

		HibernateMeshOptions options = OptionsLoader.createOrloadOptions(HibernateMeshOptions.class, args);

		options.getSearchOptions().setUrl(null);
		options.getHttpServerOptions().setPort(httpPort);

		Mesh mesh = Mesh.create(options);
		new Thread(() -> {
			try {
				mesh.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();

		awaitStartup(mesh).blockingAwait();

		MeshRestClient client = MeshRestClient.create("localhost", httpPort, false);
		client.setLogin("admin", "admin");
		client.login().blockingGet();

		ProjectResponse project = createProject(client);
		createFolder(client, project, "favorite");
		createFolder(client, project, "someNode");

		mesh.deployPlugin(FavoriteTestPlugin.class, "favorite").blockingAwait();

		mesh.dontExit();
	}

	private static ProjectResponse createProject(MeshRestClient client) {
		return client.createProject(new ProjectCreateRequest()
			.setName("test")
			.setSchemaRef("folder")).toSingle().blockingGet();
	}

	private static NodeResponse createFolder(MeshRestClient client, ProjectResponse project, String name) {
		NodeCreateRequest request = new NodeCreateRequest()
			.setSchemaName("folder")
			.setParentNodeUuid(project.getRootNode().getUuid())
			.setLanguage("en");
		request.getFields().put("name", new StringFieldImpl().setString(name));
		request.getFields().put("slug", new StringFieldImpl().setString(name));
		return client.createNode(project.getName(), request).toSingle().blockingGet();
	}

	private static Completable awaitStartup(Mesh mesh) {
		return Completable.create(sub -> {
			while (true) {
				try {
					if(mesh.getVertx() == null){
						Thread.sleep(1000);
					}else {
						break;
					}

				} catch (Throwable e) {

					Thread.sleep(1000);
				}
			}
			mesh.getVertx().eventBus().consumer(MeshEvent.STARTUP.address, mh -> {
				sub.onComplete();
			});
		});
	}
}
