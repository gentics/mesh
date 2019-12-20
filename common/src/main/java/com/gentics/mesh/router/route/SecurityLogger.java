package com.gentics.mesh.router.route;

import static com.gentics.mesh.util.RxUtil.executeBlocking;
import static com.gentics.mesh.util.RxUtil.fromNullable;
import static com.google.common.net.HttpHeaders.X_FORWARDED_FOR;

import java.util.Optional;

import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.spi.logging.LogDelegate;
import io.vertx.ext.web.RoutingContext;
import io.vertx.reactivex.core.Vertx;

public class SecurityLogger implements LogDelegate {
	private final Logger delegate;
	private final String remoteAddress;
	private final String userName;

	private SecurityLogger(Logger delegate, String remoteAddress, String userName) {
		this.delegate = delegate;
		this.remoteAddress = remoteAddress;
		this.userName = userName;
	}

	/**
	 * Creates a security logger which adds the IP address and the user name to log messages.
	 * A single is returned because a transaction is required to get the user name.
	 * @param vertx
	 * @param delegate
	 * @param context
	 * @return
	 */
	public static Single<SecurityLogger> forCurrentUser(Vertx vertx, Logger delegate, RoutingContext context) {
		return userName(vertx, context)
			.map(username -> new SecurityLogger(delegate, remoteAddress(context), username));
	}

	private static Single<String> userName(Vertx vertx, RoutingContext context) {
		return fromNullable(context.user())
			.flatMap(user -> executeBlocking(vertx, user::principal))
			.map(principal -> principal.getString("username"))
			.toSingle("<no user>");
	}

	private static String remoteAddress(RoutingContext context) {
		return forwardedForAddress(context)
			.orElse(context.request().remoteAddress().host());
	}

	private static Optional<String> forwardedForAddress(RoutingContext context) {
		return Optional.ofNullable(context.request().getHeader(X_FORWARDED_FOR))
			// https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-For
			.map(addresses -> addresses.split(",")[0].trim());
	}

	private String withUserInfo(Object message) {
		return remoteAddress + ", " + userName + " - " + message;
	}

	@Override
	public boolean isWarnEnabled() {
		return delegate.isWarnEnabled();
	}

	@Override
	public boolean isInfoEnabled() {
		return delegate.isInfoEnabled();
	}

	@Override
	public boolean isDebugEnabled() {
		return delegate.isDebugEnabled();
	}

	@Override
	public boolean isTraceEnabled() {
		return delegate.isTraceEnabled();
	}

	@Override
	public void fatal(Object message) {
		delegate.fatal(withUserInfo(message));
	}

	@Override
	public void fatal(Object message, Throwable t) {
		delegate.fatal(withUserInfo(message), t);
	}

	@Override
	public void error(Object message) {
		delegate.error(withUserInfo(message));
	}

	@Override
	public void error(Object message, Throwable t) {
		delegate.error(withUserInfo(message), t);
	}

	@Override
	public void error(Object message, Object... objects) {
		delegate.error(withUserInfo(message), objects);
	}

	@Override
	public void error(Object message, Throwable t, Object... objects) {
		delegate.error(withUserInfo(message), t, objects);
	}

	@Override
	public void warn(Object message) {
		delegate.warn(withUserInfo(message));
	}

	@Override
	public void warn(Object message, Throwable t) {
		delegate.warn(withUserInfo(message), t);
	}

	@Override
	public void warn(Object message, Object... objects) {
		delegate.warn(withUserInfo(message), objects);
	}

	@Override
	public void warn(Object message, Throwable t, Object... objects) {
		delegate.warn(withUserInfo(message), t, objects);
	}

	@Override
	public void info(Object message) {
		delegate.info(withUserInfo(message));
	}

	@Override
	public void info(Object message, Throwable t) {
		delegate.info(withUserInfo(message), t);
	}

	@Override
	public void info(Object message, Object... objects) {
		delegate.info(withUserInfo(message), objects);
	}

	@Override
	public void info(Object message, Throwable t, Object... objects) {
		delegate.info(withUserInfo(message), t, objects);
	}

	@Override
	public void debug(Object message) {
		delegate.debug(withUserInfo(message));
	}

	@Override
	public void debug(Object message, Throwable t) {
		delegate.debug(withUserInfo(message), t);
	}

	@Override
	public void debug(Object message, Object... objects) {
		delegate.debug(withUserInfo(message), objects);
	}

	@Override
	public void debug(Object message, Throwable t, Object... objects) {
		delegate.debug(withUserInfo(message), t, objects);
	}

	@Override
	public void trace(Object message) {
		delegate.trace(withUserInfo(message));
	}

	@Override
	public void trace(Object message, Throwable t) {
		delegate.trace(withUserInfo(message), t);
	}

	@Override
	public void trace(Object message, Object... objects) {
		delegate.trace(withUserInfo(message), objects);
	}

	@Override
	public void trace(Object message, Throwable t, Object... objects) {
		delegate.trace(withUserInfo(message), t, objects);
	}

	public LogDelegate getDelegate() {
		return delegate.getDelegate();
	}
}
