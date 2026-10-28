package com.gentics.mesh.plugin.like.model;

/**
 * POJO for a posted like
 */
public class PostedLike {
	private Integer rating;

	private PlusMinusType plusMinus;

	private Boolean like;

	public Integer getRating() {
		return rating;
	}

	public PostedLike setRating(Integer rating) {
		this.rating = rating;
		return this;
	}

	public PlusMinusType getPlusMinus() {
		return plusMinus;
	}

	public PostedLike setPlusMinus(PlusMinusType plusMinus) {
		this.plusMinus = plusMinus;
		return this;
	}

	public Boolean getLike() {
		return like;
	}

	public PostedLike setLike(Boolean like) {
		this.like = like;
		return this;
	}

}
