package com.gentics.mesh.hibernate.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Simplifies criteria builder query.
 * Inspired by https://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/domain/Specification.html,
 * tailored to be used in the {@link com.gentics.mesh.hibernate.data.dao.RootDaoHelper}
 * @param <CHILD>
 */
@FunctionalInterface
public interface Specification<CHILD, ROOT> {

	static <CHILD, ROOT> Specification<CHILD, ROOT> and(Specification<CHILD, ROOT> left, Specification<CHILD, ROOT> right, Combiner combiner) {
		return (root, query, join, cb) -> {
			Predicate leftPredicate = left.toPredicate(root, query, join, cb);
			Predicate rightPredicate = right.toPredicate(root, query, join, cb);

			if (leftPredicate == null) {
				return rightPredicate;
			}

			return rightPredicate == null ? leftPredicate : combiner.combine(cb, leftPredicate, rightPredicate);
		};
	}

	/**
	 * Combine this specification with another specification. The predicate of the specifications will be merged with
	 * the and operator.
	 * @param other
	 * @return
	 */
	default Specification<CHILD, ROOT> and(Specification<CHILD, ROOT> other) {
		return and(this, other, CriteriaBuilder::and);
	}

	/**
	 * This method should return a {@link Predicate} which will be added in the where statement of the query.
	 *
	 * @param root
	 * @param query
	 * @param join
	 * @param criteriaBuilder
	 * @return a predicate to use in the query.where statement, or null in case the method was used to perform side
	 * effects on the query
	 */
	Predicate toPredicate(Root<CHILD> root,
						  CriteriaQuery<CHILD> query,
						  Join<CHILD, ROOT> join,
						  CriteriaBuilder criteriaBuilder);

	interface Combiner {
		Predicate combine(CriteriaBuilder cb, Predicate left, Predicate right);
	}
}
