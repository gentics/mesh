package com.gentics.mesh.hibernate.util;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.dagger.annotations.ElementTypeKey;
import com.gentics.mesh.hibernate.data.domain.HibPermissionRootImpl;
import com.gentics.mesh.util.ExceptionUtil;

/**
 * Utility class that contains utility method concerning
 * {@link com.gentics.mesh.core.TypeInfo}
 */
public final class TypeInfoUtil {

	private TypeInfoUtil() {
		// hide constructor for utils classes
	}

	public static ElementType getType(Class<?> clazz) {
		try {
			ElementTypeKey annotation = clazz.getAnnotation(ElementTypeKey.class);
			if (annotation == null) {
				return null;
			} else {
				return annotation.value();
			}
		} catch (NullPointerException e) {
			return null;
		}  catch (Exception e) {
			throw ExceptionUtil.rethrow(e);
		}
	}

	public static <T extends BaseElement> ElementType getType(T element) {
		if (element == null) {
			return null;
		} else if (element instanceof HibPermissionRootImpl) {
			return HibPermissionRootImpl.class.cast(element).getType().getElementType();
		} else {
			return getType(element.getClass());
		}
	}
}
