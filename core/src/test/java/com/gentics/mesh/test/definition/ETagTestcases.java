package com.gentics.mesh.test.definition;

public interface ETagTestcases {

	/**
	 * Test reading one element and check whether the a 304 will be returned if the If-None-Match header was specified.
	 * 
	 * Assert that:
	 * <ul>
	 * <li>etag is part of the response when loading an element</li>
	 * <li>a 304 response is returned when specifying the same etag in the request via a if-none-match header</li>
	 * </ul>
	 * 
	 */
	public void testReadOne();

	public void testReadMultiple();

}
