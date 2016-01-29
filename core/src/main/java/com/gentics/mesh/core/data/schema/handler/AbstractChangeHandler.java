package com.gentics.mesh.core.data.schema.handler;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public abstract class AbstractChangeHandler implements ChangeHandler {

	/**
	 * Return the operation name that is handled by the handler implementation.
	 * 
	 * @return
	 */
	protected abstract String getOperation();

	@Autowired
	ChangeHandlerRegistry registry;

	@PostConstruct
	public void register() {
		registry.register(getOperation(), this);
	}
}
