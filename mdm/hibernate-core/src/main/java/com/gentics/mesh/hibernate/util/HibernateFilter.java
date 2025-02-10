package com.gentics.mesh.hibernate.util;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gentics.mesh.query.NativeJoin;

/**
 * A filter instance for SQL native query, backed by Hibernate ORM.
 * 
 * @author plyhun
 *
 */
public final class HibernateFilter {
	private final String sqlFilter;
	private final Set<NativeJoin> sqlJoin;
	private final Map<String, Object> parameters;
	private final Optional<String> maybeFieldType;

	public HibernateFilter(String sqlFilter, Set<NativeJoin> sqlJoin, Map<String, Object> parameters) {
		this(sqlFilter, sqlJoin, parameters, Optional.empty());
	}

	public HibernateFilter(String sqlFilter, Set<NativeJoin> sqlJoin, Map<String, Object> parameters, Optional<String> maybeFieldType) {
		this.sqlFilter = sqlFilter;
		this.sqlJoin = sqlJoin;
		this.parameters = parameters;
		this.maybeFieldType = maybeFieldType;
	}

	/**
	 * Get a WHERE clause part of a filter.
	 */
	public String getSqlFilter() {
		return sqlFilter;
	}

	/**
	 * Get JOINs, that are required by this filter.
	 * 
	 * @return
	 */
	public Set<NativeJoin> getRawSqlJoins() {
		return sqlJoin;
	}

	/**
	 * Get filter parameters.
	 * 
	 * @return
	 */
	public Map<String, Object> getParameters() {
		return parameters;
	}

	/**
	 * Merge this filter with another one.
	 * 
	 * @param other
	 * @param filterMerger a WHERE clause merge function. Most obvious usecase is to join parts together via AND/OR combinations.
	 * @return
	 */
	public HibernateFilter merge(HibernateFilter other, BiFunction<String, String, String> filterMerger) {
		if (other == null) {
			return this;
		}
		return new HibernateFilter(
				filterMerger.apply(sqlFilter, other.sqlFilter), 
				Stream.of(sqlJoin, other.sqlJoin).flatMap(j -> j.stream()).collect(Collectors.toSet()), 
				Stream.of(parameters, other.parameters).flatMap(m -> m.entrySet().stream()).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (a,b) -> a))
			);
	}

	public Optional<String> getMaybeFieldType() {
		return maybeFieldType;
	}
}
