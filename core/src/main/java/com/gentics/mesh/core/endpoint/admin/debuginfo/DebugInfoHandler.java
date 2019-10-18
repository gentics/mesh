package com.gentics.mesh.core.endpoint.admin.debuginfo;


import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.time.StopWatch;

import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.graphdb.spi.Database;

import io.reactivex.Flowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class DebugInfoHandler {
	private final Set<DebugInfoProvider> debugInfoProviders;
	private final Database db;
	private static final Logger log = LoggerFactory.getLogger(DebugInfoHandler.class);
	private final Set<String> defaultExcluded = Stream.of(
		"consistencyCheck"
	).collect(Collectors.toSet());

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
		Set<String> includedInfos = getIncludedInfos(ac);
		ZipOutputStream zipOutputStream = new ZipOutputStream(WrappedWriteStream.fromWriteStream(ac.response()));
		Flowable.fromIterable(debugInfoProviders)
			.filter(provider -> includedInfos.contains(provider.name()))
			.flatMap(provider -> getDebugInfo(iac, provider))
			.flatMapCompletable(entry -> {
				zipOutputStream.putNextEntry(entry.createZipEntry());
				return entry.getData()
					.doOnNext(buffer -> zipOutputStream.write(buffer.getBytes()))
					.ignoreElements()
					.doOnSubscribe(sub -> log.debug("Start writing " + entry.getFileName()))
					.doOnComplete(() -> log.debug("End writing " + entry.getFileName()));
			}, false, 1)
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

	private Set<String> getIncludedInfos(RoutingContext ac) {
		IncludedInfo includedInfos = new IncludedInfo(ac);
		return Stream.concat(
			defaultInclusions(),
			includedInfos.getIncluded().stream()
		).filter(name -> !includedInfos.getExcluded().contains(name))
		.collect(Collectors.toSet());
	}

	private Stream<String> defaultInclusions() {
		return debugInfoProviders.stream()
			.map(DebugInfoProvider::name)
			.filter(name -> !defaultExcluded.contains(name));
	}
}
