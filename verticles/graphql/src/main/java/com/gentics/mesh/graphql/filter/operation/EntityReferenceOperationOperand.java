package com.gentics.mesh.graphql.filter.operation;

import java.util.Optional;
import java.util.Set;

import com.gentics.graphqlfilter.filter.operation.FilterOperand;
import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.graphqlfilter.filter.operation.Join;

public class EntityReferenceOperationOperand implements FilterOperand<FilterOperation<?>> {

	private final String referenceType;
	private final String fieldName;
	private final FilterOperation<?> filterOperation;
	private final Optional<String> maybeOwner;

	public EntityReferenceOperationOperand(FilterOperation<?> filterOperation, String referenceType, String fieldName, Optional<String> maybeOwner) {
		this.referenceType = referenceType;
		this.fieldName = fieldName;
		this.filterOperation = filterOperation;
		this.maybeOwner = maybeOwner;
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

	public String getReferenceType() {
		return referenceType;
	}

	public String getFieldName() {
		return fieldName;
	}

}
