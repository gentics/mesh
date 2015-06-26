package com.gentics.mesh.core.rest.node.response.field;

import com.gentics.mesh.model.FieldTypes;

public class NumberField extends AbstractField {

	private String number;

	private Integer min;

	private Integer max;

	private Float step;

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public Integer getMax() {
		return max;
	}

	public void setMax(Integer max) {
		this.max = max;
	}

	public Integer getMin() {
		return min;
	}

	public void setMin(Integer min) {
		this.min = min;
	}

	public Float getStep() {
		return step;
	}

	public void setStep(Float step) {
		this.step = step;
	}

	@Override
	public String getType() {
		return FieldTypes.NUMBER.toString();
	}

}
