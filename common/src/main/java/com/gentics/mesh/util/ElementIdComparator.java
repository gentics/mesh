package com.gentics.mesh.util;

import java.util.Comparator;

import org.apache.commons.lang3.ObjectUtils;

import com.gentics.mesh.core.data.HibElement;

/**
 * Comparator for blueprint elements which uses the id of both elements for comparison.
 */
public class ElementIdComparator implements Comparator<HibElement> {

	@Override
	public int compare(HibElement o1, HibElement o2) {
		String idA = o1.getId().toString();
		String idB = o2.getId().toString();
		return ObjectUtils.compare(idA, idB);
	}

}
