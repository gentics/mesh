package com.gentics.mesh.graphql.filter.operation;

import java.util.Optional;
import java.util.Set;

import com.gentics.graphqlfilter.filter.operation.FilterOperand;
import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.graphqlfilter.filter.operation.Join;

/**
 * Wrapper for list item operand.
 * 
 * @author plyhun
 *
 */
public class ListItemOperationOperand implements FilterOperand<FilterOperation<?>> {

	private final FilterOperation<?> filterOperation;
	private final Optional<String> maybeOwner;
	private final boolean negate;
	private final boolean checkIfExists;

	public ListItemOperationOperand(FilterOperation<?> filterOperation, Optional<String> maybeOwner, boolean negate, boolean checkIfExists) {
		this.filterOperation = filterOperation;
		this.maybeOwner = maybeOwner;
		this.negate = negate;
		this.checkIfExists = checkIfExists;
	}

	@Override
	public String toSql() {
		return filterOperation.toSql();
	}

	@Override
	public Set<Join> getJoins(Set<Join> parent) {
		return filterOperation.getJoins(parent);
	}

	@Override
	public FilterOperation<?> getValue() {
		return filterOperation;
	}

	@Override
	public boolean isLiteral() {
		return false;
	}

	@Override
	public Optional<String> maybeGetOwner() {
		return maybeOwner;
	}

	public boolean isNegate() {
		return negate;
	}

	public boolean isCheckIfExists() {
		return checkIfExists;
	}
}
