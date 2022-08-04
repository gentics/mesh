package com.gentics.mesh.plugin.forms.model;

/**
 * Enum containing element types
 */
public enum ElementType {

	input(InputElement.class), // OK

	checkbox(CheckboxElement.class), // OK

	textarea(TextareaElement.class), // OK

	combielem(CombiElement.class),

	buttons(ButtonsElement.class), // OK

	formpage(FormpageElement.class),

	usermail(UsermailElement.class), // OK

	mandatorygroup(MandatorygroupElement.class),

	selectgroup(SelectgroupElement.class), // OK

	recaptcha(ReCaptchaElement.class), // OK

	friendlycaptcha(FriendlyCaptchaElement.class),

	eyepin(EyepinElement.class),

	file(FileElement.class), // OK

	conditionalcontainer(ConditionalContainerElement.class);

	private Class<? extends FormElement> implementationClass;

	private ElementType(Class<? extends FormElement> implementationClass) {
		this.implementationClass = implementationClass;
	}

	/**
	 * Get the element type or null if not known
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
