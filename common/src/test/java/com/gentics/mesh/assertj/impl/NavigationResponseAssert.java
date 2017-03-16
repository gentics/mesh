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
	 * Traverse the navigation tree and determine the maximum depth.
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

	/**
	 * Internal validation method that can be called recursively.
	 * 
	 * @param element
	 * @param level
	 * @return
	 */
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

	/**
	 * Assert that the navigation does not exceed the given depth in any of the navigation tree branches.
	 * 
	 * @param depth
	 * @return
	 */
	public NavigationResponseAssert hasDepth(int depth) {
		assertEquals("The depth of the navigation response did not match the expected depth.", depth, maxDepth(actual, 0));
		return this;
	}

	/**
	 * Validate the navigation response and assert that only the given amount of elements are referenced within the nav structure.
	 * 
	 * @param nElements
	 * @return
	 */
	public NavigationResponseAssert isValid(int nElements) {
		assertEquals("Did not find the expected amount of elements.", nElements, validateNavigation(actual, 0));
		return this;
	}

}
