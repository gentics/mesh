package com.gentics.mesh.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.core.data.schema.FieldSchemaVersionElement;

import jakarta.persistence.criteria.JoinType;

/**
 * An own Join to keep the parameters and meaningful interdependency API.
 * 
 * @author plyhun
 *
 */
public class NativeJoin {

	private final Map<String, Object> parameters = new HashMap<>();
	private final Map<String, Pair<String, NativeFilterJoin>> joins = new HashMap<>();
	private Set<String> whereClauses = new HashSet<>();
	private Optional<FieldSchemaVersionElement<?,?,?,?,?>> maybeVersion = Optional.empty();

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public boolean isAlreadyJoined(String name) {
		return joins.containsKey(name);
	}

	@Override
	public String toString() {
		return joins.keySet().stream().collect(Collectors.joining(", "));
	}

	public NativeJoin addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, JoinType joinType, String dependency) {
		HibernateFilterAnsiJoinFragment fragment = new HibernateFilterAnsiJoinFragment();
		fragment.addJoin(tableName, alias, fkColumns, pkColumns, joinType);
		return addCustomJoin(fragment, alias, dependency);
	}

	public NativeJoin addCustomJoin(NativeFilterJoin join, String alias, String dependency) {
		if (!isAlreadyJoined(alias)) {
			joins.put(alias, Pair.of(dependency, join));
		}
		return this;
	}

	public Map<String, Pair<String, NativeFilterJoin>> getJoins() {
		return joins;
	}

	public Optional<String> getMaybeWhereClause() {
		return Optional.of(whereClauses.stream().collect(Collectors.joining(" AND "))).filter(StringUtils::isNotBlank);
	}

	public NativeJoin appendWhereClause(String newClause) {
		whereClauses.add(newClause);
		return this;
	}

	public NativeJoin appendVersion(FieldSchemaVersionElement<?,?,?,?,?> version) {
		this.maybeVersion = Optional.ofNullable(version);
		return this;
	}

	public Optional<FieldSchemaVersionElement<?, ?, ?, ?, ?>> getMaybeVersion() {
		return maybeVersion;
	}

	public Set<String> getWhereClauses() {
		return Collections.unmodifiableSet(whereClauses);
	}
}
