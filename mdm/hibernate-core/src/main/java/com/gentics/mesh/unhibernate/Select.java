package com.gentics.mesh.unhibernate;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ComparisonRestriction;
import org.hibernate.sql.CompleteRestriction;
import org.hibernate.sql.Restriction;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

/**
 * An extension to SimpleSelect, considering the FROM joins, plus some fields/methods made private in super.
 */
public class Select extends SimpleSelect {

	protected String join;
	protected String groupBy;

	protected final Dialect dialect;
	protected final ParameterMarkerStrategy parameterMarkerStrategy;
	protected int parameterCount;

	public Select(final SessionFactoryImplementor factory) {
		super(factory);
		final JdbcServices jdbcServices = factory.getJdbcServices();
		this.dialect = jdbcServices.getDialect();
		this.parameterMarkerStrategy = jdbcServices.getParameterMarkerStrategy();
	}

	@Override
	public String makeParameterMarker() {
		return parameterMarkerStrategy.createMarker(++parameterCount, null);
	}

	@Override
	public SimpleSelect addRestriction(String predicate) {
		if (StringUtils.isNotBlank(predicate)) {
			restrictions.add((predicate.contains(" ") || predicate.contains("?")) ? new CompleteRestriction(predicate) : new ComparisonRestriction(predicate));
		}
		return this;
	}

	@Override
	public String toStatementString() {
		final StringBuilder buf = new StringBuilder(columns.size() * 10 + tableName.length() + restrictions.size() * 10 + 10);

		applyComment(buf);
		applySelectClause(buf);
		applyFromClause(buf);
		applyJoinClause(buf);
		applyWhereClause(buf);
		applyGroupBy(buf);
		applyOrderBy(buf);

		final String selectString = (lockOptions != null) ? dialect.applyLocksToSql(buf.toString(), lockOptions, null)
				: buf.toString();

		return dialect.transformSelectString(selectString);
	}

	protected void applyJoinClause(StringBuilder buf) {
		if (StringUtils.isNotBlank(join)) {
			buf.append(" ").append(join).append(" ");
		}
	}

	protected void applyGroupBy(StringBuilder buf) {
		if (groupBy != null) {
			buf.append(' ').append(groupBy);
		}
	}

	private void applyComment(StringBuilder buf) {
		if (comment != null) {
			buf.append("/* ").append(Dialect.escapeComment(comment)).append(" */ ");
		}
	}

	private void applySelectClause(StringBuilder buf) {
		buf.append("select ");

		boolean appendComma = false;
		final Set<String> uniqueColumns = new HashSet<>();
		for (int i = 0; i < columns.size(); i++) {
			final String col = columns.get(i);
			final String alias = aliases.get(col);

			if (uniqueColumns.add(alias == null ? col : alias)) {
				if (appendComma) {
					buf.append(", ");
				}
				buf.append(col);
				if (alias != null && !alias.equals(col)) {
					buf.append(" as ").append(alias);
				}
				appendComma = true;
			}
		}
	}

	private void applyFromClause(StringBuilder buf) {
		buf.append(" from ").append(dialect.appendLockHint(lockOptions, tableName));
	}

	private void applyWhereClause(StringBuilder buf) {
		if (restrictions.isEmpty()) {
			return;
		}
		buf.append(" where ");

		for (int i = 0; i < restrictions.size(); i++) {
			if (i > 0) {
				buf.append(" and ");
			}
			final Restriction restriction = restrictions.get(i);
			restriction.render(buf, this);
		}
	}

	private void applyOrderBy(StringBuilder buf) {
		if (orderBy != null) {
			buf.append(' ').append(orderBy);
		}
	}

	public void setJoin(String join) {
		this.join = join;
	}

	public void setGroupBy(String groupBy) {
		this.groupBy = groupBy;
	}
}
