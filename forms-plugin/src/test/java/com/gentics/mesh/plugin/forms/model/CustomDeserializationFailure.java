package com.gentics.mesh.plugin.forms.model;

/**
 * Custom element the will throw a {@link RuntimeException} in the constructor
 */
@Reportable
public class CustomDeserializationFailure extends FormElement {
	/**
	 * Let the creation fail
	 */
	public CustomDeserializationFailure() {
		throw new RuntimeException("Deserialization of element CustomDeserializationFailure must fail");
	}

	/**
	 * Copy constructor
	 * @param other
	 * @param deep
	 */
	protected CustomDeserializationFailure(CustomDeserializationFailure other, boolean deep) {
		super(other, deep);
	}

	@Override
	protected FormElement clone(boolean deep) {
		return new CustomDeserializationFailure(this, true);
	}
}
