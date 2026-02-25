package com.gentics.mesh.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;

import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.dao.PersistingUserDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.handler.DataHolderContext;
import com.gentics.mesh.parameter.value.FieldsSet;

/**
 * Utility for data preparation
 */
public final class PreparationUtil {
	private PreparationUtil() {
	}

	/**
	 * Prepare the permissions for the given elements, if not done before.
	 * When the permissions are prepared, Boolean.TRUE is stored in the data holder context
	 * @param user user
	 * @param page page of elements
	 * @param dhc data holder context
	 */
	public static void preparePermissions(HibUser user, Page<? extends HibCoreElement<? extends RestModel>> page,
			DataHolderContext dhc) {
		// when the page is empty, there is nothing to do
		if (page.getSize() == 0 || dhc == null) {
			return;
		}

		// get the type of the elements in the page (check the first element only)
		String typeName = page.getWrappedList().get(0).getTypeInfo().getType().name();
		String attributeName = "%s.permissions.prepared".formatted(typeName);

		Boolean prepared = dhc.get(attributeName);
		if (BooleanUtils.isNotTrue(prepared)) {
			PersistingUserDao userDao = CommonTx.get().userDao();
			List<Object> permIds = new ArrayList<>();
			permIds.addAll(page.getWrappedList().stream().map(HibCoreElement::getId).collect(Collectors.toList()));
			userDao.preparePermissionsForElementIds(user, permIds);
			dhc.put(attributeName, true);
		}
	}

	/**
	 * Prepare the permissions for the given elements, if the fields contain "perms" and preparation was not done before.
	 * @param page page of elements
	 * @param dhc data holder context
	 * @param fields fields
	 */
	public static void preparePermissions(HibUser user, Page<? extends HibCoreElement<? extends RestModel>> page,
			DataHolderContext dhc, FieldsSet fields) {
		if (fields.has("perms")) {
			preparePermissions(user, page, dhc);
		}
	}

	/**
	 * Prepare some data for the given elements
	 * @param <T> type of the elements
	 * @param elements collection of elements for which data need to be prepared
	 * @param dhc data holder context
	 * @param elementType type of the elements
	 * @param dataType type of the data to be prepared
	 * @param func function that prepares the data
	 */
	public static <T> void prepareData(
			Collection<T> elements, DataHolderContext dhc, String elementType, String dataType,
			Function<Collection<T>, ?> func) {
		if (dhc == null) {
			return;
		}
		String attributeName = getAttributeName(elementType, dataType);

		if (dhc.get(attributeName) == null) {
			Object data = func.apply(elements);
			dhc.put(attributeName, data);
		}
	}

	/**
	 * Prepare some data for the given elements if the assumption is true
	 * @param <T> type of the elements
	 * @param elements collection of elements for which data need to be prepared
	 * @param dhc data holder context
	 * @param elementType type of the elements
	 * @param dataType type of the data to be prepared
	 * @param func function that prepares the data
	 * @param assumption if true, the data is prepared
	 */
	public static <T> void prepareData(
			Collection<T> elements, DataHolderContext dhc, String elementType, String dataType,
			Function<Collection<T>, ?> func, boolean assumption) {
		if (assumption) {
			prepareData(elements, dhc, elementType, dataType, func);
		}
	}

	/**
	 * Get prepared data
	 * @param <T> type of the element
	 * @param <U> type of the prepared data
	 * @param element element
	 * @param dhc data holder context, if null, the data is fetched from the alternative fetcher
	 * @param elementType type of the element
	 * @param dataType type of the prepared data
	 * @param alternative alternative data fetcher
	 * @return either prepared data or data fetched from the alternative
	 */
	public static <T, U> U getPreparedData(T element, DataHolderContext dhc, String elementType, String dataType, Function<T, U> alternative) {
		if (dhc == null) {
			return alternative.apply(element);
		}

		String attributeName = getAttributeName(elementType, dataType);
		try {
			Map<T, U> data = dhc.get(attributeName);
			if (data != null) {
				return Optional.ofNullable(data.get(element)).orElseGet(() -> alternative.apply(element));
			} else {
				return alternative.apply(element);
			}
		} catch(Throwable e) {
			return alternative.apply(element);
		}
	}

	/**
	 * Get the attribute name
	 * @param elementType element type
	 * @param dataType data type
	 * @return attribute name
	 */
	public static String getAttributeName(String elementType, String dataType) {
		return "%s.%s.prepared".formatted(elementType, dataType);
	}
}
