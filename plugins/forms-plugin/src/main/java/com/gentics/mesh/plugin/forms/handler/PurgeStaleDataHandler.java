package com.gentics.mesh.plugin.forms.handler;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.service.PurgeService;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.http.HttpHeaders;

/**
 * Handler for purging stale data. This is currently only used for tests, not in the productive code.
 */
@Singleton
public class PurgeStaleDataHandler extends AbstractFormsHandler {
	private final PurgeService purgeService;

	@Inject
	public PurgeStaleDataHandler(PurgeService purgeService) {
		this.purgeService = purgeService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		Single<Long> secondsSingle = ParameterUtil.getLongFromPath(context, "seconds");
		int perPage = Integer.valueOf(context.request().getParam("perPage", "100"));

		return secondsSingle.map(seconds -> ZonedDateTime.now(ZoneOffset.UTC).minus(Duration.ofSeconds(seconds)))
				.flatMapCompletable(
						purgeThreshold -> purgeService.purgeAllStaleData(projectName, purgeThreshold, perPage))
				.doOnComplete(() -> {
					context.response().putHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.APPLICATION_JSON_UTF8)
							.end("{}");
				});
	}
}
