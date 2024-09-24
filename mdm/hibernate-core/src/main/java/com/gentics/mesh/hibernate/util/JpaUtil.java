/*
 * From https://github.com/chelu/jdal/blob/master/core/src/main/java/org/jdal/dao/jpa/JpaUtils.java
 *
 * Copyright 2009-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gentics.mesh.hibernate.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.hibernate.query.criteria.JpaCriteriaQuery;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;
import jakarta.persistence.metamodel.CollectionAttribute;
import jakarta.persistence.metamodel.EntityType;

/**
 * Utility class for dealing with JPA API
 *
 * @author Jose Luis Martin
 * @since 1.1
 */
public final class JpaUtil {

	private static String ALIAS_PATTERN_STRING = "(?<=from)\\s+(?:\\S+)\\s+(?:as\\s+)*(\\w*)";
	private static Pattern ALIAS_PATTERN = Pattern.compile(ALIAS_PATTERN_STRING, Pattern.CASE_INSENSITIVE);
	private static String FROM_PATTERN_STRING = "(from.*+)";
	private static Pattern FROM_PATTERN = Pattern.compile(FROM_PATTERN_STRING, Pattern.CASE_INSENSITIVE);
	private static volatile int aliasCount = 0;

	/**
	 * Result count from a CriteriaQuery
	 * @param em Entity Manager
	 * @param criteria Criteria Query to count results
	 * @return row count
	 */
	public static <R, F> Long count(EntityManager em, CriteriaQuery<R> criteria) {
		return em.createQuery(countCriteria(em, criteria)).getSingleResult();
	}

	/**
	 * Given an hql string query, return a transformed query to fetch a count
	 * @param hql
	 * @return
	 */
	public static String toCountHQL(String hql) {
		int selectIndex = hql.indexOf("select");
		int fromIndex = hql.indexOf("from");
		String select = hql.substring(0, "select".length() + 1);
		String count = "count(" + hql.substring(selectIndex + "select".length(), fromIndex) + ") ";
		String rest = hql.substring(fromIndex);

		return select + count + rest;
	}

	/**
	 * Create a row count CriteriaQuery from a CriteriaQuery
	 * @param em entity manager
	 * @param criteria source criteria
	 * @return row count CriteriaQuery
	 */
	@SuppressWarnings("unchecked")
	public static <R, F> CriteriaQuery<Long> countCriteria(EntityManager em, CriteriaQuery<R> criteria) {
		return JpaCriteriaQuery.class.cast(criteria).createCountQuery();
	}

	/**
	 * Gets The result alias, if none set a default one and return it
	 * @param selection
	 * @return root alias or generated one
	 */
	public static synchronized <T> String getOrCreateAlias(Selection<T> selection) {
		// reset alias count
		if (aliasCount > 1000)
			aliasCount = 0;

		String alias = selection.getAlias();
		if (alias == null) {
			alias = "JDAL_generatedAlias" + aliasCount++;
			selection.alias(alias);
		}
		return alias;

	}

	/**
	 * Find Root of result type
	 * @param query criteria query
	 * @return the root of result type or null if none
	 */
	public static  <T> Root<T> findRoot(CriteriaQuery<T> query) {
		return findRoot(query, query.getResultType());
	}

	/**
	 * Find the Root with type class on CriteriaQuery Root Set
	 * @param <T> root type
	 * @param query criteria query
	 * @param queryRootClass root type
	 * @return Root<T> of null if none
	 */
	@SuppressWarnings("unchecked")
	public static <T> Root<T> findRoot(CriteriaQuery<?> query, Class<T> queryRootClass) {

		for (Root<?> r : query.getRoots()) {
			if (queryRootClass.equals(r.getJavaType())) {
				return (Root<T>) r;
			}
		}
		return null;
	}

	/**
	 * Create a count query string from a query string
	 * @param queryString string to parse
	 * @return the count query string
	 */
	public static String createCountQueryString(String queryString) {
		return queryString.replaceFirst("^.*(?i)from", "select count (*) from ");
	}

	/**
	 * Gets the alias of root entity of JQL query
	 * @param queryString JQL query
	 * @return alias of root entity.
	 */
	public static String getAlias(String queryString) {
		Matcher m = ALIAS_PATTERN.matcher(queryString);
		return m.find() ? m.group(1) : null;
	}

	/**
	 * Add order by clause to queryString
	 * @param queryString JPL Query String
	 * @param propertyPath Order properti
	 * @param asc true if ascending
	 * @return JQL Query String with Order clause appened.
	 */
	public static String addOrder(String queryString, String propertyPath, boolean asc ) {

		if (StringUtils.containsIgnoreCase(queryString, "order by")) {
			return queryString;
		}

		StringBuilder sb = new StringBuilder(queryString);
		sb.append(" ORDER BY ");
		sb.append(getAlias(queryString));
		sb.append(".");
		sb.append(propertyPath);
		sb.append(" ");
		sb.append(asc ? "ASC" : "DESC");

		return sb.toString();
	}

	/**
	 * Gets Query String for selecting primary keys
	 * @param queryString the original query
	 * @param name primary key name
	 * @return query string
	 */
	public static String getKeyQuery(String queryString, String name) {
		Matcher m = FROM_PATTERN.matcher(queryString);
		if (m.find()) {
			StringBuilder sb = new StringBuilder("SELECT ");
			sb.append(getAlias(queryString));
			sb.append(".");
			sb.append(name);
			sb.append(" ");
			sb.append(m.group());
			return sb.toString();
		}

		return null;
	}

