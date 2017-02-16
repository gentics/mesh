package com.gentics.mesh.etc;

/**
 * POJO that is used to deserialize the languages JSON file.
 */
public class LanguageEntry {

	private String name;
	private String nativeName;

	public LanguageEntry() {
	}

	/**
	 * Return the name of the language (eg. German)
	 * 
	 * @return Name of the language
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the language.
	 * 
	 * @param name
	 *            Name of the language
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return the language native name (eg. Deutsch)
	 * 
	 * @return Native language name
	 */
	public String getNativeName() {
		return nativeName;
	}

	/**
	 * Set the language native name.
	 * 
	 * @param nativeName
	 *            Native language name
	 */
	public void setNativeName(String nativeName) {
		this.nativeName = nativeName;
	}
}
