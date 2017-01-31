package com.gentics.mesh.core.data;

/**
 * Enum of container edge types
 */
public enum ContainerType {

	/**
	 * Edge to Initial Version
	 */
	INITIAL("I"),

	/**
	 * Edge to Draft Version
	 */
	DRAFT("D"),

	/**
	 * Edge to Published Version
	 */
	PUBLISHED("P");

	/**
	 * Edge code
	 */
	protected String code;

	/**
	 * Create an instance
	 * 
	 * @param code
	 *            code
	 */
	private ContainerType(String code) {
		this.code = code;
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
			return ContainerType.DRAFT;
		} else if ("published".equals(version)) {
			return ContainerType.PUBLISHED;
		} else {
			return ContainerType.INITIAL;
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

}
