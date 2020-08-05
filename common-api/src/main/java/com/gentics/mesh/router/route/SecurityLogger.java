package com.gentics.mesh.router.route;

import static com.gentics.mesh.util.StreamUtil.lazy;
import static com.google.common.net.HttpHeaders.X_FORWARDED_FOR;

import java.util.Optional;
import java.util.function.Supplier;

import io.vertx.core.logging.Logger;
import io.vertx.core.spi.logging.LogDelegate;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;

public class SecurityLogger implements LogDelegate {
	private final Logger delegate;
	private final Supplier<String> remoteAddress;
	private final Supplier<String> userName;

	public SecurityLogger(Logger delegate, RoutingContext context) {
		this.delegate = delegate;
		remoteAddress = lazy(() -> remoteAddress(context));
		userName = lazy(() -> userName(context));
	}

	private String userName(RoutingContext context) {
		return Optional.ofNullable(context.user())
			.map(User::principal)
			.map(principal -> principal.getString("username"))
			.orElse("<no user>");
	}

	private String remoteAddress(RoutingContext context) {
		return forwardedForAddress(context)
			.orElse(context.request().remoteAddress().host());
	}

	private Optional<String> forwardedForAddress(RoutingContext context) {
		return Optional.ofNullable(context.request().getHeader(X_FORWARDED_FOR))
			// https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-For
			.map(addresses -> addresses.split(",")[0].trim());
	}

	private String withUserInfo(Object message) {
		return remoteAddress.get() + ", " + userName.get() + " - " + message;
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
