package com.gentics.mesh.util;

import org.junit.Test;
import rx.Observable;
import rx.subjects.BehaviorSubject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;


/**
 * Created by philippguertler on 23.06.16.
 */
public class RxUtilTest {
    @Test
    public void testLargeConcat() {
        int amount = 10000;
        Object testObject = new Object();
        ArrayList<Observable<Object>> list = new ArrayList<>(10000);
        for (int i = 0; i < 10000; i++) {
            list.add(Observable.just(testObject));
        }

        int resultCount = RxUtil.concatList(list).count().toBlocking().single();

        assertEquals(amount, resultCount);
    }

    @Test
    public void testConcatWithSlowFirstObservable() {
        AtomicInteger expectedNumber = new AtomicInteger(1);
        AtomicBoolean completed = new AtomicBoolean();
        BehaviorSubject<Integer> sub1 = BehaviorSubject.create();
        BehaviorSubject<Integer> sub2 = BehaviorSubject.create();
        BehaviorSubject<Integer> sub3 = BehaviorSubject.create();

        List<Observable<Integer>> list = Arrays.asList(sub1, sub2, sub3);

        RxUtil.concatList(list).subscribe(number -> {
            assertEquals(expectedNumber.getAndIncrement(), (int)number);
        }, err -> fail("error occurred"), () -> completed.set(true));

        sub2.onNext(4);
        sub2.onNext(5);

        sub3.onNext(7);
        sub3.onNext(8);

        sub2.onNext(6);
        sub3.onNext(9);

        sub2.onCompleted();
        sub3.onCompleted();

        sub1.onNext(1);
        sub1.onNext(2);
        sub1.onNext(3);

        sub1.onCompleted();

        assertTrue("RxUtil.concatList should be completed", completed.get());
        assertEquals("All elements should be emitted", 10, expectedNumber.get());
    }
}
