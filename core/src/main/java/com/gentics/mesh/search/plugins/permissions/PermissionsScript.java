package com.gentics.mesh.search.plugins.permissions;

import org.elasticsearch.script.AbstractSearchScript;

/**
 * Created by philippguertler on 05.09.16.
 */
public class PermissionsScript extends AbstractSearchScript {
	@Override
	public Object run() {
		System.out.println(String.format("UUID: %s", source().get("uuid")));
		return true;
	}
}