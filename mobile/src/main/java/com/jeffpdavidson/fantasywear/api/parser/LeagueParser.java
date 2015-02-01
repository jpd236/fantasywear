package com.jeffpdavidson.fantasywear.api.parser;

import android.support.annotation.Nullable;
import android.util.Xml;

import com.android.volley.ParseError;
import com.jeffpdavidson.fantasywear.api.model.League;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * XML parser for the list of fantasy leagues a user belongs to.
 *
 * Input XML is obtained via the users/games/leagues API.
 */
public final class LeagueParser {
    private LeagueParser() {}

    public static League[] parseXml(Reader reader) throws ParseError {
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(reader);
            return parseFantasyContent(parser);
        } catch (XmlPullParserException | IOException e) {
            // We make the assumption that an IOException is a parser error rather than an error
            // reading the file, because in practice, we will read the full HTTP response as a
            // String before attempting to parse the XML.
            throw new ParseError(e);
        }
    }

    private static League[] parseFantasyContent(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        parser.next();
        parser.require(XmlPullParser.START_TAG, null, "fantasy_content");
        List<League> leagues = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.START_TAG &&
                    "league".equals(parser.getName())) {
                League league = parseLeague(parser);
                if (league != null) {
                    leagues.add(league);
                }
            }
        }
        return leagues.toArray(new League[leagues.size()]);
    }

    @Nullable
    private static League parseLeague(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "league");
        League.Builder league = new League.Builder();
        boolean isSupported = false;
        while (parser.next() != XmlPullParser.END_TAG && !"league".equals(parser.getName())) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                if ("league_key".equals(parser.getName())) {
                    league.league_key(parser.nextText());
                } else if ("name".equals(parser.getName())) {
                    league.league_name(parser.nextText());
                } else if ("scoring_type".equals(parser.getName())) {
                    // Only return head-to-head leagues.
                    // TODO: Support other fantasy league formats (points, roto, others?).
                    isSupported = parser.nextText().contains("head");
                } else {
                    Util.skipCurrentTag(parser);
                }
            }
        }
        parser.require(XmlPullParser.END_TAG, null, "league");
        if (!isSupported) {
            return null;
        }
        return league.build();
    }
}
