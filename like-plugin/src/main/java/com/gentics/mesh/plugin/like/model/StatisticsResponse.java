package com.gentics.mesh.plugin.like.model;

import java.util.Map;

/**
 * POJO for statistics response
 */
public class StatisticsResponse {
	private int likes;

	private Map<Integer, Integer> ratings;

	private Map<PlusMinusType, Integer> plusMinus;

	public int getLikes() {
		return likes;
	}

	public StatisticsResponse setLikes(int likes) {
		this.likes = likes;
		return this;
	}

	public Map<Integer, Integer> getRatings() {
		return ratings;
	}

	public StatisticsResponse setRatings(Map<Integer, Integer> ratings) {
		this.ratings = ratings;
		return this;
	}

	public Map<PlusMinusType, Integer> getPlusMinus() {
		return plusMinus;
	}

	public StatisticsResponse setPlusMinus(Map<PlusMinusType, Integer> plusMinus) {
		this.plusMinus = plusMinus;
		return this;
	}
}
