package com.jeffpdavidson.fantasywear.api.parser;

import android.test.InstrumentationTestCase;

import com.jeffpdavidson.fantasywear.api.model.Matchup;
import com.jeffpdavidson.fantasywear.api.model.Team;
import com.jeffpdavidson.fantasywear.test.R;

import java.io.InputStream;
import java.io.InputStreamReader;

public class ScoreboardParserTest extends InstrumentationTestCase {
    public void testParser() throws Exception {
        Team myTeam = new Team.Builder()
                .name("Me")
                .is_owned_by_current_login(true)
                .logo_url("http://logo")
                .score("100.00")
                .build();
        Team opponentTeam = new Team.Builder()
                .name("Opponent")
                .logo_url("http://opplogo")
                .score("50.00")
                .build();
        Matchup expectedMatchup = new Matchup.Builder()
                .my_team(myTeam)
                .opponent_team(opponentTeam)
                .build();

        InputStream is = getInstrumentation().getContext().getResources()
                .openRawResource(R.raw.scoreboard_example);
        // Can't use try-with-resources here; we support pre-API-19 devices.
        //noinspection TryFinallyCanBeTryWithResources
        try {
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            assertEquals(expectedMatchup, ScoreboardParser.parseXml(isr));
        } finally {
            is.close();
        }
    }
}
