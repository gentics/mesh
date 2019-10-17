package com.gentics.mesh.core.endpoint.admin.debuginfo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;

import io.vertx.core.buffer.Buffer;
import io.vertx.reactivex.core.file.FileSystem;

public class DebugInfoFileEntry implements DebugInfoEntry {
	private final FileSystem fs;
	private final String source;
	private final String target;

	public DebugInfoFileEntry(FileSystem fs, String source, String target) {
		this.source = source;
		this.target = target;
		this.fs = fs;
	}


	public static DebugInfoEntry fromFile(FileSystem vertx, String sourceBaseDir, String sourcePath, String targetBaseDir) {
		Path relativeFile = Paths.get(sourceBaseDir)
			.toAbsolutePath()
			.relativize(Paths.get(sourcePath));

		String target = Paths.get(targetBaseDir)
			.resolve(relativeFile).toString();
		return new DebugInfoFileEntry(vertx, sourcePath, target);
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
	public Buffer getData() {
		try {
			return fs.readFileBlocking(source).getDelegate();
		} catch (Throwable e) {
			e.printStackTrace();
			throw e;
		}
	}
}
