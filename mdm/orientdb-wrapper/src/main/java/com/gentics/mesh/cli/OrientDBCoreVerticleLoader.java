package com.gentics.mesh.cli;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.gentics.mesh.dbadmin.DatabaseAdminServerVerticle;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.vertx.core.DeploymentOptions;
import io.vertx.reactivex.core.Vertx;

@Singleton
public class OrientDBCoreVerticleLoader extends CoreVerticleLoader {

	@Inject
	OrientDBMeshOptions meshOptions;

	@Inject
	public Provider<DatabaseAdminServerVerticle> dbAdminServerVerticle;

	@Inject
	public OrientDBCoreVerticleLoader(Vertx rxVertx) {
		super(rxVertx);
	}

	@Override
	protected Completable deployAll() {
		return Completable.mergeArray(
				deployRestVerticle(),
				deployMonitoringVerticle(),
				deployJobWorkerVerticle(),
				deploySearchVerticle(),
				deployDatabaseAdminVerticle());
	}

	protected CompletableSource deployDatabaseAdminVerticle() {
		if (meshOptions.getStorageOptions().getAdministrationOptions() != null && meshOptions.getStorageOptions().getAdministrationOptions().isEnabled()) {
			return rxVertx.rxDeployVerticle(dbAdminServerVerticle::get, new DeploymentOptions()
				.setInstances(1))
				.ignoreElement();
		} else {
			return Completable.complete();
		}
	}
}
