package com.gentics.mesh.plugin.comment;

import static com.gentics.mesh.core.rest.MeshEvent.STARTUP;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import io.reactivex.Completable;
import org.junit.Test;

public class DeployCommentPluginTest {


	@Test
	public void givenCommentPluginWithoutValidLicense_shouldThrowException() {
		final int httpPort = 8092;
		OrientDBMeshOptions options = OptionsLoader.createOrloadOptions(OrientDBMeshOptions.class,
				null);
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

		mesh.deployPlugin(CommentPlugin.class, "comment").test().assertComplete();
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
