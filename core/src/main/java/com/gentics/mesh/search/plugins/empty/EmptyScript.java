package com.gentics.mesh.search.plugins.empty;

import org.elasticsearch.script.AbstractSearchScript;

/**
 * Created by philippguertler on 05.09.16.
 */
public class EmptyScript extends AbstractSearchScript {
	@Override
	public Object run() {
		return true;
	}
}