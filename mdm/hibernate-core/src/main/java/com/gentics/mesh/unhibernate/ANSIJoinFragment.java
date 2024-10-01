package com.gentics.mesh.unhibernate;

import org.hibernate.AssertionFailure;
import org.hibernate.internal.util.StringHelper;

import jakarta.persistence.criteria.JoinType;

/**
 * SQL join fragment renderer, migrated from Hibernate 5.6.
 */
public class ANSIJoinFragment {

	private boolean hasFilterCondition;
	private boolean hasThetaJoins;

	private StringBuilder buffer = new StringBuilder();
	private StringBuilder conditions = new StringBuilder();

	/**
	 * Adds a join, represented by the given information, to the fragment.
	 *
	 * @param tableName The name of the table being joined.
	 * @param alias     The alias applied to the table being joined.
	 * @param fkColumns The columns (from the table being joined) used to define the
	 *                  join-restriction (the ON)
	 * @param pkColumns The columns (from the table being joined to) used to define
	 *                  the join-restriction (the ON)
	 * @param joinType  The type of join to produce (INNER, etc).
	 */
	public void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, JoinType joinType) {
		addJoin(tableName, alias, fkColumns, pkColumns, joinType, null);
	}

	/**
	 * Adds a join, represented by the given information, to the fragment.
	 *
	 * @param rhsTableName The name of the table being joined (the RHS table).
	 * @param rhsAlias     The alias applied to the table being joined (the alias
	 *                     for the RHS table).
	 * @param lhsColumns   The columns (from the table being joined) used to define
	 *                     the join-restriction (the ON). These are the LHS columns,
	 *                     and are expected to be qualified.
	 * @param rhsColumns   The columns (from the table being joined to) used to
	 *                     define the join-restriction (the ON). These are the RHS
	 *                     columns and are expected to *not* be qualified.
	 * @param joinType     The type of join to produce (INNER, etc).
	 * @param on           Any extra join restrictions
	 */
	public void addJoin(String rhsTableName, String rhsAlias, String[] lhsColumns, String[] rhsColumns,	JoinType joinType, String on) {
		final String joinString;
		switch (joinType) {
		case INNER:
			joinString = " inner join ";
			break;
		case LEFT:
			joinString = " left outer join ";
			break;
		case RIGHT:
			joinString = " right outer join ";
			break;
		default:
			throw new AssertionFailure("undefined join type");
		}

		this.buffer.append(joinString).append(rhsTableName).append(' ').append(rhsAlias).append(" on ");

		for (int j = 0; j < lhsColumns.length; j++) {
			this.buffer.append(lhsColumns[j]).append('=').append(rhsAlias).append('.').append(rhsColumns[j]);
			if (j < lhsColumns.length - 1) {
				this.buffer.append(" and ");
			}
		}

		addCondition(buffer, on);

	}

	public void addJoin(String rhsTableName, String rhsAlias, String[][] lhsColumns, String[] rhsColumns, JoinType joinType, String on) {
		final String joinString;
		switch (joinType) {
		case INNER:
			joinString = " inner join ";
			break;
		case LEFT:
			joinString = " left outer join ";
			break;
		case RIGHT:
			joinString = " right outer join ";
			break;
		default:
			throw new AssertionFailure("undefined join type");
		}

		this.buffer.append(joinString).append(rhsTableName).append(' ').append(rhsAlias).append(" on ");

		if (lhsColumns.length > 1) {
			this.buffer.append("(");
		}
		for (int i = 0; i < lhsColumns.length; i++) {
			for (int j = 0; j < lhsColumns[i].length; j++) {
				this.buffer.append(lhsColumns[i][j]).append('=').append(rhsAlias).append('.').append(rhsColumns[j]);
				if (j < lhsColumns[i].length - 1) {
					this.buffer.append(" and ");
				}
			}
			if (i < lhsColumns.length - 1) {
				this.buffer.append(" or ");
			}
		}
		if (lhsColumns.length > 1) {
			this.buffer.append(")");
		}

		addCondition(buffer, on);
	}

	public String toFromFragmentString() {
		return this.buffer.toString();
	}

	public String toWhereFragmentString() {
		return this.conditions.toString();
	}

	public void addJoins(String fromFragment, String whereFragment) {
		this.buffer.append(fromFragment);
		// where fragment must be empty!
	}

	public ANSIJoinFragment copy() {
		final ANSIJoinFragment copy = new ANSIJoinFragment();
		copy.buffer = new StringBuilder(this.buffer.toString());
		return copy;
	}

	/**
	 * Adds a condition to the join fragment. For each given column a predicate is
	 * built in the form: {@code [alias.[column] = [condition]}
	 *
	 * @param alias     The alias to apply to column(s)
	 * @param columns   The columns to apply restriction
	 * @param condition The restriction condition
	 */
	public void addCondition(String alias, String[] columns, String condition) {
		for (String column : columns) {
			this.conditions.append(" and ").append(alias).append('.').append(column).append(condition);
		}
	}

	public void addCrossJoin(String tableName, String alias) {
		this.buffer.append(", ").append(tableName).append(' ').append(alias);
	}

	public void addCondition(String alias, String[] fkColumns, String[] pkColumns) {
		throw new UnsupportedOperationException();

	}

	public boolean addCondition(String condition) {
		return addCondition(conditions, condition);
	}

	/**
	 * Adds an externally built join fragment.
	 *
	 * @param fromFragmentString The join fragment string
	 */
	public void addFromFragmentString(String fromFragmentString) {
		this.buffer.append(fromFragmentString);
	}

	/**
	 * Adds another join fragment to this one.
	 *
	 * @param ojf The other join fragment
	 */
	public void addFragment(ANSIJoinFragment ojf) {
		if (ojf.hasThetaJoins()) {
			hasThetaJoins = true;
		}
		addJoins(ojf.toFromFragmentString(), ojf.toWhereFragmentString());
	}

	/**
	 * Appends the 'on' condition to the buffer, returning true if the condition was
	 * added. Returns false if the 'on' condition was empty.
	 *
	 * @param buffer The buffer to append the 'on' condition to.
	 * @param on     The 'on' condition.
	 * @return Returns true if the condition was added, false if the condition was
	 *         already in 'on' string.
	 */
	protected boolean addCondition(StringBuilder buffer, String on) {
		if (StringHelper.isNotEmpty(on)) {
			if (!on.startsWith(" and")) {
				buffer.append(" and ");
			}
			buffer.append(on);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * True if the where fragment is from a filter condition.
	 *
	 * @return True if the where fragment is from a filter condition.
	 */
	public boolean hasFilterCondition() {
		return hasFilterCondition;
	}

	public void setHasFilterCondition(boolean b) {
		this.hasFilterCondition = b;
	}

	/**
	 * Determine if the join fragment contained any theta-joins.
	 *
	 * @return {@code true} if the fragment contained theta joins
	 */
	public boolean hasThetaJoins() {
		return hasThetaJoins;
	}

	public void setHasThetaJoins(boolean hasThetaJoins) {
		this.hasThetaJoins = hasThetaJoins;
	}

	public boolean isEmpty() {
		return this.buffer.isEmpty();
	}
}
