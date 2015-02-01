package com.jeffpdavidson.fantasywear.log;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FWLogTest {
    @Test
    public void formatMsgWithNoArgs() {
        Assert.assertEquals("[FWLogTest.formatMsgWithNoArgs] test message",
                FWLog.formatMsg("test message"));
    }

    @Test
    public void formatMsgWithArgs() {
        Assert.assertEquals("[FWLogTest.formatMsgWithArgs] test message 2",
                FWLog.formatMsg("test message %d", 2));
    }

    @Test
    public void formatMsgFromInnerClass() {
        Assert.assertEquals("[FWLogTest$InnerClass.formatMsg] test message",
                InnerClass.formatMsg("test message"));
    }

    @Test
    public void formatMsgFromAnonymousInnerClass() {
        new Runnable() {
            @Override
            public void run() {
                Assert.assertEquals(
                        "[FWLogTest$1.run] test message", FWLog.formatMsg("test message"));
            }
        }.run();
    }

    private static class InnerClass {
        static String formatMsg(String msg) {
            return FWLog.formatMsg(msg);
        }
    }
}
