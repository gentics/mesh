package com.gentics.mesh.util;

import static com.gentics.mesh.util.MimeTypeUtils.DEFAULT_BINARY_MIME_TYPE;

import io.reactivex.Single;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileProps;
import io.vertx.core.file.OpenOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;

/**
 * An open file in read mode with its filesystem properties.
 */
public class PropReadFileStream {
	private FileProps props;
	private AsyncFile file;
	private String path;
	private String mimeType;

	private static final OpenOptions openOptions = new OpenOptions().setRead(true);

	/**
	 * Convenience constructor where the MIME type is automatically derived from the filename.
	 *
	 * @param props
	 * @param file
	 * @param path
	 */
	private PropReadFileStream(FileProps props, AsyncFile file, String path) {
		this(
			props,
			file,
			path,
			MimeTypeUtils.getMimeTypeForFilename(path).orElse(DEFAULT_BINARY_MIME_TYPE));
	}

	/**
	 * Default constructor.
	 * @param props
	 * @param file
	 * @param path
	 * @param mimeType
	 */
	private PropReadFileStream(FileProps props, AsyncFile file, String path, String mimeType) {
		this.props = props;
		this.file = file;
		this.path = path;
		this.mimeType = mimeType;
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

	/**
	 * Gets the MIME type of the file.
	 *
	 * @return The MIME type of the file
	 */
	public String getMimeType() {
		return mimeType;
	}
}
