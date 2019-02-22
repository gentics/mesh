package com.gentics.mesh.storage;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
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

	private final MeshOptions options;
	private final Vertx rxVertx;

	@Inject
	public LocalBinaryStorage(MeshOptions options, Vertx rxVertx) {
		this.options = options;
		this.rxVertx = rxVertx;
	}

	@Override
	public Completable moveInPlace(String uuid, String temporaryId) {
		return Completable.defer(() -> {
			FileSystem fileSystem = rxVertx.fileSystem();
			log.debug("Move temporary upload for uuid '{}' into place.", uuid);
			String tempPath = getTemporaryFilePath(uuid, temporaryId);
			String finalPath = getFilePath(uuid);
			return fileSystem.rxMove(tempPath, finalPath);
		});
	}

	@Override
	public Completable purgeTemporaryUpload(String uuid, String temporaryId) {
		return Completable.defer(() -> {
			FileSystem fileSystem = rxVertx.fileSystem();
			log.debug("Purging temporary upload for uuid '{}'", uuid);
			String path = getTemporaryFilePath(uuid, temporaryId);
			return fileSystem.rxDelete(path);
		});
	}

	@Override
	public Completable storeInTemp(Flowable<Buffer> stream, String uuid, String temporaryId) {
		Objects.requireNonNull(uuid, "The binary uuid was not specified");
		return Completable.defer(() -> {
			FileSystem fileSystem = rxVertx.fileSystem();
			String path = getTemporaryFilePath(uuid, temporaryId);
			log.debug("Saving data for field to path {" + path + "}");
			MeshUploadOptions uploadOptions = options.getUploadOptions();
			File uploadFolder = new File(uploadOptions.getDirectory(), getSegmentedPath(uuid));
			return createParentPath(uploadFolder.getAbsolutePath())
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
		FileSystem fileSystem = rxVertx.fileSystem();
		return fileSystem.rxExists(folderPath)
			.flatMapCompletable(exists -> {
				if (exists.booleanValue()) {
					return Completable.complete();
				} else {
					return fileSystem.rxMkdirs(folderPath).onErrorResumeNext(e -> {
						log.error("Failed to create target folder {" + folderPath + "}");
						return Completable.error(error(BAD_REQUEST, "node_error_upload_failed"));
					}).doOnComplete(() -> {
						if (log.isDebugEnabled()) {
							log.debug("Created folders for path {" + folderPath + "}");
						}
					});
				}
			});

	}

	/**
	 * Return the absolute path to the binary data for the given uuid.
	 * 
	 * @param binaryUuid
	 * @param temporaryId
	 * @return
	 */
	public String getFilePath(String binaryUuid, String temporaryId) {
		Objects.requireNonNull(binaryUuid, "The binary uuid was not specified");
		File folder = new File(options.getUploadOptions().getDirectory(), getSegmentedPath(binaryUuid));
		String postfix = temporaryId == null ? "" : "." + temporaryId + ".temp";
		File binaryFile = new File(folder, binaryUuid + ".bin" + postfix);
		return binaryFile.getAbsolutePath();
	}

	public String getTemporaryFilePath(String binaryUuid, String temporaryId) {
		return getFilePath(binaryUuid, temporaryId);
	}

	public String getFilePath(String binaryUuid) {
		return getFilePath(binaryUuid, null);
	}

	@Override
	public boolean exists(BinaryGraphField field) {
		String uuid = field.getBinary().getUuid();
		return new File(getFilePath(uuid)).exists();
	}

	@Override
	public Flowable<Buffer> read(String binaryUuid) {
		String path = getFilePath(binaryUuid);
		Flowable<Buffer> obs = FileSystem.newInstance(Mesh.vertx().fileSystem())
			.rxOpen(path, new OpenOptions())
			.toFlowable()
			.flatMap(RxUtil::toBufferFlow);
		return obs;
	}

	@Override
	public Buffer readAllSync(String binaryUuid) {
		return Mesh.vertx().fileSystem().readFileBlocking(getFilePath(binaryUuid));
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
