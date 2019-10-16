package com.gentics.mesh.core.endpoint.admin.debuginfo;


import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class DebugInfoHandler {
	private final Set<DebugInfoProvider> debugInfoProviders;

	@Inject
	public DebugInfoHandler(Set<DebugInfoProvider> debugInfoProviders) {
		this.debugInfoProviders = debugInfoProviders;
	}

	public void handle(RoutingContext ac) {
		setHeaders(ac);
		ZipOutputStream zipOutputStream = new ZipOutputStream(WrappedWriteStream.fromWriteStream(ac.response()));
		Flowable.fromIterable(debugInfoProviders)
			.flatMap(DebugInfoProvider::debugInfoEntries)
			.flatMapCompletable(entry -> {
				System.out.println("start " + entry.getFileName());
				zipOutputStream.putNextEntry(entry.createZipEntry());
				zipOutputStream.write(entry.getData().getBytes());
				System.out.println("end " + entry.getFileName());
				return Completable.complete();
			})
			.subscribe(() -> {
				zipOutputStream.close();
				ac.response().end();
			}, ac::fail);
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
