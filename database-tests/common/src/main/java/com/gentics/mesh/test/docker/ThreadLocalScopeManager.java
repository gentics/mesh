package com.gentics.mesh.test.docker;

import liquibase.Scope;
import liquibase.ScopeManager;

/**
 * ScopeManager implementation, which stores the current scope as thread local.
 * This is necessary, so that liquibase can be used multithreaded.
 */
public class ThreadLocalScopeManager extends ScopeManager {
	private final ThreadLocal<Scope> currentScope = new ThreadLocal<>();
	private final Scope rootScope;

	/**
	 * Create an instance
	 */
	ThreadLocalScopeManager() {
		this.rootScope = Scope.getCurrentScope();
	}

	@Override
	public synchronized Scope getCurrentScope() {
		Scope returnedScope = currentScope.get();

		if (returnedScope == null) {
			returnedScope = rootScope;
		}

		return returnedScope;
	}

	@Override
	protected Scope init(Scope scope) throws Exception {
		return scope;
	}

	@Override
	protected synchronized void setCurrentScope(Scope scope) {
		this.currentScope.set(scope);
	}
}
