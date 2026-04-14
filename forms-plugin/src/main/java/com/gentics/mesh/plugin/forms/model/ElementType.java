package com.gentics.mesh.plugin.forms.model;

/**
 * Enum containing element types
 */
public enum ElementType {
	string(InputElement.class),

	number(InputElement.class),

	checkbox(CheckboxElement.class), // TODO 8.5 Unname it to boolean ? 

	select(CombiElement.class),

	formpage(FormpageElement.class), // TODO 8.5 delete

	adminmail(AdminMailElement.class),

	usermail(UsermailElement.class), // OK

	mandatorygroup(MandatorygroupElement.class),

	selectgroup(SelectgroupElement.class), // OK

	recaptcha(ReCaptchaElement.class), // OK

	friendlycaptcha(FriendlyCaptchaElement.class),

	eyepin(EyepinElement.class),

	file(FileElement.class), // OK //TODO 8.5 unname it to binary ? 

	datetime(DatetimeElement.class),
	time(DatetimeElement.class);


	private Class<? extends FormElement> implementationClass;

	private ElementType(Class<? extends FormElement> implementationClass) {
		this.implementationClass = implementationClass;
	}

	/**
	 * Get the element type or null if not known.
	 * Handles both legacy type names (e.g. "input") and new schema type names (e.g. "string").
	 * "boolean" is a Java keyword and cannot be an enum constant — it maps to {@link #checkbox}.
	 *
	 * @param type type string
	 * @return type or null
	 */
	public static ElementType get(String type) {
		try {
			return valueOf(type);
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * Get the implementation class
	 *
	 * @return implementation class
	 */
	public Class<? extends FormElement> getImplementationClass() {
		return implementationClass;
	}
}
