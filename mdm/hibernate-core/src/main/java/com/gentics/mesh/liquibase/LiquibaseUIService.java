package com.gentics.mesh.liquibase;

import liquibase.AbstractExtensibleObject;
import liquibase.ui.InputHandler;
import liquibase.ui.UIService;

/**
 * UIService implementation, that swallows all messages
 */
public class LiquibaseUIService extends AbstractExtensibleObject implements UIService {

	@Override
	public int getPriority() {
		return PRIORITY_DEFAULT;
	}

	@Override
	public void sendMessage(String message) {
	}

	@Override
	public void sendErrorMessage(String message) {
	}

	@Override
	public void sendErrorMessage(String message, Throwable exception) {
	}

	@Override
	public <T> T prompt(String prompt, T valueIfNoEntry, InputHandler<T> inputHandler, Class<T> type) {
		return valueIfNoEntry;
	}

	@Override
	public void setAllowPrompt(boolean allowPrompt) throws IllegalArgumentException {
		if (allowPrompt) {
			throw new IllegalArgumentException("allowPrompt=true not allowed in LiquibaseLoggerService");
		}
	}

	@Override
	public boolean getAllowPrompt() {
		return false;
	}
}
