package com.gentics.mesh.demo;

import java.io.IOException;
import java.net.ServerSocket;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.demo.verticle.DemoVerticle;
import com.gentics.mesh.etc.OptionsLoader;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.util.DeploymentUtil;

import io.vertx.core.json.JsonObject;

/**
 * Simple dump generator which will start mesh and terminate it once the test data has been generated.
 */
public class DumpGenerator {

	public static void main(String[] args) throws Exception {
		MeshOptions options = OptionsLoader.createOrloadOptions();
		options.getHttpServerOptions().setEnableCors(true);
		options.getHttpServerOptions().setCorsAllowedOriginPattern("*");
		int port = getRandomPort();
		options.getHttpServerOptions().setPort(port);
		options.getStorageOptions().setDirectory("target/dbdump");
		final Mesh mesh = Mesh.mesh(options);
		mesh.setCustomLoader((vertx) -> {
			JsonObject config = new JsonObject();
			config.put("port", port);
			DeploymentUtil.deployAndWait(vertx, config, DemoVerticle.class, false);
		});

		mesh.getVertx().eventBus().consumer("mesh-startup-complete", rh -> {
			try {
				mesh.shutdown();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		mesh.run();
	}

	/**
	 * Return a free port random port by opening an socket and check whether it is currently used. Not the most elegant or efficient solution, but works.
	 * 
	 * @param port
	 * @return
	 */
	public static int getRandomPort() {
		ServerSocket socket = null;

		try {
			socket = new ServerSocket(0);
			return socket.getLocalPort();
		} catch (IOException ioe) {
			return -1;
		} finally {
			// if we did open it cause it's available, close it
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					// ignore
					e.printStackTrace();
				}
			}
		}
	}

}
