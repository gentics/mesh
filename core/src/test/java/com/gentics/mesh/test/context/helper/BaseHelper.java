package com.gentics.mesh.test.context.helper;

import com.gentics.madl.tx.Tx;
import com.gentics.madl.tx.TxAction;
import com.gentics.madl.tx.TxAction0;
import com.gentics.madl.tx.TxAction1;
import com.gentics.madl.tx.TxAction2;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.manager.MeshPluginManager;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.monitoring.MonitoringRestClient;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.TrackingSearchProvider;
import com.gentics.mesh.search.impl.ElasticSearchProvider;
import com.gentics.mesh.storage.LocalBinaryStorage;
import com.gentics.mesh.test.TestDataProvider;
import com.gentics.mesh.test.context.MeshTestContext;

import io.vertx.core.Vertx;

public interface BaseHelper {

	MeshTestContext getTestContext();

	default MeshComponent mesh() {
		return getTestContext().getMeshComponent();
	}

	default ElasticSearchProvider getProvider() {
		return ((ElasticSearchProvider) searchProvider());
	}

	default TestDataProvider data() {
		return getTestContext().getData();
	}

	default User user() {
		return data().user();
	}

	default void grantAdmin() {
		tx(() -> user().setAdmin(true));
	}

	default void revokeAdmin() {
		tx(() -> user().setAdmin(false));
	}

	/**
	 * Return the test project.
	 * 
	 * @return
	 */
	default Project project() {
		return data().getProject();
	}

	default Database db() {
		return mesh().database();
	}

	default Tx tx() {
		return db().tx();
	}

	default void tx(TxAction0 handler) {
		db().tx(handler);
	}

	default <T> T tx(TxAction1<T> handler) {
		return db().tx(handler);
	}

	default void tx(TxAction2 handler) {
		db().tx(handler);
	}

	default <T> T tx(TxAction<T> handler) {
		return db().tx(handler);
	}

	default Mesh meshApi() {
		return boot().mesh();
	}

	default public Vertx vertx() {
		return getTestContext().getVertx();
	}

	default BootstrapInitializer boot() {
		return mesh().boot();
	}

	default MeshOptions options() {
		return mesh().options();
	}

	default ComplianceMode complianceMode() {
		return options().getSearchOptions().getComplianceMode();
	}

	default MeshPluginManager pluginManager() {
		return mesh().pluginManager();
	}

	default PluginEnvironment pluginEnv() {
		return mesh().pluginEnv();
	}

	default LocalBinaryStorage localBinaryStorage() {
		return mesh().localBinaryStorage();
	}

	default MeshComponent meshDagger() {
		return mesh();
	}

	default SearchProvider searchProvider() {
		return meshDagger().searchProvider();
	}

	default void sleep(long timeMs) {
		try {
			Thread.sleep(timeMs);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	default MonitoringRestClient monClient() {
		return getTestContext().getMonitoringClient();
	}

	default MeshRestClient client() {
		return getTestContext().getHttpClient();
	}

	default MeshRestClient sslClient() {
		return getTestContext().getHttpsClient();
	}

	default MeshRestClient client(String version) {
		return getTestContext().getHttpClient(version);
	}

	default TrackingSearchProvider trackingSearchProvider() {
		return getTestContext().getTrackingSearchProvider();
	}

}
