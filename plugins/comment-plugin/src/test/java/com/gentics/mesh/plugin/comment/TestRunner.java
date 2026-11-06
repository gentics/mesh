package com.gentics.mesh.plugin.comment;

import static com.gentics.mesh.core.rest.MeshEvent.STARTUP;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Completable;

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
		createFolder(client, project, "comments");
		createFolder(client, project, "someNode");

		TestCommentPlugin.config = "anonymousNoApproval";
		mesh.deployPlugin(TestCommentClient.class, "comment").blockingAwait();

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
			while (mesh.getVertx() == null) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			mesh.getVertx().eventBus().consumer(STARTUP.getAddress(), mh -> sub.onComplete());
		});
	}
}
