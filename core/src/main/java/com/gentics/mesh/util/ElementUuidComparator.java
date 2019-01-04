package com.gentics.mesh.util;

import java.util.Comparator;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.tinkerpop.gremlin.structure.Element;

/**
 * Comparator for blueprint elements which uses the UUID property of both elements for comparison.
 */
public class ElementUuidComparator implements Comparator<Element> {

	@Override
	public int compare(Element o1, Element o2) {
		String uuidA = o1.value("uuid");
		String uuidB = o2.value("uuid");
		return ObjectUtils.compare(uuidA, uuidB);
	}

}
