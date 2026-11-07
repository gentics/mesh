package com.gentics.mesh.plugin.mustread.initializer;

import com.gentics.mesh.plugin.mustread.config.MustReadConfig;
import com.gentics.mesh.plugin.mustread.helper.LogHelper;
import com.gentics.mesh.rest.client.MeshRestClient;
import io.reactivex.Completable;

abstract class BaseInitializer {
    private MeshRestClient client;
    private MustReadConfig config;
    private LogHelper logHelper;

    BaseInitializer(MeshRestClient client, MustReadConfig config, LogHelper logHelper) {
        this.client = client;
        this.config = config;
        this.logHelper = logHelper;

    }

    abstract Completable initialize();

    MeshRestClient getClient() {
        return client;
    }

    MustReadConfig getConfig() {
        return config;
    }

    LogHelper getLogHelper() {
        return logHelper;
    }
}
