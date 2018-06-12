package com.gentics.mesh.util;

import java.util.Comparator;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.tinkerpop.gremlin.structure.Element;

/**
 * Comparator for blueprint elements which uses the id of both elements for comparison.
 */
public class ElementIdComparator implements Comparator<Element> {

	@Override
	public int compare(Element o1, Element o2) {
		String idA = o1.id().toString();
		String idB = o2.id().toString();
		return ObjectUtils.compare(idA, idB);
	}

}
