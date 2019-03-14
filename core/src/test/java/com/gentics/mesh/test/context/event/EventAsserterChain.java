package com.gentics.mesh.test.context.event;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshEventModel;

import io.reactivex.functions.Consumer;

public class EventAsserterChain {

	private EventAsserter asserter;
	private MeshEvent event;

	public EventAsserterChain(EventAsserter asserter, MeshEvent event) {
		this.asserter = asserter;
		this.event = event;
		asserter.registerForEvent(event);
	}

	/**
	 * Add an expectation for an event body.
	 * 
	 * @param expectedCount
	 *            How many events should be passed by the asserter
	 * @param clazzOfEM
	 * @param asserter
	 * @return
	 */
	public <EM extends MeshEventModel> EventAsserterChain match(int expectedCount, Class<EM> clazzOfEM, Consumer<EM> asserter) {
		this.asserter.addExpectation(new EventBodyExpectation(event, expectedCount, clazzOfEM, asserter));
		return this;
	}

	/**
	 * Expect the given number of events.
	 * 
	 * @param count
	 * @return
	 */
	public EventAsserterChain total(int count) {
		this.asserter.addExpectation(new EventCountExpectation(event, count));
		return this;
	}

	/**
	 * Expect one event.
	 * 
	 * @return
	 */
	public EventAsserterChain one() {
		return total(1);
	}

	/**
	 * Expect no events.
	 * 
	 * @return
	 */
	public EventAsserterChain none() {
		return total(0);
	}

}
