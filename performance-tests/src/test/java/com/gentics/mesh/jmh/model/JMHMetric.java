package com.gentics.mesh.jmh.model;

import com.gentics.mesh.core.rest.common.RestModel;

public class JMHMetric implements RestModel {

	private double score;
	private String scoreError = "NaN";
	private String[] scoreConfidence = { "NaN", "NaN" };
	private JMHPercentiles scoePercentiles = new JMHPercentiles();
	private String scoreUnit = "s/op";
	private double[] rawData;

	public double getScore() {
		return score;
	}

	public JMHMetric setScore(double score) {
		this.score = score;
		return this;
	}

	public String getScoreError() {
		return scoreError;
	}

	public JMHMetric setScoreError(String scoreError) {
		this.scoreError = scoreError;
		return this;
	}

	public String[] getScoreConfidence() {
		return scoreConfidence;
	}

	public JMHMetric setScoreConfidence(String[] scoreConfidence) {
		this.scoreConfidence = scoreConfidence;
		return this;
	}

	public JMHPercentiles getScoePercentiles() {
		return scoePercentiles;
	}

	public JMHMetric setScoePercentiles(JMHPercentiles scoePercentiles) {
		this.scoePercentiles = scoePercentiles;
		return this;
	}

	public String getScoreUnit() {
		return scoreUnit;
	}

	public JMHMetric setScoreUnit(String scoreUnit) {
		this.scoreUnit = scoreUnit;
		return this;
	}

	public double[] getRawData() {
		return rawData;
	}

	public JMHMetric setRawData(double... rawData) {
		this.rawData = rawData;
		return this;
	}
}
