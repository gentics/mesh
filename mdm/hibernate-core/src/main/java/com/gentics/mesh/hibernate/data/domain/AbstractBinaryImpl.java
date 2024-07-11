package com.gentics.mesh.hibernate.data.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;

import com.gentics.mesh.core.data.AntivirableBinaryElement;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;

@MappedSuperclass
public abstract class AbstractBinaryImpl extends AbstractImageDataImpl implements AntivirableBinaryElement {

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	protected BinaryCheckStatus checkStatus = BinaryCheckStatus.POSTPONED;

	protected String checkSecret;

	@Override
	public BinaryCheckStatus getCheckStatus() {
		return checkStatus;
	}

	@Override
	public AbstractBinaryImpl setCheckStatus(BinaryCheckStatus checkStatus) {
		this.checkStatus = checkStatus;
		return this;
	}

	@Override
	public String getCheckSecret() {
		return checkSecret;
	}

	@Override
	public AbstractBinaryImpl setCheckSecret(String checkSecret) {
		this.checkSecret = checkSecret;
		return this;
	}
}