	/**
	 * Copy Criteria without Selection.
	 * @param from source Criteria.
	 * @param to destination Criteria.
	 */
	public static void  copyCriteriaNoSelection(CriteriaQuery<?> from, CriteriaQuery<?> to) {
		copyCriteriaWithoutSelectionAndOrder(from, to, true);
		to.orderBy(from.getOrderList());
	}

	/**
	 * Copy criteria without selection and order.
	 * @param from source Criteria.
	 * @param to destination Criteria.
	 */
	private static void copyCriteriaWithoutSelectionAndOrder(
		CriteriaQuery<?> from, CriteriaQuery<?> to, boolean copyFetches) {
		if (isEclipseLink(from) && from.getRestriction() != null) {
			// EclipseLink adds roots from predicate paths to critera. Skip copying
			// roots as workaround.
		}
		else {
			// Copy Roots
			for (Root<?> root : from.getRoots()) {
				Root<?> dest = to.from(root.getJavaType());
				dest.alias(getOrCreateAlias(root));
				copyJoins(root, dest);
				if (copyFetches)
					copyFetches(root, dest);
			}
		}

		to.groupBy(from.getGroupList());
		to.distinct(from.isDistinct());

		if (from.getGroupRestriction() != null)
			to.having(from.getGroupRestriction());

		Predicate predicate = from.getRestriction();
		if (predicate != null)
			to.where(predicate);
	}

	private static boolean isEclipseLink(CriteriaQuery<?> from) {
		return from.getClass().getName().contains("org.eclipse.persistence");
	}

	public static <T> void copyCriteria(CriteriaQuery<T> from, CriteriaQuery<T> to) {
		copyCriteriaNoSelection(from, to);
		to.select(from.getSelection());
	}

	/**
	 * Copy Joins
	 * @param from source Join
	 * @param to destination Join
	 */
	public static void copyJoins(From<?, ?> from, From<?, ?> to) {
		for (Join<?, ?> j : from.getJoins()) {
			Join<?, ?> toJoin = to.join(j.getAttribute().getName(), j.getJoinType());
			toJoin.alias(getOrCreateAlias(j));

			copyJoins(j, toJoin);
		}
	}

	/**
	 * Copy Fetches
	 * @param from source From
	 * @param to destination From
	 */
	public static void copyFetches(From<?, ?> from, From<?, ?> to) {
		for (Fetch<?, ?> f : from.getFetches()) {
			Fetch<?, ?> toFetch = to.fetch(f.getAttribute().getName());
			copyFetches(f, toFetch);
		}
	}

	/**
	 * Copy Fetches
	 * @param from source Fetch
	 * @param to dest Fetch
	 */
	public static void copyFetches(Fetch<?, ?> from, Fetch<?, ?> to) {
		for (Fetch<?, ?> f : from.getFetches()) {
			Fetch<?, ?> toFetch = to.fetch(f.getAttribute().getName());
			// recursively copy fetches
			copyFetches(f, toFetch);
		}
	}

	/**
	 * Get all attributes where type or element type is assignable from class and has persistent type
	 * @param type entity type
	 * @param persistentType persistentType
	 * @param clazz class
	 * @return Set with matching attributes
	 */
	public static Set<Attribute<?, ?>> getAttributes(EntityType<?> type, PersistentAttributeType persistentType,
		Class<?> clazz) {
		Set<Attribute<?, ?>> attributes = new HashSet<Attribute<?, ?>>();

		for (Attribute<?, ?> a : type.getAttributes()) {
			if (a.getPersistentAttributeType() == persistentType && isTypeOrElementType(a, clazz)) {
				attributes.add(a);
			}
		}

		return attributes;
	}

	/**
	 * Get all attributes of type by persistent type
	 * @param type
	 * @param persistentType
	 * @return a set with all attributes of type with persistent type persistentType.
	 */
	public static Set<Attribute<?, ?>> getAttributes(EntityType<?> type, PersistentAttributeType persistentType) {
		return getAttributes(type, persistentType, Object.class);
	}

	/**
	 * Test if attribute is type or in collections has element type
	 * @param attribute attribute to test
	 * @param clazz Class to test
	 * @return true if clazz is asignable from type or element type
	 */
	public static boolean isTypeOrElementType(Attribute<?, ?> attribute, Class<?> clazz) {
		if (attribute.isCollection()) {
			return clazz.isAssignableFrom(((CollectionAttribute<?, ?>) attribute).getBindableJavaType());
		}

		return clazz.isAssignableFrom(attribute.getJavaType());
	}

	/**
	 * Gets the mappedBy value from an attribute
	 * @param attribute attribute
	 * @return mappedBy value or null if none.
	 */
	public static String getMappedBy(Attribute<?, ?> attribute) {
		String mappedBy = null;

		if (attribute.isAssociation()) {
			Annotation[] annotations = null;
			Member member = attribute.getJavaMember();
			if (member instanceof Field) {
				annotations = ((Field) member).getAnnotations();
			}
			else if (member instanceof Method) {
				annotations = ((Method) member).getAnnotations();
			}

			for (Annotation a : annotations) {
				if (a.annotationType().equals(OneToMany.class)) {
					mappedBy = ((OneToMany) a).mappedBy();
					break;
				}
				else if (a.annotationType().equals(ManyToMany.class)) {
					mappedBy = ((ManyToMany) a).mappedBy();
					break;
				}
				else if (a.annotationType().equals(OneToOne.class)) {
					mappedBy = ((OneToOne) a).mappedBy();
					break;
				}
			}
		}

		return "".equals(mappedBy) ? null : mappedBy;
	}
}
