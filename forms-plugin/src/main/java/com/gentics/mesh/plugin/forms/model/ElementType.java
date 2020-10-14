package com.gentics.mesh.plugin.forms.model;

/**
 * Enum containing element types
 */
public enum ElementType {

	input(InputElement.class),

	dropdown(DropdownElement.class),

	radio(RadioElement.class),

	checkbox(CheckboxElement.class),

	textarea(TextareaElement.class),

	zipcode(ZipcodeElement.class),

	svnr(SvnrElement.class),

	range(RangeElement.class),

	combielem(CombiElement.class),

	buttons(ButtonsElement.class),

	formpage(FormpageElement.class),

	usermail(UsermailElement.class),

	mandatorygroup(MandatorygroupElement.class),

	selectgroup(SelectgroupElement.class),

	recaptcha(ReCaptchaElement.class),

	date(DateElement.class),

	time(DateElement.class),

	zaehlerpunkt(ZaehlerpunktElement.class),

	file(FileElement.class),

	conditionalcontainer(ConditionalContainerElement.class);

	private Class<? extends FormElement> implementationClass;

	private ElementType(Class<? extends FormElement> implementationClass) {
		this.implementationClass = implementationClass;
	}

	/**
	 * Get the element type or null if not known
	 * 
	 * @param type
	 *            type string
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
