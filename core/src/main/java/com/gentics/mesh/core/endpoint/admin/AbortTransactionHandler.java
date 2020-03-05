package com.gentics.mesh.core.endpoint.admin;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.madl.tx.AbstractTx;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.common.RestModel;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class AbortTransactionHandler {
	private static final Logger log = LoggerFactory.getLogger(AbortTransactionHandler.class);

	@Inject
	public AbortTransactionHandler() {
	}

	public void abortTransactions(InternalActionContext context) {
		List<Thread> toInterrupt = Thread.getAllStackTraces().entrySet()
			.stream()
			.filter(entry -> isCommitting(entry.getValue()))
			.map(Map.Entry::getKey)
			.collect(Collectors.toList());

		log.info("Interrupting {} threads", toInterrupt.size());
		for (Thread thread : toInterrupt) {
			log.info("Interrupting {}", thread.getName());
			thread.interrupt();
		}

		context.send(new AbortTransactionResponse(toInterrupt.size()), HttpResponseStatus.OK);
	}

	private boolean isCommitting(StackTraceElement[] stackTrace) {
		return Stream.of(stackTrace)
			.anyMatch(element ->
				element.getClassName().equals(AbstractTx.class.getName()) &&
				element.getMethodName().equals("commit")
			);
	}

	public static class AbortTransactionResponse implements RestModel {
		private final int interrupted;

		public AbortTransactionResponse(int interrupted) {
			this.interrupted = interrupted;
		}

		public int getInterrupted() {
			return interrupted;
		}
	}
}
