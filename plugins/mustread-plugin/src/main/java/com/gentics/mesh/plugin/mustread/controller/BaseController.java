package com.gentics.mesh.plugin.mustread.controller;

import com.gentics.mesh.plugin.mustread.helper.LogHelper;
import com.gentics.mesh.rest.client.MeshRestClient;

abstract class BaseController {
    private MeshRestClient client;
    private LogHelper logHelper;

    BaseController(MeshRestClient client, LogHelper logHelper) {
        this.client = client;
        this.logHelper = logHelper;

    }

    MeshRestClient getClient() {
        return client;
    }

    LogHelper getLogHelper() {
        return logHelper;
    }
}
