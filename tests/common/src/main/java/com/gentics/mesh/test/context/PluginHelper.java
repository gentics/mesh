package com.gentics.mesh.test.context;

import static com.gentics.mesh.test.ClientHelper.expectException;
import static org.junit.Assert.fail;

import java.util.Set;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.error.GenericRestException;

import io.netty.handler.codec.http.HttpResponseStatus;

public interface PluginHelper {

	MeshTestContext getTestContext();

	default void deployPlugin(Class<?> clazz, String id) {
		meshApi2().deployPlugin(clazz, id).blockingAwait();
	}

	default void deployPlugin(Class<?> clazz, String id, HttpResponseStatus status, String i18nKey, String... i18nProps) {
		try {
			meshApi2().deployPlugin(clazz, id).blockingAwait();
			fail("Deployment of plugin {" + clazz.getSimpleName() + "/" + id + "} should have failed.");
		} catch (GenericRestException e) {
			expectException(e, status, i18nKey, i18nProps);
		}
	}

	default String pluginDir() {
		return getTestContext().getOptions().getPluginDirectory();
	}

	default Set<String> plugins() {
		return meshApi2().pluginIds();
	}

	/**
	 * Return the count of deployed plugins.
	 * 
	 * @return
	 */
	default long pluginCount() {
		return plugins().size();
	}

	default Mesh meshApi2() {
		return getTestContext().getMesh();
	}
}
