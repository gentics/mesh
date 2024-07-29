package com.gentics.mesh.handler;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.shared.SharedKeys.API_VERSION_CONTEXT_KEY;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.core.rest.error.GenericRestException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.ext.web.RoutingContext;

/**
 * Puts the requested API version in the routing context with the key {@link #API_VERSION_CONTEXT_KEY}
 */
@Singleton
public class VersionHandlerImpl implements VersionHandler {

	public static final String API_MOUNTPOINT = "/api/:apiversion/*";

	private static final Pattern versionRegex = Pattern.compile("^v(\\d+)$");

	@Inject
	public VersionHandlerImpl() {
	}

	@Override
	public void handle(RoutingContext event) {
		event.put(API_VERSION_CONTEXT_KEY, parseVersion(event));
		event.next();
	}

	private int parseVersion(RoutingContext event) {
		String strVersion = event.pathParam(API_VERSION_CONTEXT_KEY);
		Matcher matcher = versionRegex.matcher(strVersion);
		matcher.find();
		try {
			int version = Integer.parseInt(matcher.group(1));
			if (version < 1 || version > MeshVersion.CURRENT_API_VERSION) {
				throw notFoundError(strVersion);
			}
			return version;
		} catch (RuntimeException ex) {
			throw notFoundError(strVersion);
		}
	}

	private GenericRestException notFoundError(String strVersion) {
		return error(HttpResponseStatus.NOT_FOUND, "error_version_not_found", strVersion, "v" + MeshVersion.CURRENT_API_VERSION);
	}
}
