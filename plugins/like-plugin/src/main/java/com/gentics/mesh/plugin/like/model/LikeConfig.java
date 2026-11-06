package com.gentics.mesh.plugin.like.model;

/**
 * POJO for the like configuration of a node
 */
public class LikeConfig {
	private boolean active = false;

	private Type type;

	private int maxRating = 0;

	private String baseUrl = "/";

	/**
	 * True when likes are active for the node
	 * @return true for likes
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Set likes active
	 * @param active active
	 * @return fluent API
	 */
	public LikeConfig setActive(boolean active) {
		this.active = active;
		return this;
	}

	/**
	 * Type of the like
	 * @return like type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Set the type
	 * @param type type
	 * @return fluent API
	 */
	public LikeConfig setType(Type type) {
		this.type = type;
		return this;
	}

	/**
	 * Maximum rating (if type is {@link Type})
	 * @return maximum rating
	 */
	public int getMaxRating() {
		return maxRating;
	}

	/**
	 * Set the maximum rating
	 * @param maxRating maximum rating
	 * @return fluent API
	 */
	public LikeConfig setMaxRating(int maxRating) {
		this.maxRating = maxRating;
		return this;
	}

	/**
	 * Get the base URL
	 * @return base URL
	 */
	public String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * Set the base URL
	 * @param baseUrl base URL
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}
}
