package com.gentics.mesh.plugin.forms.model;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import io.vertx.core.MultiMap;

/**
 * Zaehlerpunkt element
 */
@Reportable
public class ZaehlerpunktElement extends FormElement {
	private String zaehlerpunkt;

	/**
	 * Get the element data as single value
	 *
	 * @param postedData posted data
	 * @return single value (may be null)
	 */
	public String getSingleFormData(MultiMap postedData) {
		String value = postedData.get(getName());
		if (isNotEmpty(value) && !value.startsWith(zaehlerpunkt)) {
			value = zaehlerpunkt + value;
		}
		return value;
	}

	public String getZaehlerpunkt() {
		return zaehlerpunkt;
	}

	public void setZaehlerpunkt(String zaehlerpunkt) {
		this.zaehlerpunkt = zaehlerpunkt + ".";
	}
}
