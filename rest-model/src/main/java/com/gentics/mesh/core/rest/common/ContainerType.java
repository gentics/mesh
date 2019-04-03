package com.gentics.mesh.core.rest.common;

/**
 * Enum of container edge types
 */
public enum ContainerType {

	/**
	 * Edge to Initial Version
	 */
	INITIAL("I", "initial"),

	/**
	 * Edge to Draft Version
	 */
	DRAFT("D", "draft"),

	/**
	 * Edge to Published Version
	 */
	PUBLISHED("P", "published");

	/**
	 * Edge code
	 */
	protected String code;

	/**
	 * Human readable code
	 */
	protected String humanCode;

	/**
	 * Create an instance
	 * 
	 * @param code
	 *            Edge code
	 * @param humanCode
	 *            Human readable code
	 */
	private ContainerType(String code, String humanCode) {
		this.code = code;
		this.humanCode = humanCode;
	}

	/**
	 * Get the code
	 * 
	 * @return code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Get the type from the code
	 * 
	 * @param code
	 *            code
	 * @return type
	 */
	public static ContainerType get(String code) {
		if (code == null) {
			return null;
		}
		for (ContainerType type : values()) {
			if (type.code.equals(code)) {
				return type;
			}
		}

		throw new IllegalArgumentException("Unknown edge type " + code);
	}

	/**
	 * Get the type for the given version string. Returns DRAFT for "draft", PUBLISHED for "published" and INITIAL for everything else
	 * 
	 * @param version
	 *            version
	 * @return edge type
	 */
	public static ContainerType forVersion(String version) {
		if ("draft".equals(version)) {
			return DRAFT;
		} else if ("published".equals(version)) {
			return PUBLISHED;
		} else {
			return INITIAL;
		}
	}

	/**
	 * Returns the lowercase name of the type.
	 * 
	 * @return
	 */
	public String getShortName() {
		return name().toLowerCase();
	}

	/**
	 * Return the human readble code
	 * 
	 * @return
	 */
	public String getHumanCode() {
		return humanCode;
	}

}
