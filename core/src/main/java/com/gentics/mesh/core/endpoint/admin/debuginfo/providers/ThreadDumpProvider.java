package com.gentics.mesh.core.endpoint.admin.debuginfo.providers;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;

import io.reactivex.Flowable;

@Singleton
public class ThreadDumpProvider implements DebugInfoProvider {
	@Inject
	public ThreadDumpProvider() {
	}

	@Override
	public String name() {
		return "threadDump";
	}

	@Override
	public Flowable<DebugInfoEntry> debugInfoEntries() {
		return Flowable.just(DebugInfoEntry.fromString("threaddump.txt", createThreadDump()));
	}

	private String createThreadDump() {
		ThreadMXBean bean = ManagementFactory.getThreadMXBean();
		ThreadInfo[] info = bean.getThreadInfo(bean.getAllThreadIds(), Integer.MAX_VALUE);
		return Stream.of(info)
			.map(this::printThreadInfo)
			.collect(Collectors.joining());
	}

	/**
	 * Same as {@link ThreadInfo#toString()}, except that the amount of stack frames are not limited.
	 * @param threadInfo
	 * @return
	 */
	private String printThreadInfo(ThreadInfo threadInfo) {
		StringBuilder sb = new StringBuilder("\"" + threadInfo.getThreadName() + "\"" +
			" Id=" + threadInfo.getThreadId() + " " +
			threadInfo.getThreadState());
		if (threadInfo.getLockName() != null) {
			sb.append(" on " + threadInfo.getLockName());
		}
		if (threadInfo.getLockOwnerName() != null) {
			sb.append(" owned by \"" + threadInfo.getLockOwnerName() +
				"\" Id=" + threadInfo.getLockOwnerId());
		}
		if (threadInfo.isSuspended()) {
			sb.append(" (suspended)");
		}
		if (threadInfo.isInNative()) {
			sb.append(" (in native)");
		}
		sb.append('\n');
		StackTraceElement[] stackTrace = threadInfo.getStackTrace();
		int i = 0;
		for (; i < stackTrace.length; i++) {
			StackTraceElement ste = stackTrace[i];
			sb.append("\tat " + ste.toString());
			sb.append('\n');
			if (i == 0 && threadInfo.getLockInfo() != null) {
				Thread.State ts = threadInfo.getThreadState();
				switch (ts) {
					case BLOCKED:
						sb.append("\t-  blocked on " + threadInfo.getLockInfo());
						sb.append('\n');
						break;
					case WAITING:
						sb.append("\t-  waiting on " + threadInfo.getLockInfo());
						sb.append('\n');
						break;
					case TIMED_WAITING:
						sb.append("\t-  waiting on " + threadInfo.getLockInfo());
						sb.append('\n');
						break;
					default:
				}
			}

			for (MonitorInfo mi : threadInfo.getLockedMonitors()) {
				if (mi.getLockedStackDepth() == i) {
					sb.append("\t-  locked " + mi);
					sb.append('\n');
				}
			}
		}

		LockInfo[] locks = threadInfo.getLockedSynchronizers();
		if (locks.length > 0) {
			sb.append("\n\tNumber of locked synchronizers = " + locks.length);
			sb.append('\n');
			for (LockInfo li : locks) {
				sb.append("\t- " + li);
				sb.append('\n');
			}
		}
		sb.append('\n');
		return sb.toString();
	}
}
