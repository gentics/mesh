package com.gentics.mesh.database;

import javax.inject.Inject;
import javax.inject.Singleton;
import jakarta.persistence.EntityManager;

/**
 * Workaround for DAOs not being scoped to a transaction.
 */
@Singleton
public class CurrentTransaction {

	@Inject
	public CurrentTransaction() {
	}

	public HibernateTxImpl getTx() {
		return HibernateTx.get().unwrap();
	}

	public EntityManager getEntityManager() {
		return getTx().entityManager();
	}
}
