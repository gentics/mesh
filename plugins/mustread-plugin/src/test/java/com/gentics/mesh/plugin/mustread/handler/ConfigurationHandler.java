package com.gentics.mesh.plugin.mustread.handler;

import com.gentics.mesh.plugin.mustread.MustReadPlugin;
import com.gentics.mesh.plugin.mustread.config.MustReadConfig;
import com.gentics.mesh.plugin.mustread.helper.LogHelper;
import io.vertx.ext.web.RoutingContext;

public class ConfigurationHandler extends BaseHandler {

    public ConfigurationHandler(MustReadPlugin plugin, MustReadConfig config, LogHelper logHelper) {
        super(plugin, config, logHelper);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        initialize(routingContext);

        doJsonResponse(getConfig());
    }
}
