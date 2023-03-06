package com.grassroot.academy.test;

import com.grassroot.academy.BuildConfig;
import com.grassroot.academy.base.BaseTestCase;
import org.junit.Test;

import static org.junit.Assert.*;

public class PropertyTests extends BaseTestCase {

    @Test
    public void testGetDisplayVersionName() throws Exception {
        String name = BuildConfig.VERSION_NAME;
        assertTrue("failed to read versionName, found=" + name,
                name != null && !name.isEmpty());
    }
}
