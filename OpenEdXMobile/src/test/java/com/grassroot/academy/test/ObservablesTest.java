package com.grassroot.academy.test;


import androidx.annotation.NonNull;

import com.grassroot.academy.base.BaseTestCase;
import com.grassroot.academy.util.observer.BasicObservable;
import com.grassroot.academy.util.observer.Func1;
import com.grassroot.academy.util.observer.Observable;
import com.grassroot.academy.util.observer.Observables;
import com.grassroot.academy.util.observer.Observer;
import org.junit.Test;
import static org.junit.Assert.*;

public class ObservablesTest extends BaseTestCase {

    @Test
    public void testMapSendsData() {
        BasicObservable<String> observable = new BasicObservable<>();
        Observable<String> upper = Observables.map(observable, new Func1<String, String>() {
            @Override
            public String call(String arg) {
                // uppercase doesn't matter - just do something
                return arg.toUpperCase();
            }
        });

        final String[] observed = {null};
        upper.subscribe(new Observer<String>() {
            @Override
            public void onData(@NonNull String data) {
                observed[0] = data;
            }

            @Override
            public void onError(@NonNull Throwable error) {
                fail();
            }
        });
        observable.sendData("test");
        assertEquals(observed[0], "test".toUpperCase());
    }

    public void testMapSendsError() {
        BasicObservable<String> observable = new BasicObservable<>();
        Observable<String> upper = Observables.map(observable, new Func1<String, String>() {
            @Override
            public String call(String arg) {
                // uppercase doesn't matter - just do something
                return arg.toUpperCase();
            }
        });

        Throwable error = new Exception("test");
        final Throwable[] observed = {null};
        upper.subscribe(new Observer<String>() {
            @Override
            public void onData(@NonNull String data) {
                fail();
            }

            @Override
            public void onError(@NonNull Throwable error) {
                observed[0] = error;
            }
        });
        assertEquals(observed[0], error);
    }
}
