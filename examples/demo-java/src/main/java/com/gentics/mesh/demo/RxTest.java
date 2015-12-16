package com.gentics.mesh.demo;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.rest.schema.Microschema;

import rx.Observable;

public class RxTest {

	public static void main(String[] args) {
//		Observable<String> bla = Observable.just("Start", "Item", "Error1").map(s -> {
//			if ("Error".equals(s)) {
//				throw new Error();
//			} else {
//				return StringUtils.reverse(s);
//			}
//		});
//
//		bla.subscribe(item -> {
//			System.out.println(item);
//		}, e -> {
//			System.err.println("Oops");
//		}, () -> {
//			System.out.println("Complete");
//		});

		doIt().doOnError(e -> {
			e.printStackTrace();
		}).doOnCompleted(() -> {
			System.out.println("Done");
		}).subscribe();
	}

	private static Observable<Boolean> doIt() {
		return Observable.create(t -> {
			Observable.just("One", "Two", "Three").flatMap(string -> Observable.just(StringUtils.reverse(string)),
					(string, reverse) -> string + reverse).subscribe(a -> {
				System.out.println(a);
				throw new Error();
			}, e -> {
				t.onError(e);
			}, () -> {
				t.onNext(true);
				t.onCompleted();
			});
		});
	}

	private static void bla() {
		List<Micronode> nodes = null;
		Observable.from(nodes).flatMap(node -> {
			Observable<Microschema> microschema = Observable.just(null);
			return microschema;
		}, (node, ms) -> {
			return Observable.just(null);
		}).subscribe(l -> {
			
		});
		
		Observable.from(nodes).flatMap(node -> {
			Observable<Microschema> microschema = Observable.just(null);
			return microschema.flatMap(ms -> {
				return Observable.just(null);
			});
		});
	}
}
