package com.gentics.mesh.test;

import java.text.DecimalFormat;

import rx.functions.Action1;

public final class StopWatch {

	public static void stopWatch(String name, int steps, Action1<Integer> action) {
		System.out.println("-------------------------------");
		DecimalFormat df = new DecimalFormat("#.####");
		org.apache.commons.lang3.time.StopWatch watch = new org.apache.commons.lang3.time.StopWatch();
		int tenth = steps / 10;
		watch.start();
		for (int step = 0; step < steps; step++) {
			action.call(step);
			if (step % tenth == 0 && step != 0) {
				double perStep = (double) watch.getTime() / (double) step;
				System.out.println("[" + name + "] Step [" + step + "/" + steps + "] avg step: " + df.format(perStep) + " [ms]");
			}
		}
		watch.stop();
		double perStep = (double) watch.getTime() / (double) steps;
		System.out.println("[" + name + "] Avg step: " + df.format(perStep) + " [ms]");
		System.out.println("[" + name + "] Took: " + watch.getTime() + " [ms]");
	}
}
