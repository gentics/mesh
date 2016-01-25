package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.navigation.NavigationElement;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;

public class NavigationResponseAssert extends AbstractAssert<NavigationResponseAssert, NavigationResponse> {

	public NavigationResponseAssert(NavigationResponse actual) {
		super(actual, NavigationResponseAssert.class);
	}

	/**
	 * Traverse the navigation tree and determine the maxium depth.
	 * 
	 * @param element
	 * @param depth
	 * @return
	 */
	private int maxDepth(NavigationElement element, int depth) {
		int max = depth;
		if (element.getChildren() != null) {
			for (NavigationElement child : element.getChildren()) {
				int foundDepth = maxDepth(child, depth + 1);
				if (foundDepth > max) {
					max = foundDepth;
				}
			}
		}
		return max;
	}

	private int validateNavigation(NavigationElement element, int level) {
		assertNotNull("Level: " + level + " The given root element must not be null", element);
		assertNotNull("Level: " + level + " The node field within one of the navigation elements was null.", element.getNode());
		assertNotNull("Level: " + level + " The uuid of the navigation element must not be null", element.getUuid());
		assertEquals("Level: " + level + " The uuid of the navigation element and the nested node did not match.", element.getUuid(),
				element.getNode().getUuid());
		int elements = 1;
		if (element.getChildren() != null) {
			if (element.getChildren().isEmpty()) {
				fail("Level: " + level + " The children field should never be empty. Instead it should be null to avoid it being serialized to JSON");
			}
			for (NavigationElement child : element.getChildren()) {
				elements += validateNavigation(child, level++);
			}
		}
		return elements;
	}

	public NavigationResponseAssert hasDepth(int depth) {
		assertEquals("The depth of the navigation response did not match the expected depth.", depth, maxDepth(actual.getRoot(), 0));
		return this;
	}

	public NavigationResponseAssert isValid(int nElements) {
		assertEquals("Did not find the expected amount of elements.", nElements, validateNavigation(actual.getRoot(), 0));
		return this;
	}

}
