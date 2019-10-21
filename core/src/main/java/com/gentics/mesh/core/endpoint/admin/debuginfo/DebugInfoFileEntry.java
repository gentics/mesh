package com.gentics.mesh.core.endpoint.admin.debuginfo;

import java.util.zip.ZipEntry;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.reactivex.core.file.AsyncFile;
import io.vertx.reactivex.core.file.FileSystem;

public class DebugInfoFileEntry implements DebugInfoEntry {
	private final FileSystem fs;
	private final String source;
	private final String target;
	private final boolean deleteFileAfterRead;

	public DebugInfoFileEntry(FileSystem fs, String source, String target, boolean deleteFileAfterRead) {
		this.source = source;
		this.target = target;
		this.fs = fs;
		this.deleteFileAfterRead = deleteFileAfterRead;
	}

	public static DebugInfoEntry fromFile(FileSystem vertx, String source, String target) {
		return fromFile(vertx, source, target, false);
	}

	public static DebugInfoEntry fromFile(FileSystem vertx, String source, String target, boolean deleteFileAfterRead) {
		return new DebugInfoFileEntry(vertx, source, target, deleteFileAfterRead);
	}

	@Override
	public ZipEntry createZipEntry() {
		return new ZipEntry(target);
	}

	@Override
	public String getFileName() {
		return target;
	}

	@Override
	public Flowable<Buffer> getData() {
		return fs.rxOpen(source, new OpenOptions().setRead(true).setCreateNew(false).setDeleteOnClose(deleteFileAfterRead))
			.flatMapPublisher(AsyncFile::toFlowable)
			.map(io.vertx.reactivex.core.buffer.Buffer::getDelegate);
	}
}
