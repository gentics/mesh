package com.gentics.mesh.hibernate.data;

import java.util.Optional;

/**
 * Map a query field name onto a database entity element field.
 * 
 * @author plyhun
 *
 */
public interface HibQueryFieldMapper {

	/**
	 * Get an actual database item entity table names (zero or more). Arguments may be asked.
	 * 
	 * @return
	 */
	String[] getHibernateEntityName(Object... arg);

	/**
	 * Get a list of field names, allowed for the usage in GraphQL / REST sorting API
	 * @param noDependencies check, if no dependent entity fields should be included
	 * 
	 * @return null, if getting the list is not supported
	 */
	default String[] getGraphQlSortingFieldNames(boolean noDependencies) {
		return null;
	}

	/**
	 * Map a field name coming from GraphQL filter API onto the actual table column name.
	 * 
	 * @param gqlName
	 * @return
	 */
	default String mapGraphQlFilterFieldName(String gqlName) {
		switch (gqlName) {
		case "uuid": return "dbUuid";
		}
		return gqlName;
	}

	/**
	 * Map a field name coming from GraphQL sort API onto the actual table column name.
	 * 
	 * @param gqlName
	 * @return
	 */
	default String mapGraphQlSortingFieldName(String gqlName) {
		return mapGraphQlFilterFieldName(gqlName);
	}

	/**
	 * Provide a custom SQL operation format for the field
	 * 
	 * @return
	 */
	default Optional<String> maybeGetCustomMapFormat(String gqlName) {
		return Optional.empty();
	}

	/**
	 * Does this mapper serve a field container type?
	 * 
	 * @return
	 */
	default boolean isContent() {
		return false;
	}
}
