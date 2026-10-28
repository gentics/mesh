package com.gentics.mesh.plugin.comment;

import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.etc.config.HibernateMeshOptions;

public class DeployCommentPluginTest {


	@Test
	@Ignore("Intended for manual testing")
	public void testCommentDeployment() {
		final int httpPort = 8092;
		HibernateMeshOptions options = OptionsLoader.createOrloadOptions(HibernateMeshOptions.class,
				null);
		options.getHttpServerOptions().setPort(httpPort);
		options.setAdminPassword("admin")
				.setForceInitialAdminPasswordReset(false);

		Mesh mesh = Mesh.create(options);
		mesh.rxRun().blockingAwait(200, TimeUnit.SECONDS);
		mesh.deployPlugin(CommentPlugin.class, "comment").blockingAwait(60, TimeUnit.SECONDS);
	}


}
