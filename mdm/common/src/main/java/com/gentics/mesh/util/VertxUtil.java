package com.gentics.mesh.util;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.MeshUploadOptions;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.VertxImpl;
import io.vertx.ext.web.FileUpload;

/**
 * Various utility functions regarding Vert.x
 */
public final class VertxUtil {
	private VertxUtil() {
	}

	/**
	 * Return the version of Vert.x framework.
	 * 
	 * @return
	 */
	public static final String vertxVersion() {
		return VertxImpl.version();
	}

	/**
	 * Sends a {@link RestModel} to the client. Propagates any error to the failure handler.
	 *
	 * Usage: <code>.subscribe(restModelSender(ac))</code>
	 *
	 * @param rc
	 * @return
	 */
	public static final SingleObserver<RestModel> restModelSender(InternalActionContext rc) {
		return restModelSender(rc, OK);
	}

	/**
	 * Convert a {@link Runnable} into {@link Action};
	 * 
	 * @param r
	 * @return
	 */
	public static final Action intoAction(Runnable r) {
		return () -> r.run();
	}

	/**
	 * Convert a {@link Action} into {@link Runnable}, wrapping its exception with {@link IllegalStateException}
	 * 
	 * @param r
	 * @return
	 */
	public static final Runnable intoRunnable(Action r) {
		return () -> {
			 try {
				r.run();
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		};
	}

	/**
	 * Sends a {@link RestModel} to the client. Propagates any error to the failure handler.
	 *
	 * Usage: <code>.subscribe(restModelSender(ac, OK))</code>
	 *
	 * @param rc
	 * @param statusCode
	 * @return
	 */
	public static final SingleObserver<RestModel> restModelSender(InternalActionContext rc, HttpResponseStatus statusCode) {
		return new SingleObserver<RestModel>() {
			@Override
			public void onSubscribe(Disposable d) {
			}

			@Override
			public void onSuccess(RestModel restModel) {
				rc.send(restModel, statusCode);
			}

			@Override
			public void onError(Throwable e) {
				rc.fail(e);
			}
		};
	}

	/**
	 * Ember a content-disposition header into the response.
	 * 
	 * @param response
	 * @param fileName
	 * @param type
	 */
	public static void addContentDispositionHeader(HttpServerResponse response, String fileName, String type) {
		String encodedFileNameUTF8 = EncodeUtil.encodeForRFC5597(fileName);
		String encodedFileNameISO = EncodeUtil.toISO88591(fileName);
	
		StringBuilder value = new StringBuilder();
		value.append(type + ";");
		value.append(" filename=\"" + encodedFileNameISO + "\";");
		value.append(" filename*=" + encodedFileNameUTF8);
		response.putHeader("content-disposition", value.toString());
	}

	/**
	 * Validate a Vert.x file upload.
	 * 
	 * @param ul
	 * @param fieldName
	 * @param options
	 */
	public static void validateFileUpload(FileUpload ul, String fieldName, MeshOptions options) {
		MeshUploadOptions uploadOptions = options.getUploadOptions();
		long byteLimit = uploadOptions.getByteLimit();
	
		if (ul.size() > byteLimit) {
			String humanReadableFileSize = org.apache.commons.io.FileUtils.byteCountToDisplaySize(ul.size());
			String humanReadableUploadLimit = org.apache.commons.io.FileUtils.byteCountToDisplaySize(byteLimit);
			throw error(BAD_REQUEST, "node_error_uploadlimit_reached", humanReadableFileSize, humanReadableUploadLimit);
		}
	
		if (isEmpty(ul.fileName())) {
			throw error(BAD_REQUEST, "field_binary_error_emptyfilename", fieldName);
		}
		if (isEmpty(ul.contentType())) {
			throw error(BAD_REQUEST, "field_binary_error_emptymimetype", fieldName);
		}
		if (ul.size() < 1) {
			throw error(BAD_REQUEST, "field_binary_error_emptyfile", fieldName, ul.fileName());
		}
	}
}
