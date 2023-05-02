package com.gentics.mesh.plugin.forms.handler.downloads;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.dagger.UserClient;
import com.gentics.mesh.plugin.common.util.ContentDispositionMode;
import com.gentics.mesh.plugin.common.util.HandlerUtil;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.plugin.forms.util.ParameterUtil;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshRestClient;
import io.reactivex.Completable;
import io.vertx.core.http.HttpHeaders;

import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Download the binary field of the specified download node.
 */
public class DownloadHandler extends AbstractFormsHandler {
	private final MeshRestClient userClient;

	@Inject
	public DownloadHandler(@UserClient @Nullable MeshRestClient userClient) {
		this.userClient = userClient;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String downloadUuid = context.pathParam("downloadUuid");

		return userClient.downloadBinaryField(projectName, downloadUuid, "en", "download").toSingle()
			.doOnEvent(HandlerUtil.meshBinaryHandler(context, ContentDispositionMode.ATTACHMENT, false))
			.ignoreElement();
	}
}
