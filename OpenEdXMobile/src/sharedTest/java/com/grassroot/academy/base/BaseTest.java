package com.grassroot.academy.base;

import androidx.annotation.NonNull;

import com.grassroot.academy.util.observer.MainThreadObservable;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.Executor;

@Ignore
@RunWith(RobolectricTestRunner.class)
public class BaseTest {
    @Before
    public final void beforeBaseTest() {
        MainThreadObservable.EXECUTOR = new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                command.run();
            }
        };
        MockitoAnnotations.initMocks(this);
    }
}
