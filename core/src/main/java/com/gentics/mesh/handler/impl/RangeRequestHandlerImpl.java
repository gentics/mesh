package com.gentics.mesh.handler.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;
import static io.netty.handler.codec.http.HttpResponseStatus.PARTIAL_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE;

import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gentics.mesh.handler.RangeRequestHandler;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.VertxException;
import io.vertx.core.file.FileProps;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.LRUCache;
import io.vertx.ext.web.impl.Utils;

/**
 * @see RangeRequestHandler
 **/
public class RangeRequestHandlerImpl implements RangeRequestHandler {

	private static final Logger log = LoggerFactory.getLogger(RangeRequestHandlerImpl.class);

	private Map<String, CacheEntry> propsCache;

	private static final Pattern RANGE = Pattern.compile("^bytes=(\\d+)-(\\d*)$");
	private final DateFormat dateTimeFormatter = Utils.createRFC1123DateTimeFormatter();
	private String defaultContentEncoding = Charset.defaultCharset().name();

	private boolean filesReadOnly = DEFAULT_FILES_READ_ONLY;
	private boolean sendVaryHeader = DEFAULT_SEND_VARY_HEADER;
	private long maxAgeSeconds = DEFAULT_MAX_AGE_SECONDS; // One day
	private int maxCacheSize = DEFAULT_MAX_CACHE_SIZE;

	@Override
	public void handle(RoutingContext context, final String file, String contentType) {

		// Look in cache
		CacheEntry entry = propsCache().get(file);
		if (entry != null) {
			HttpServerRequest request = context.request();
			if ((filesReadOnly || entry.shouldUseCached(request))) {
				context.response().setStatusCode(NOT_MODIFIED.code()).end();
				return;
			}
		}

		// verify if the file exists
		isFileExisting(context, file, exists -> {
			if (exists.failed()) {
				context.fail(exists.cause());
				return;
			}

			// file does not exist, continue...
			if (!exists.result()) {
				log.error("Could not find binary file {" + file + "}");
				context.fail(error(INTERNAL_SERVER_ERROR, "field_binary_error_data_not_found"));
				return;
			}

			// Need to read the props from the filesystem
			getFileProps(context, file, res -> {
				if (res.succeeded()) {
					FileProps fprops = res.result();
					if (fprops == null) {
						// File does not exist
						log.error("Could not load file props of file {" + file + "}");
						context.fail(error(INTERNAL_SERVER_ERROR, "field_binary_error_data_not_found"));
					} else {
						propsCache().put(file, new CacheEntry(fprops));
						sendFile(context, file, contentType, fprops);
					}
				} else {
					context.fail(res.cause());
				}
			});
		});
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

		writeCacheHeaders(request, fileProps);

		if (request.method() == HttpMethod.HEAD) {
			request.response().end();
			return;
		}

		if (offset != null) {
			// must return content range
			headers.set("Content-Range", "bytes " + offset + "-" + end + "/" + fileProps.size());
			// return a partial response
			request.response().setStatusCode(PARTIAL_CONTENT.code());

			final Long finalOffset = offset;
			final Long finalEnd = end;
			if (contentType != null) {
				if (contentType.startsWith("text")) {
					request.response().putHeader("Content-Type", contentType + ";charset=" + defaultContentEncoding);
				} else {
					request.response().putHeader("Content-Type", contentType);
				}
			}

			request.response().sendFile(file, finalOffset, finalEnd + 1);
		} else {
			if (contentType != null) {
				if (contentType.startsWith("text")) {
					request.response().putHeader("Content-Type", contentType + ";charset=" + defaultContentEncoding);
				} else {
					request.response().putHeader("Content-Type", contentType);
				}
			}

			request.response().sendFile(file, res2 -> {
				if (res2.failed()) {
					context.fail(res2.cause());
				}
			});
		}

	}

	private synchronized void getFileProps(RoutingContext context, String file, Handler<AsyncResult<FileProps>> resultHandler) {
		// Check whether we can find the props in the cache
		CacheEntry entry = propsCache().get(file);
		if (entry != null) {
			resultHandler.handle(Future.succeededFuture(entry.props));
		}

		FileSystem fs = context.vertx().fileSystem();
		fs.props(file, resultHandler);
	}

	private synchronized void isFileExisting(RoutingContext context, String file, Handler<AsyncResult<Boolean>> resultHandler) {
		FileSystem fs = context.vertx().fileSystem();
		fs.exists(file, resultHandler);
	}

	/**
	 * Create all required header so content can be cache by Caching servers or Browsers
	 *
	 * @param request
	 *            base HttpServerRequest
	 * @param props
	 *            file properties
	 */
	private void writeCacheHeaders(HttpServerRequest request, FileProps props) {

		MultiMap headers = request.response().headers();

		// We use cache-control and last-modified
		// We *do not use* etags and expires (since they do the same thing - redundant)
		headers.set("cache-control", "public, max-age=" + maxAgeSeconds);
		headers.set("last-modified", dateTimeFormatter.format(props.lastModifiedTime()));
		// We send the vary header (for intermediate caches)
		// (assumes that most will turn on compression when using static handler)
		if (sendVaryHeader && request.headers().contains("accept-encoding")) {
			headers.set("vary", "accept-encoding");
		}

		// date header is mandatory
		headers.set("date", dateTimeFormatter.format(new Date()));
	}

	private Date parseDate(String header) {
		try {
			return dateTimeFormatter.parse(header);
		} catch (ParseException e) {
			throw new VertxException(e);
		}
	}

	private Map<String, CacheEntry> propsCache() {
		if (propsCache == null) {
			propsCache = new LRUCache<>(maxCacheSize);
		}
		return propsCache;
	}

	private final class CacheEntry {
		final FileProps props;

		private CacheEntry(FileProps props) {
			this.props = props;
		}

		// return true if there are conditional headers present and they match what is in the entry
		boolean shouldUseCached(HttpServerRequest request) {
			String ifModifiedSince = request.headers().get("if-modified-since");
			if (ifModifiedSince == null) {
				// Not a conditional request
				return false;
			}
			Date ifModifiedSinceDate = parseDate(ifModifiedSince);
			boolean modifiedSince = Utils.secondsFactor(props.lastModifiedTime()) > ifModifiedSinceDate.getTime();
			return !modifiedSince;
		}

	}

}
