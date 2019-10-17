package com.gentics.mesh.core.endpoint.admin.debuginfo;


import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.time.StopWatch;

import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.graphdb.spi.Database;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class DebugInfoHandler {
	private final Set<DebugInfoProvider> debugInfoProviders;
	private final Database db;
	private static final Logger log = LoggerFactory.getLogger(DebugInfoHandler.class);

	@Inject
	public DebugInfoHandler(Set<DebugInfoProvider> debugInfoProviders, Database db) {
		this.debugInfoProviders = debugInfoProviders;
		this.db = db;
	}

	public void handle(RoutingContext ac) {
		InternalRoutingActionContextImpl iac = new InternalRoutingActionContextImpl(ac);
//		if (db.tx(() -> !iac.getUser().hasAdminRole())) {
//			throw error(FORBIDDEN, "error_admin_permission_required");
//		}
		setHeaders(ac);
		ZipOutputStream zipOutputStream = new ZipOutputStream(WrappedWriteStream.fromWriteStream(ac.response()));
		Flowable.fromIterable(debugInfoProviders)
			.flatMap(provider -> getDebugInfo(iac, provider))
			.flatMapCompletable(entry -> {
				log.debug("Start writing " + entry.getFileName());
				zipOutputStream.putNextEntry(entry.createZipEntry());
				zipOutputStream.write(entry.getData().getBytes());
				log.debug("End writing " + entry.getFileName());
				return Completable.complete();
			})
			.subscribe(() -> {
				zipOutputStream.close();
				ac.response().end();
			}, ac::fail);
	}

	private Flowable<DebugInfoEntry> getDebugInfo(InternalRoutingActionContextImpl iac, DebugInfoProvider provider) {
		if (!log.isDebugEnabled()) {
			return provider.debugInfoEntries(iac);
		}
		StopWatch watch = new StopWatch();
		return provider.debugInfoEntries(iac)
			.doOnSubscribe(ignore -> {
				log.debug("Starting " + provider.name());
				watch.start();
			})
			.doOnComplete(() -> {
				watch.stop();
				log.debug(String.format("%s completed. Took %d ms. ", provider.name(), watch.getTime()));
			});
	}

	private void setHeaders(RoutingContext ac) {
		String filename = "debuginfo--" + Instant.now()
			.atZone(ZoneId.systemDefault())
			.format(DateTimeFormatter.ofPattern("yyyy-MM-dd--HH-mm-ss")) + ".zip";
		ac.response().setChunked(true);
		ac.response().putHeader("Content-Type", "application/zip");
		ac.response().putHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
	}
}
