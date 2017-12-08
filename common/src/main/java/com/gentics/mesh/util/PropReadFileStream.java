package com.gentics.mesh.util;

import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileProps;
import io.vertx.core.file.OpenOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.file.FileSystem;
import rx.Single;

/**
 * An open file in read mode with its filesystem properties.
 */
public class PropReadFileStream {
	private FileProps props;
	private AsyncFile file;
	private String path;

	private static final OpenOptions openOptions = new OpenOptions().setRead(true);

	private PropReadFileStream(FileProps props, AsyncFile file, String path) {
		this.props = props;
		this.file = file;
		this.path = path;
	}

	/**
	 * Opens a file and reads its properties.
	 * 
	 * @param path
	 *            Path to the file
	 * @return
	 */
	public static Single<PropReadFileStream> openFile(Vertx vertx, String path) {
		FileSystem fs = vertx.fileSystem();
		return Single.zip(fs.rxProps(path).map(props -> props.getDelegate()), fs.rxOpen(path, openOptions).map(file -> file.getDelegate()),
				(props, file) -> {
					return new PropReadFileStream(props, file, path);
				});
	}

	/**
	 * Gets the filesystem props
	 * 
	 * @return
	 */
	public FileProps getProps() {
		return props;
	}

	/**
	 * Gets the opened file, ready to read.
	 * 
	 * @return
	 */
	public AsyncFile getFile() {
		return file;
	}

	/**
	 * Return the file path.
	 * 
	 * @return
	 */
	public String getPath() {
		return path;
	}
}
