package com.gentics.mesh.util;

import java.util.Comparator;

import org.apache.commons.lang3.ObjectUtils;

import com.gentics.mesh.core.data.HibElement;

/**
 * Comparator for blueprint elements which uses the UUID property of both elements for comparison.
 */
public class ElementUuidComparator implements Comparator<HibElement> {

	@Override
	public int compare(HibElement o1, HibElement o2) {
		String uuidA = o1.getUuid();
		String uuidB = o2.getUuid();
		return ObjectUtils.compare(uuidA, uuidB);
	}

}
