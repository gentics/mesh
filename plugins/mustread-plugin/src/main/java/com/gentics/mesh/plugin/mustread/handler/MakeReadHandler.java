package com.gentics.mesh.plugin.mustread.handler;

import com.gentics.mesh.plugin.mustread.MustReadPlugin;
import com.gentics.mesh.plugin.mustread.config.MustReadConfig;
import com.gentics.mesh.plugin.mustread.controller.NodeController;
import com.gentics.mesh.plugin.mustread.exception.PluginPathNotFoundException;
import com.gentics.mesh.plugin.mustread.helper.LogHelper;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.vertx.core.json.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.List;

public class MakeReadHandler extends BaseHandler {

    private static final Logger log = LoggerFactory.getLogger(MakeReadHandler.class);

    private NodeController nodeController;

    public MakeReadHandler(MustReadPlugin plugin, MustReadConfig config, LogHelper logHelper, NodeController nodeController) {
        super(plugin, config, logHelper);

        this.nodeController = nodeController;
    }

    @Override
    public void handle(RoutingContext routinContext) {
        List<Completable> completables = new ArrayList<>();

        initialize(routinContext);

        if (!checkProjectConfiguration()) {
            doHttpCode(HttpResponseStatus.NOT_IMPLEMENTED);
            return;
        }

        JsonArray providedData = getBodyAsJson().getJsonArray(getConfig().getNodeListParameterName());

        if (providedData == null || providedData.size() < 1) {
            doJsonResponse("Noting updated, because no uuids provided", HttpResponseStatus.BAD_REQUEST);
            return;
        }

        providedData.forEach((item -> {
            String uuid = (String) item;

            completables.add(getNodeController().createReadNodeForProjectAndUser(getProjectName(), getUser(), uuid));
        }));

        // First we create the user folder then we do the creation of the read nodes
        getNodeController().createUserFolderForProject(getProjectName(), getUser())
                .andThen(Completable.merge(completables))
                .subscribe(() -> {
                    doJsonResponse("successful");
                }, error -> {
                    if (error instanceof PluginPathNotFoundException) {
                        doHttpCode(HttpResponseStatus.NOT_FOUND);
                    } else {
                        doHttpCode(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                    }
                });
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    private NodeController getNodeController() {
        return nodeController;
    }
}
