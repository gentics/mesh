package com.gentics.mesh.core.data.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * A developer extension to {@link DaoGlobal} with low level persistent storage access.
 * 
 * @author plyhun
 *
 * @param <T>
 */
public interface PersistingDaoGlobal<T extends HibBaseElement> extends DaoGlobal<T>, PersistingDao<T>, ElementResolver<HibBaseElement, T> {

	@Override
	default BiFunction<HibBaseElement, String, T> getFinder() {
		return (unused, uuid) -> findByUuid(uuid);
	}

	default void preparePermissions(Page<? extends HibCoreElement<? extends RestModel>> page,
			InternalActionContext ac, String attributeName) {
		Boolean prepared = ac.get(attributeName);
		if (BooleanUtils.isNotTrue(prepared)) {
			PersistingUserDao userDao = CommonTx.get().userDao();
			List<Object> permIds = new ArrayList<>();
			permIds.addAll(page.getWrappedList().stream().map(HibCoreElement::getId).collect(Collectors.toList()));
			userDao.preparePermissionsForElementIds(ac.getUser(), permIds);
			ac.put(attributeName, true);
		}
	}
}
