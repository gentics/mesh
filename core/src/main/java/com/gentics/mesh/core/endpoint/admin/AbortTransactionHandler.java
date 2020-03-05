package com.gentics.mesh.core.endpoint.admin;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.madl.tx.AbstractTx;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.common.RestModel;
import com.google.common.collect.ImmutableSet;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class AbortTransactionHandler {
	private static final Logger log = LoggerFactory.getLogger(AbortTransactionHandler.class);
	private static final Set<ClassMethod> interruptedMethods = ImmutableSet.of(
		new ClassMethod(AbstractTx.class, "commit")
	);

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
			.map(ClassMethod::of)
			.anyMatch(interruptedMethods::contains);
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

	public static class ClassMethod {
		private final String className;
		private final String methodName;

		public ClassMethod(Class<?> clazz, String methodName) {
			this(clazz.getName(), methodName);
		}

		public ClassMethod(String className, String methodName) {
			this.className = className;
			this.methodName = methodName;
		}

		public static ClassMethod of(StackTraceElement element) {
			return new ClassMethod(element.getClassName(), element.getMethodName());
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ClassMethod that = (ClassMethod) o;
			return Objects.equals(className, that.className) &&
				Objects.equals(methodName, that.methodName);
		}

		@Override
		public int hashCode() {
			return Objects.hash(className, methodName);
		}
	}
}
