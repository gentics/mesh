package com.gentics.mesh.storage;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.util.RxUtil;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;

@Singleton
public class LocalBinaryStorage extends AbstractBinaryStorage {

	private static final Logger log = LoggerFactory.getLogger(LocalBinaryStorage.class);
	private final Vertx rxVertx;
	private final FileSystem fileSystem;

	private MeshUploadOptions options;

	@Inject
	public LocalBinaryStorage(MeshOptions options, Vertx rxVertx) {
		this.options = options.getUploadOptions();
		this.rxVertx = rxVertx;
		this.fileSystem = rxVertx.fileSystem();
	}

	@Override
	public Completable moveInPlace(String uuid, String temporaryId) {
		return Completable.defer(() -> {
			if (log.isDebugEnabled()) {
				log.debug("Move temporary upload for uuid '{}' into place using temporaryId '{}'", uuid, temporaryId);
			}
			String source = getTemporaryFilePath(temporaryId);
			String target = getFilePath(uuid);
			if (log.isDebugEnabled()) {
				log.debug("Moving '{}' to '{}'", source, target);
			}
			File uploadFolder = new File(options.getDirectory(), getSegmentedPath(uuid));
			return createParentPath(uploadFolder.getAbsolutePath()).andThen(fileSystem.rxMove(source, target));
		});
	}

	@Override
	public Completable purgeTemporaryUpload(String temporaryId) {
		return Completable.defer(() -> {
			if (log.isDebugEnabled()) {
				log.debug("Purging temporary upload for tempId '{}'", temporaryId);
			}
			String path = getTemporaryFilePath(temporaryId);
			return fileSystem.rxDelete(path);
		});
	}

	@Override
	public Completable storeInTemp(Flowable<Buffer> stream, String temporaryId) {
		Objects.requireNonNull(temporaryId, "The temporary id was not specified.");
		return Completable.defer(() -> {
			String path = getTemporaryFilePath(temporaryId);
			if (log.isDebugEnabled()) {
				log.debug("Saving data for field to path '{}'.", path);
			}
			// First ensure that the temp folder can be created and finally store the data in the folder.
			File tempFolder = new File(options.getDirectory(), "temp");
			return createParentPath(tempFolder.getAbsolutePath())
				.andThen(fileSystem.rxOpen(path, new OpenOptions()).flatMapCompletable(file -> stream
					.map(io.vertx.reactivex.core.buffer.Buffer::new)
					.doOnNext(file::write)
					.ignoreElements()
					.andThen(file.rxFlush())
					.andThen(file.rxClose())
					.doOnError(err -> file.close())));
		});
	}

	private Completable createParentPath(String folderPath) {
		return fileSystem.rxExists(folderPath)
			.flatMapCompletable(exists -> {
				if (exists.booleanValue()) {
					return Completable.complete();
				} else {
					return fileSystem.rxMkdirs(folderPath).onErrorResumeNext(e -> {
						log.error("Failed to create target folder {}", folderPath);
						return Completable.error(error(BAD_REQUEST, "node_error_upload_failed"));
					}).doOnComplete(() -> {
						if (log.isDebugEnabled()) {
							log.debug("Created folders for path {}", folderPath);
						}
					});
				}
			});

	}

	/**
	 * Return the temporary file path.
	 * 
	 * @param binaryUuid
	 * @param temporaryId
	 * @return
	 */
	public String getTemporaryFilePath(String temporaryId) {
		Objects.requireNonNull(temporaryId, "The temporary id was specified.");

		File tempFolder = new File(options.getDirectory(), "temp");
		File binaryFile = new File(tempFolder, temporaryId + ".tmp");
		return binaryFile.getAbsolutePath();
	}

	public String getFilePath(String binaryUuid) {
		Objects.requireNonNull(binaryUuid, "The binary uuid was not specified.");
		File folder = new File(options.getDirectory(), getSegmentedPath(binaryUuid));
		File binaryFile = new File(folder, binaryUuid + ".bin");
		return binaryFile.getAbsolutePath();
	}

	@Override
	public boolean exists(BinaryGraphField field) {
		String uuid = field.getBinary().getUuid();
		return new File(getFilePath(uuid)).exists();
	}

	@Override
	public Flowable<Buffer> read(String binaryUuid) {
		String path = getFilePath(binaryUuid);
		Flowable<Buffer> obs = fileSystem
			.rxOpen(path, new OpenOptions())
			.toFlowable()
			.flatMap(RxUtil::toBufferFlow);
		return obs;
	}

	@Override
	public Buffer readAllSync(String binaryUuid) {
		return fileSystem.getDelegate().readFileBlocking(getFilePath(binaryUuid));
	}

	@Override
	public String getLocalPath(String binaryUuid) {
		return getFilePath(binaryUuid);
	}

	/**
	 * Generate the segmented path for the given binary uuid.
	 * 
	 * @param binaryUuid
	 * @return
	 */
	public static String getSegmentedPath(String binaryUuid) {
		String partA = binaryUuid.substring(0, 2);
		String partB = binaryUuid.substring(2, 4);
		StringBuffer buffer = new StringBuffer();
		buffer.append(File.separator);
		buffer.append(partA);
		buffer.append(File.separator);
		buffer.append(partB);
		buffer.append(File.separator);
		return buffer.toString();
	}

	@Override
	public Completable delete(String binaryUuid) {
		String path = getFilePath(binaryUuid);
		return rxVertx.fileSystem()
			.rxDelete(path)
			// Don't fail if the file is not even in the local storage
			.onErrorComplete(e -> {
				Throwable cause = e.getCause();
				if (cause != null) {
					return cause instanceof NoSuchFileException;
				} else {
					return e instanceof NoSuchFileException;
				}
			});
	}

}
