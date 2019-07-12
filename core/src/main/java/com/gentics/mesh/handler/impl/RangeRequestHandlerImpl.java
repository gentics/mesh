package com.gentics.mesh.handler.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.PARTIAL_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.handler.RangeRequestHandler;

import io.reactivex.Single;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.LRUCache;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileProps;

/**
 * @see RangeRequestHandler
 **/
@Singleton
public class RangeRequestHandlerImpl implements RangeRequestHandler {

	private static final Logger log = LoggerFactory.getLogger(RangeRequestHandlerImpl.class);

	private Map<String, FileProps> propsCache;

	private static final Pattern RANGE = Pattern.compile("^bytes=(\\d+)-(\\d*)$");
	private String defaultContentEncoding = Charset.defaultCharset().name();

	private int maxCacheSize = DEFAULT_MAX_CACHE_SIZE;
	private final Vertx rxVertx;

	@Inject
	public RangeRequestHandlerImpl(Vertx rxVertx) {
		this.rxVertx = rxVertx;
	}

	@Override
	public void handle(RoutingContext context, final String file, String contentType) {

		// Need to read the props from the filesystem
		getFileProps(file).subscribe(fprops -> {
			if (fprops == null) {
				// File does not exist
				log.error("Could not load file props of file {" + file + "}");
				context.fail(error(NOT_FOUND, "node_error_binary_data_not_found"));
			} else {
				propsCache().put(file, fprops);
				sendFile(context, file, contentType, fprops);
			}
		}, context::fail);
	}

	private void sendFile(RoutingContext context, String file, String contentType, FileProps fileProps) {
		HttpServerRequest request = context.request();

		Long offset = null;
		Long end = null;
		MultiMap headers = null;

		// check if the client is making a range request
		String range = request.getHeader("Range");
		// end byte is length - 1
		end = fileProps.size() - 1;

		if (range != null) {
			Matcher m = RANGE.matcher(range);
			if (m.matches()) {
				try {
					String part = m.group(1);
					// offset cannot be empty
					offset = Long.parseLong(part);
					// offset must fall inside the limits of the file
					if (offset < 0 || offset >= fileProps.size()) {
						throw new IndexOutOfBoundsException();
					}
					// length can be empty
					part = m.group(2);
					if (part != null && part.length() > 0) {
						// ranges are inclusive
						end = Math.min(end, Long.parseLong(part));
						// end offset must not be smaller than start offset
						if (end < offset) {
							throw new IndexOutOfBoundsException();
						}
					}
				} catch (NumberFormatException | IndexOutOfBoundsException e) {
					context.response().putHeader("Content-Range", "bytes */" + fileProps.size());
					context.fail(REQUESTED_RANGE_NOT_SATISFIABLE.code());
					return;
				}
			}

			// notify client we support range requests
			headers = request.response().headers();
			headers.set("Accept-Ranges", "bytes");
			// send the content length even for HEAD requests
			headers.set("Content-Length", Long.toString(end + 1 - (offset == null ? 0 : offset)));
		}

		if (request.method() == HttpMethod.HEAD) {
			request.response().end();
			return;
		}

		if (contentType != null) {
			if (contentType.startsWith("text")) {
				request.response().putHeader("Content-Type", contentType + ";charset=" + defaultContentEncoding);
			} else {
				request.response().putHeader("Content-Type", contentType);
			}
		}
		if (offset != null) {
			// must return content range
			headers.set("Content-Range", "bytes " + offset + "-" + end + "/" + fileProps.size());
			// return a partial response
			request.response().setStatusCode(PARTIAL_CONTENT.code());

			final Long finalOffset = offset;
			final Long finalLength = end + 1 - offset;

			request.response().sendFile(file, finalOffset, finalLength);
		} else {
			// Return the full file
			request.response().sendFile(file, res2 -> {
				if (res2.failed()) {
					context.fail(res2.cause());
				}
			});
		}

	}

	private Single<FileProps> getFileProps(String file) {
		// Check whether we can find the props in the cache
		FileProps entry = propsCache().get(file);
		if (entry != null) {
			return Single.just(entry);
		}

		return rxVertx.fileSystem().rxProps(file);
	}

	private Map<String, FileProps> propsCache() {
		if (propsCache == null) {
			propsCache = new LRUCache<>(maxCacheSize);
		}
		return propsCache;
	}

}
