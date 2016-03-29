package com.gentics.mesh.core.data;

/**
 * Interface for edges between i18n field containers and the node.
 * Edges are language specific, are bound to releases and are either of type "Initial, Draft or Published"
 */
public interface GraphFieldContainerEdge {
	/**
	 * Get the language tag
	 * @return language tag
	 */
	String getLanguageTag();

	/**
	 * Set the language tag.
	 * 
	 * @param languageTag
	 */
	void setLanguageTag(String languageTag);

	/**
	 * Get the edge type
	 * @return edge type
	 */
	Type getType();

	/**
	 * Set the edge type
	 * @param type edge type
	 */
	void setType(Type type);

	/**
	 * Get the release Uuid
	 * @return release Uuid
	 */
	String getReleaseUuid();

	/**
	 * Set the release Uuid
	 * @param uuid release Uuid
	 */
	void setReleaseUuid(String uuid);

	/**
	 * Enum of edge types
	 */
	public static enum Type {
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
		 * @param code code
		 */
		private Type(String code) {
			this.code = code;
		}

		/**
		 * Get the code
		 * @return code
		 */
		public String getCode() {
			return code;
		}

		/**
		 * Get the type from the code
		 * @param code code
		 * @return type
		 */
		public static Type get(String code) {
			if (code == null) {
				return null;
			}
			for (Type type : values()) {
				if (type.code.equals(code)) {
					return type;
				}
			}

			throw new IllegalArgumentException("Unknown edge type " + code);
		}

		/**
		 * Get the type for the given version string.
		 * Returns DRAFT for "draft", PUBLISHED for "published" and INITIAL for everything else
		 * @param version version
		 * @return edge type
		 */
		public static Type forVersion(String version) {
			if ("draft".equals(version)) {
				return Type.DRAFT;
			} else if ("published".equals(version)) {
				return Type.PUBLISHED;
			} else {
				return Type.INITIAL;
			}
		}
	}
}
