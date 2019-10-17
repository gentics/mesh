package com.gentics.mesh.core.endpoint.admin.debuginfo.providers;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoFileEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.LoadLevel;
import com.gentics.mesh.etc.config.MeshOptions;

import io.reactivex.Flowable;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;

@Singleton
public class DatabaseDumpProvider implements DebugInfoProvider {
	private final MeshOptions options;
	private final FileSystem fs;
	private final String baseDir;
	private final String TARGET_DIR = "graphdb";

	@Inject
	public DatabaseDumpProvider(MeshOptions options, Vertx vertx) {
		this.options = options;
		this.fs = vertx.fileSystem();
		this.baseDir = options.getStorageOptions().getDirectory();
	}

	@Override
	public String name() {
		return "databaseDump";
	}

	@Override
	public LoadLevel loadLevel() {
		return LoadLevel.HEAVY;
	}

	@Override
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		if (baseDir == null) {
			return Flowable.empty();
		}
		return sendFiles(baseDir);
	}

	private Flowable<DebugInfoEntry> sendFiles(String path) {
		return fs.rxProps(path).flatMapPublisher(props -> {
			if (props.isRegularFile()) {
				return Flowable.just(DebugInfoFileEntry.fromFile(fs, baseDir, path, TARGET_DIR));
			} else if (props.isDirectory()) {
				return fs.rxReadDir(path)
					.flatMapPublisher(Flowable::fromIterable)
					.flatMap(this::sendFiles);
			} else {
				return Flowable.empty();
			}
		});
	}
}
