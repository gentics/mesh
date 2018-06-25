package com.gentics.mesh.jmh.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JMHPercentiles {

	@JsonProperty("0.0")
	private double n0 = 0;

	@JsonProperty("50.0")
	private double n50 = 0;

	@JsonProperty("90.0")
	private double n90 = 0;

	@JsonProperty("95.0")
	private double n95 = 0;

	@JsonProperty("99.0")
	private double n99_0 = 0;

	@JsonProperty("99.9")
	private double n99_9 = 0;

	@JsonProperty("99.99")
	private double n99_99 = 0;

	@JsonProperty("99.999")
	private double n99_999 = 0;

	@JsonProperty("99.9999")
	private double n99_9999 = 0;

	@JsonProperty("100.0")
	private double n100 = 0;

	public double getN0() {
		return n0;
	}

	public JMHPercentiles setN0(double n0) {
		this.n0 = n0;
		return this;
	}

	public double getN50() {
		return n50;
	}

	public JMHPercentiles setN50(double n50) {
		this.n50 = n50;
		return this;
	}

	public double getN90() {
		return n90;
	}

	public JMHPercentiles setN90(double n90) {
		this.n90 = n90;
		return this;
	}

	public double getN95() {
		return n95;
	}

	public JMHPercentiles setN95(double n95) {
		this.n95 = n95;
		return this;
	}

	public double getN99_0() {
		return n99_0;
	}

	public JMHPercentiles setN99_0(double n99_0) {
		this.n99_0 = n99_0;
		return this;
	}

	public double getN99_9() {
		return n99_9;
	}

	public JMHPercentiles setN99_9(double n99_9) {
		this.n99_9 = n99_9;
		return this;
	}

	public double getN99_99() {
		return n99_99;
	}

	public JMHPercentiles setN99_99(double n99_99) {
		this.n99_99 = n99_99;
		return this;
	}

	public double getN99_999() {
		return n99_999;
	}

	public JMHPercentiles setN99_999(double n99_999) {
		this.n99_999 = n99_999;
		return this;
	}

	public double getN99_9999() {
		return n99_9999;
	}

	public JMHPercentiles setN99_9999(double n99_9999) {
		this.n99_9999 = n99_9999;
		return this;
	}

	public double getN100() {
		return n100;
	}

	public JMHPercentiles setN100(double n100) {
		this.n100 = n100;
		return this;
	}

}
