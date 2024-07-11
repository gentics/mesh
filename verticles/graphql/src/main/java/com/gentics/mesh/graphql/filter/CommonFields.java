package com.gentics.mesh.graphql.filter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.gentics.graphqlfilter.filter.DateFilter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.graphqlfilter.filter.operation.JoinPart;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.core.data.Element;
import com.gentics.mesh.core.data.NamedElement;
import com.gentics.mesh.core.data.user.UserTracking;

import graphql.util.Pair;

/**
 * Common fields that can be composed in various types
 */
public final class CommonFields {
	private CommonFields() {
	}

	/**
	 * Filters by uuid
	 * @deprecated Use {@link #hibUuidFilter()} instead.
	 */
	@Deprecated
	public static <T extends Element> FilterField<T, Map<String, ?>> uuidFilter(String owner) {
		return new MappedFilter<>(owner, "uuid", "Filters by uuid", StringFilter.filter(), Element::getUuid);
	}

	/**
	 * Filters by uuid
	 * 
	 * @param <T>
	 * @return
	 */
	public static <T extends BaseElement> FilterField<T, Map<String, ?>> hibUuidFilter(String owner) {
		return new MappedFilter<>(owner, "uuid", "Filters by uuid", StringFilter.filter(), BaseElement::getUuid);
	}

	/**
	 * Filters by name
	 */
	@Deprecated
	public static <T extends NamedElement> FilterField<T, Map<String, ?>> nameFilter(String owner) {
		return new MappedFilter<>(owner, "name", "Filters by name", StringFilter.filter(), NamedElement::getName);
	}

	/**
	 * Filters by name
	 */
	public static <T extends NamedElement> FilterField<T, Map<String, ?>> hibNameFilter(String owner) {
		return new MappedFilter<>(owner, "name", "Filters by name", StringFilter.filter(), NamedElement::getName);
	}

	/**
	 * Filters by created time, creator, edited time and editor.
	 * @deprecated Use {@link #hibUserTrackingFilter()}
	 */
	@Deprecated
	public static <T extends UserTracking> List<FilterField<T, Map<String, ?>>> userTrackingFilter(String owner) {
		return userTrackingFilter(owner, UserFilter.filter());
	}

	/**
	 * Filters by created time, creator, edited time and editor.
	 */
	public static <T extends UserTracking> List<FilterField<T, Map<String, ?>>> hibUserTrackingFilter(String owner) {
		return hibUserTrackingFilter(owner, UserFilter.filter());
	}

	/**
	 * Filters by created time, creator, edited time and editor.
	 * 
	 * @param userFilter
	 *            The user filter to use for creator and editor
	 */
	@Deprecated
	public static <T extends UserTracking> List<FilterField<T, Map<String, ?>>> userTrackingFilter(String owner, UserFilter userFilter) {
		return hibUserTrackingFilter(owner, userFilter);
	}

	/**
	 * Filters by created time, creator, edited time and editor.
	 * 
	 * @param userFilter
	 *            The user filter to use for creator and editor
	 */
	public static <T extends UserTracking> List<FilterField<T, Map<String, ?>>> hibUserTrackingFilter(String owner, UserFilter userFilter) {
		return Arrays.asList(
			new MappedFilter<>(owner, "created", "Filters by creation timestamp", DateFilter.filter(), UserTracking::getCreationTimestamp),
			new MappedFilter<>(owner, "edited", "Filters by update timestamp", DateFilter.filter(), UserTracking::getLastEditedTimestamp),
			new MappedFilter<>(owner, "creator", "Filters by creator", userFilter, UserTracking::getCreator, Pair.pair("creator", new JoinPart(ElementType.USER.name(), "uuid"))),
			new MappedFilter<>(owner, "editor", "Filters by editor", userFilter, UserTracking::getEditor, Pair.pair("editor", new JoinPart(ElementType.USER.name(), "uuid"))));
	}
}
