package com.gentics.mesh.plugin.mustread.consumer;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.plugin.mustread.MustReadPlugin;
import com.gentics.mesh.plugin.mustread.config.MustReadConfig;
import com.gentics.mesh.plugin.mustread.helper.LogHelper;
import com.gentics.mesh.rest.client.MeshRestClient;
import io.vertx.core.json.JsonObject;

abstract class BaseConsumer {

    private MustReadPlugin plugin;

    private MeshRestClient client;

    private LogHelper logHelper;

    private MustReadConfig configuration;

    BaseConsumer(MustReadPlugin plugin, MeshRestClient client, LogHelper logHelper, MustReadConfig configuration) {
        this.plugin = plugin;
        this.client = client;
        this.logHelper = logHelper;
        this.configuration = configuration;
    }

    MustReadPlugin getPlugin() {
        return plugin;
    }

    MeshRestClient getClient() {
        return client;
    }

    LogHelper getLogHelper() {
        return logHelper;
    }

    public MustReadConfig getConfiguration() {
        return configuration;
    }

    abstract void consume(JsonObject object);

    void register(MeshEvent Event) {
        getPlugin().getRxVertx().eventBus().<JsonObject>consumer(Event.address, message -> {
            consume(message.body());
        });
    }

}
