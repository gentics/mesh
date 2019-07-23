package com.gentics.mesh.test.context;

import static com.gentics.mesh.test.ClientHelper.expectException;
import static org.junit.Assert.fail;

import java.util.Map;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.error.GenericRestException;

import io.netty.handler.codec.http.HttpResponseStatus;

public interface PluginHelper {

	MeshTestContext getTestContext();

	default void deployPlugin(Class<?> clazz, String id) {
		Mesh.mesh().deployPlugin(clazz, id).blockingGet();
	}

	default void deployPlugin(Class<?> clazz, String id, HttpResponseStatus status, String i18nKey, String... i18nProps) {
		try {
			Mesh.mesh().deployPlugin(clazz, id).blockingGet();
			fail("Deployment of plugin {" + clazz.getSimpleName() + "/" + id + "} should have failed.");
		} catch (GenericRestException e) {
			expectException(e, status, i18nKey, i18nProps);
		}
	}

	default String pluginDir() {
		return getTestContext().getOptions().getPluginDirectory();
	}

	default Map<String, String> plugins() {
		return Mesh.mesh().pluginUuids();
	}

	/**
	 * Return the count of deployed plugins.
	 * 
	 * @return
	 */
	default long pluginCount() {
		return plugins().size();
	}
}
