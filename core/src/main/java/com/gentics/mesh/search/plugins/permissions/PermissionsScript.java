package com.gentics.mesh.search.plugins.permissions;

import org.elasticsearch.script.AbstractSearchScript;

import java.util.Map;

/**
 * Created by philippguertler on 05.09.16.
 */
public class PermissionsScript extends AbstractSearchScript {

	private final String userUuid;

	public PermissionsScript(Map<String, Object> params) {
		this.userUuid = (String)params.get("userUuid");
	}

	@Override
	public Object run() {
		System.out.println(source().get("odbclusterid") + ":" + source().get("odbposistion"));
		return true;
	}
}