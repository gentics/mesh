package com.gentics.mesh.database;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.jdbc.AbstractWork;

import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.db.TxAction;
import com.gentics.mesh.util.ExceptionUtil;

/**
 * Class responsible for executing some work inside a hibernate session and storing a result
 * @param <T>
 */
public class HibernateWork<T> extends AbstractWork {

	private final Tx tx;
	private final TxAction<T> txHandler;
	private T result = null;
	
	public HibernateWork(Tx tx, TxAction<T> txHandler) {
		this.tx = tx;
		this.txHandler = txHandler;
	}

	@Override
	public void execute(Connection connection) throws SQLException {
		try {
			result = txHandler.handle(tx);
		} catch (Throwable e) {
			throw ExceptionUtil.rethrow(e);
		}
	}

	public T getResult() {
		return result;
	}
}
