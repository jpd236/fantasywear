package com.jeffpdavidson.fantasywear.util;

import android.support.test.runner.AndroidJUnit4;

import com.jeffpdavidson.fantasywear.api.model.League;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WireUtilTest {
    @Test
    public void encodeAndDecodeEmptyMessage() {
        League league = new League.Builder().build();
        Assert.assertEquals(league,
                WireUtil.decodeFromString(WireUtil.encodeToString(league), League.class));
    }

    @Test
    public void encodeAndDecodePopulatedMessage() {
        League league = new League.Builder().account_name("acct").league_key("key").build();
        Assert.assertEquals(league,
                WireUtil.decodeFromString(WireUtil.encodeToString(league), League.class));
    }
}
