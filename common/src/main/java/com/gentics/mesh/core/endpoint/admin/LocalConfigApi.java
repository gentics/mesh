package com.gentics.mesh.core.endpoint.admin;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.rest.admin.runtimeconfig.LocalConfigModel;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.shareddata.AsyncMap;

@Singleton
public class LocalConfigApi {

	private final Lazy<Vertx> vertx;
	private final String LOCAL_CONFIG_KEY = "localConfig";

	@Inject
	public LocalConfigApi(Lazy<Vertx> vertx) {
		this.vertx = vertx;
	}

	/**
	 * Initializes the local config.
	 * @return
	 */
	public Completable init() {
		LocalConfigModel localConfigModel = new LocalConfigModel();
		return setActiveConfig(localConfigModel).ignoreElement();
	}

	public Single<LocalConfigModel> getActiveConfig() {
		return getMap().flatMap(map -> map.rxGet(LOCAL_CONFIG_KEY).toSingle());
	}

	public Single<LocalConfigModel> setActiveConfig(LocalConfigModel runtimeConfig) {
		return getMap().flatMap(map -> map.rxPut(LOCAL_CONFIG_KEY, runtimeConfig)
			.andThen(Single.just(runtimeConfig)));
	}

	private Single<AsyncMap<String, LocalConfigModel>> getMap() {
		return vertx.get().sharedData().rxGetLocalAsyncMap("mesh");
	}
}
