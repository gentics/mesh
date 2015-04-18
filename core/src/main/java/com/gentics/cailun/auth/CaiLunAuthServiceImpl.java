package com.gentics.cailun.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.apex.RoutingContext;
import io.vertx.ext.apex.Session;
import io.vertx.ext.auth.AuthService;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.shiro.authz.Permission;

import com.gentics.cailun.core.data.model.auth.User;

/**
 * Custom auth* service implementation that adds support for object based permission checks
 * 
 * @author johannes2
 *
 */
public class CaiLunAuthServiceImpl implements AuthService, Handler<Long> {

	private final Vertx vertx;
	private final ExposingShiroAuthProvider provider;
	protected final Map<String, LoginSession> loginSessions = new ConcurrentHashMap<>();
	private long reaperPeriod = AuthService.DEFAULT_REAPER_PERIOD;
	private long timerID;
	private boolean closed;

	public CaiLunAuthServiceImpl(Vertx vertx, JsonObject config, ExposingShiroAuthProvider provider) {
		this.vertx = vertx;
		this.provider = provider;
		//provider.init(config);
		setTimer();
	}

	public String createLoginSession(long timeout, Object principal) {
		String id = UUID.randomUUID().toString();
		loginSessions.put(id, new LoginSession(timeout, principal));
		return id;
	}

	// @Override
	// public AuthService login(JsonObject credentials, Handler<AsyncResult<String>> resultHandler) {
	// loginWithTimeout(credentials, DEFAULT_LOGIN_TIMEOUT, resultHandler);
	// return this;
	// }
	//
	// @Override
	// public AuthService loginWithTimeout(JsonObject credentials, long timeout, Handler<AsyncResult<String>> resultHandler) {
	// provider.login(credentials, res -> {
	// if (res.succeeded()) {
	// Object principal = res.result();
	// if (principal != null) {
	// String loginSessionID = createLoginSession(timeout, principal);
	// resultHandler.handle(Future.succeededFuture(loginSessionID));
	// } else {
	// resultHandler.handle(Future.failedFuture("null principal"));
	// }
	// } else {
	// resultHandler.handle(Future.failedFuture(res.cause()));
	// }
	// });
	// return this;
	// }

	@Override
	public AuthService logout(String loginID, Handler<AsyncResult<Void>> resultHandler) {
		LoginSession session = loginSessions.remove(loginID);
		resultHandler.handle(session == null ? Future.failedFuture("not logged in") : Future.succeededFuture());
		return this;
	}

	@Override
	public AuthService refreshLoginSession(String loginID, Handler<AsyncResult<Void>> resultHandler) {
		LoginSession session = loginSessions.get(loginID);
		if (session != null) {
			session.touch();
		}
		resultHandler.handle(session == null ? Future.failedFuture("not logged in") : Future.succeededFuture());
		return this;
	}

	@Override
	public AuthService setReaperPeriod(long reaperPeriod) {
		this.reaperPeriod = reaperPeriod;
		return this;
	}

	@Override
	public synchronized void handle(Long tid) {
		long now = System.currentTimeMillis();
		Iterator<Map.Entry<String, LoginSession>> iter = loginSessions.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, LoginSession> entry = iter.next();
			LoginSession session = entry.getValue();
			if (now - session.lastAccessed() > session.timeout()) {
				iter.remove();
			}
		}
		if (!closed) {
			setTimer();
		}
	}

	private void setTimer() {
		if (reaperPeriod != 0) {
			timerID = vertx.setTimer(reaperPeriod, this);
		}
	}

	@Override
	public synchronized void start() {
		closed = false;
		setTimer();
	}

	@Override
	public synchronized void stop() {
		closed = true;
		loginSessions.clear();
		if (timerID != -1) {
			vertx.cancelTimer(timerID);
		}
	}

	public User getUser(RoutingContext rc) {
		return getUser(rc.session().id());
	}

	public User getUser(String loginID) {
		LoginSession session = loginSessions.get(loginID);
		if (session.principal() != null && session.principal() instanceof User) {
			return (User) session.principal();
		} else {
			return null;
		}
	}

	public Map<String, LoginSession> getLoginSessions() {
		return loginSessions;
	}

//	public boolean hasPermission(Session session, Permission permission) {
//		session.hasPermission(permission, resultHandler);
//		LoginSession loginSession = loginSessions.get(loginID);
//		if (loginSession != null) {
//			if (loginSession.principal() != null && loginSession.principal() instanceof User) {
//				User user = (User) loginSession.principal();
//				boolean hasPerm = getAuthRealm().hasPermission(user.getPrincipalId(), permission);
//				return hasPerm;
//			}
//			return false;
//		}
//		return false;
//
//	}

	public EnhancedShiroAuthRealmImpl getAuthRealm() {
		return (EnhancedShiroAuthRealmImpl) provider.getRealm();
	}

	@Override
	public AuthService hasRole(String loginID, String role, Handler<AsyncResult<Boolean>> resultHandler) {
		throw new NotImplementedException("Not yet implemented");
	}

	@Override
	public AuthService hasRoles(String loginID, Set<String> roles, Handler<AsyncResult<Boolean>> resultHandler) {
		throw new NotImplementedException("Not yet implemented");
	}

	@Override
	public AuthService hasPermission(String loginID, String permission, Handler<AsyncResult<Boolean>> resultHandler) {
		throw new NotImplementedException("Not yet implemented");
	}

	@Override
	public AuthService hasPermissions(String loginID, Set<String> permissions, Handler<AsyncResult<Boolean>> resultHandler) {
		throw new NotImplementedException("Not yet implemented");
	}

	@Override
	public AuthService login(JsonObject principal, JsonObject credentials, Handler<AsyncResult<String>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AuthService loginWithTimeout(JsonObject principal, JsonObject credentials, long timeout, Handler<AsyncResult<String>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}

}
