package com.gentics.mesh.core.endpoint.admin.debuginfo;

import java.util.zip.ZipEntry;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.reactivex.core.file.AsyncFile;
import io.vertx.reactivex.core.file.FileSystem;

/**
 * @see DebugInfoEntry
 */
public class DebugInfoFileEntry implements DebugInfoEntry {

	private final FileSystem fs;
	private final String source;
	private final String target;
	private final boolean deleteFileAfterRead;

	/**
	 * Create a new entry
	 * 
	 * @param fs
	 *            Async FS provider
	 * @param source
	 *            Source path from the filesystem
	 * @param target
	 *            Filename within the entry (e.g. zipfile itself)
	 * @param deleteFileAfterRead
	 *            Whether to the delete the source after reading it
	 */
	public DebugInfoFileEntry(FileSystem fs, String source, String target, boolean deleteFileAfterRead) {
		this.source = source;
		this.target = target;
		this.fs = fs;
		this.deleteFileAfterRead = deleteFileAfterRead;
	}

	/**
	 * Create a new entry.
	 * 
	 * @param fs
	 * @param source
	 * @param target
	 * @return
	 */
	public static DebugInfoEntry fromFile(FileSystem fs, String source, String target) {
		return fromFile(fs, source, target, false);
	}

	/**
	 * Create a new entry.
	 * 
	 * @param fs
	 * @param source
	 * @param target
	 * @param deleteFileAfterRead
	 * @return
	 */
	public static DebugInfoEntry fromFile(FileSystem fs, String source, String target, boolean deleteFileAfterRead) {
		return new DebugInfoFileEntry(fs, source, target, deleteFileAfterRead);
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
