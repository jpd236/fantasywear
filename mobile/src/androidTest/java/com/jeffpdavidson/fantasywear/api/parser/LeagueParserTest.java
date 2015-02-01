package com.jeffpdavidson.fantasywear.api.parser;

import android.test.InstrumentationTestCase;
import android.test.MoreAsserts;

import com.jeffpdavidson.fantasywear.api.model.League;
import com.jeffpdavidson.fantasywear.test.R;

import java.io.InputStream;
import java.io.InputStreamReader;

public class LeagueParserTest extends InstrumentationTestCase {
    public void testParser() throws Exception {
        League league = new League.Builder()
                .league_key("key1")
                .league_name("name1")
                .build();

        InputStream is = getInstrumentation().getContext().getResources()
                .openRawResource(R.raw.league_example);
        // Can't use try-with-resources here; we support pre-API-19 devices.
        //noinspection TryFinallyCanBeTryWithResources
        try {
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            MoreAsserts.assertEquals(new League[] { league }, LeagueParser.parseXml(isr));
        } finally {
            is.close();
        }
    }
}
