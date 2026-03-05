package com.gentics.mesh.plugin.forms.handler.downloads;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.apache.commons.lang3.Strings;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.dagger.UserClient;
import com.gentics.mesh.plugin.common.util.ContentDispositionMode;
import com.gentics.mesh.plugin.common.util.HandlerUtil;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.FormsError;
import com.gentics.mesh.plugin.forms.SchemaInitializer;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Completable;
import io.reactivex.Single;

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

		Single<String> downloadUuidSingle = ParameterUtil.getUuidFromPath(context, "downloadUuid");

		return downloadUuidSingle
			.flatMap(downloadUuid -> userClient.findNodeByUuid(projectName, downloadUuid).toSingle())
			.flatMap(downloadNode -> {
				if (Strings.CI.equals(downloadNode.getSchema().getName(), SchemaInitializer.DOWNLOAD_SCHEMA_NAME)) {
					return Single.just(downloadNode);
				} else {
					return Single.error(new FormsError("Could not find download node with uuid %s".formatted(downloadNode.getUuid()), 404));
				}
			})
			.flatMap(downloadNode -> userClient.downloadBinaryField(projectName, downloadNode.getUuid(), "en", "download").toSingle())
			.doOnEvent(HandlerUtil.meshBinaryHandler(context, ContentDispositionMode.ATTACHMENT, false))
			.ignoreElement();
	}
}
