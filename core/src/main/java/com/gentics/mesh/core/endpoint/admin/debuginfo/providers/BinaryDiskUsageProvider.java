package com.gentics.mesh.core.endpoint.admin.debuginfo.providers;

import static com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoUtil.humanReadableByteCount;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoBufferEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.LoadLevel;
import com.gentics.mesh.etc.config.MeshOptions;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;

@Singleton
public class BinaryDiskUsageProvider implements DebugInfoProvider {
	private static final Logger log = LoggerFactory.getLogger(BinaryDiskUsageProvider.class);

	private final MeshOptions options;
	private final FileSystem fs;

	@Inject
	public BinaryDiskUsageProvider(MeshOptions options, Vertx vertx) {
		this.options = options;
		this.fs = vertx.fileSystem();
	}

	@Override
	public String name() {
		return "binaryDiskUsage";
	}

	@Override
	public LoadLevel loadLevel() {
		return LoadLevel.MEDIUM;
	}

	@Override
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		return Single.zip(
			getTotalDiskUsage(options.getUploadOptions().getDirectory()),
			getTotalDiskUsage(options.getImageOptions().getImageCacheDirectory()),
			BinaryDiskUsage::new
		).map(usage -> DebugInfoBufferEntry.asJson("binaryDiskUsage.json", usage))
		.toFlowable();
	}

	private Single<Long> getTotalDiskUsage(String path) {
		return fs.rxProps(path)
			.flatMap(props -> {
				if (props.isRegularFile()) {
					return Single.just(props.size());
				} else if (props.isDirectory()) {
					return fs.rxReadDir(path)
						.flatMapPublisher(Flowable::fromIterable)
						.flatMapSingle(this::getTotalDiskUsage)
						.reduce(Long::sum)
						.toSingle(0L);
				} else {
					return Single.just(0L);
				}
			})
			.doOnError(err -> log.warn(String.format("Could not get filesize of file {%s}", path), err))
			.onErrorReturnItem(0L);
	}

	public static class BinaryDiskUsage {
		public final String binaries;
		public final String imageCache;

		public BinaryDiskUsage(String binaries, String imageCache) {
			this.binaries = binaries;
			this.imageCache = imageCache;
		}

		public BinaryDiskUsage(long binaries, long imageCache) {
			this.binaries = humanReadableByteCount(binaries);
			this.imageCache = humanReadableByteCount(imageCache);
		}
	}
}
