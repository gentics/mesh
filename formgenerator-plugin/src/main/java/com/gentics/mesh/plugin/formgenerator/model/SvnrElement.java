package com.gentics.mesh.plugin.formgenerator.model;

import io.vertx.core.MultiMap;

/**
 * Svnr element
 */
@Reportable
public class SvnrElement extends FormElement {
	@Override
	public String getSingleFormData(MultiMap postedData) {
		return String.format("%s%s", postedData.get(getName()), postedData.get(String.format("%s_bday", getName()))).trim();
	}
}
