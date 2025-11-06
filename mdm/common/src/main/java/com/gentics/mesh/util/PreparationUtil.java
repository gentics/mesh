package com.gentics.mesh.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.dao.PersistingUserDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.parameter.value.FieldsSet;

/**
 * Utility for data preparation
 */
public final class PreparationUtil {
	private PreparationUtil() {
	}

	/**
	 * Prepare the permissions for the given elements, if not done before.
	 * When the permissions are prepared, Boolean.TRUE is stored in the action context
	 * @param page page of elements
	 * @param ac action context
	 */
	public static void preparePermissions(Page<? extends HibCoreElement<? extends RestModel>> page,
			InternalActionContext ac) {
		// when the page is empty, there is nothing to do
		if (page.getSize() == 0) {
			return;
		}

		// get the type of the elements in the page (check the first element only)
		String typeName = page.getWrappedList().get(0).getTypeInfo().getType().name();
		String attributeName = "%s.permissions.prepared".formatted(typeName);

		Boolean prepared = ac.get(attributeName);
		if (BooleanUtils.isNotTrue(prepared)) {
			PersistingUserDao userDao = CommonTx.get().userDao();
			List<Object> permIds = new ArrayList<>();
			permIds.addAll(page.getWrappedList().stream().map(HibCoreElement::getId).collect(Collectors.toList()));
			userDao.preparePermissionsForElementIds(ac.getUser(), permIds);
			ac.put(attributeName, true);
		}
	}

	/**
	 * Prepare the permissions for the given elements, if the fields contain "perms" and preparation was not done before.
	 * @param page page of elements
	 * @param ac action context
	 * @param fields fields
	 */
	public static void preparePermissions(Page<? extends HibCoreElement<? extends RestModel>> page,
			InternalActionContext ac, FieldsSet fields) {
		if (fields.has("perms")) {
			preparePermissions(page, ac);
		}
	}

	/**
	 * Prepare some data for the given elements
	 * @param <T> type of the elements
	 * @param page page holding the elements for which data is to be prepared
	 * @param ac action context
	 * @param elementType type of the elements
	 * @param dataType type of the data to be prepared
	 * @param func function that prepares the data
	 */
	public static <T extends HibCoreElement<? extends RestModel>> void prepareData(
			Page<T> page, InternalActionContext ac, String elementType, String dataType,
			Function<Collection<T>, ?> func) {
		String attributeName = getAttributeName(elementType, dataType);

		if (ac.get(attributeName) == null) {
			@SuppressWarnings("unchecked")
			Object data = func.apply((List<T>)page.getWrappedList());
			ac.put(attributeName, data);
		}
	}

	/**
	 * Prepare some data for the given elements if the assumption is true
	 * @param <T> type of the elements
	 * @param page page holding the elements for which data is to be prepared
	 * @param ac action context
	 * @param elementType type of the elements
	 * @param dataType type of the data to be prepared
	 * @param func function that prepares the data
	 * @param assumption if true, the data is prepared
	 */
	public static <T extends HibCoreElement<? extends RestModel>> void prepareData(
			Page<T> page, InternalActionContext ac, String elementType, String dataType,
			Function<Collection<T>, ?> func, boolean assumption) {
		if (assumption) {
			prepareData(page, ac, elementType, dataType, func);
		}
	}

	public static <T, U> Iterable<? extends U> getPreparedData(T element, InternalActionContext ac, String elementType, String dataType, Function<T, Iterable<? extends U>> alternative) {
		String attributeName = getAttributeName(elementType, dataType);

		Map<T, Collection<? extends U>> data = ac.get(attributeName);
		if (data != null) {
			return new TraversalResult<>(data.getOrDefault(element, Collections.emptyList()));
		} else {
			return alternative.apply(element);
		}
	}

	private static String getAttributeName(String elementType, String dataType) {
		return "%s.%s.prepared".formatted(elementType, dataType);
	}
}
