package com.gentics.mesh.plugin.like.model;

/**
 * POJO for the like response
 */
public class LikeResponse {
	private LikeConfig configuration;

	private PostedLike posted;

	private StatisticsResponse statistics;

	public LikeConfig getConfiguration() {
		return configuration;
	}

	public LikeResponse setConfiguration(LikeConfig configuration) {
		this.configuration = configuration;
		return this;
	}

	public PostedLike getPosted() {
		return posted;
	}

	public LikeResponse setPosted(PostedLike posted) {
		this.posted = posted;
		return this;
	}

	public StatisticsResponse getStatistics() {
		return statistics;
	}

	public LikeResponse setStatistics(StatisticsResponse statistics) {
		this.statistics = statistics;
		return this;
	}
}
