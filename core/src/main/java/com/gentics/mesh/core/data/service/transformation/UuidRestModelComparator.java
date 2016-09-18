package com.gentics.mesh.core.data.service.transformation;

import java.util.Comparator;

import com.gentics.mesh.core.rest.common.AbstractResponse;

/**
 * Comparator for rest model objects.
 * 
 * @param <T>
 */
public class UuidRestModelComparator<T extends AbstractResponse> implements Comparator<T> {

	@Override
	public int compare(T o1, T o2) {
		// TODO use order and sorting here?
		String uuid1 = o1.getUuid();
		String uuid2 = o2.getUuid();
		if (uuid1 == null) {
			uuid1 = "";
		}
		if (uuid2 == null) {
			uuid2 = "";
		}
		return uuid1.compareTo(uuid2);
	}

}
