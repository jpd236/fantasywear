package com.jeffpdavidson.fantasywear.api.parser;

import android.util.Xml;

import com.android.volley.ParseError;
import com.jeffpdavidson.fantasywear.api.model.Matchup;
import com.jeffpdavidson.fantasywear.api.model.Team;
import com.squareup.wire.Wire;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.Reader;

/**
 * XML parser for the scoreboard of a fantasy league.
 *
 * Input XML is obtained via the league/[league_key]/scoreboard API.
 */
public final class ScoreboardParser {
    private ScoreboardParser() {}

    public static Matchup parseXml(Reader reader) throws ParseError {
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

    private static Matchup parseFantasyContent(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        parser.next();
        parser.require(XmlPullParser.START_TAG, null, "fantasy_content");
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.START_TAG &&
                    "matchup".equals(parser.getName())) {
                return parseMatchup(parser);
            }
        }
        throw new XmlPullParserException("No <league> in <fantasy_content> tag");
    }

    private static Matchup parseMatchup(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "matchup");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                if ("teams".equals(parser.getName())) {
                    if (!"2".equals(parser.getAttributeValue(null, "count"))) {
                        // TODO: Support other fantasy league formats (points, roto, others?).
                        throw new UnsupportedOperationException(
                                "Only head-to-head leagues are supported");
                    }
                    Team[] teams = parseTeams(parser);
                    if (Wire.get(teams[0].is_owned_by_current_login, false)) {
                        return new Matchup.Builder()
                                .my_team(teams[0])
                                .opponent_team(teams[1])
                                .build();
                    } else if (Wire.get(teams[1].is_owned_by_current_login, false)) {
                        return new Matchup.Builder()
                                .my_team(teams[1])
                                .opponent_team(teams[0])
                                .build();
                    }
                } else {
                    Util.skipCurrentTag(parser);
                }
            }
        }
        // If the user's team wasn't found, we'll try the next matchup in parseMatchups.
        return null;
    }

    private static Team[] parseTeams(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "teams");
        Team[] teams = new Team[Integer.parseInt(parser.getAttributeValue(null, "count"))];
        int i = 0;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                if ("team".equals(parser.getName())) {
                    if (i >= teams.length) {
                        throw new XmlPullParserException("More teams than specified in count");
                    }
                    teams[i++] = parseTeam(parser);
                } else {
                    Util.skipCurrentTag(parser);
                }
            }
        }
        if (i < teams.length) {
            throw new XmlPullParserException("Found " + i + " teams, expected " + teams.length);
        }
        parser.require(XmlPullParser.END_TAG, null, "teams");
        return teams;
    }

    private static Team parseTeam(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "team");
        Team.Builder builder = new Team.Builder();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                switch (parser.getName()) {
                    case "name":
                        builder.name(parser.nextText());
                        break;
                    case "is_owned_by_current_login":
                        builder.is_owned_by_current_login("1".equals(parser.nextText()));
                        break;
                    case "team_logos":
                        builder.logo_url(parseTeamLogos(parser));
                        break;
                    case "team_points":
                        builder.score(parseTeamPoints(parser));
                        break;
                    default:
                        Util.skipCurrentTag(parser);
                        break;
                }
            }
        }
        parser.require(XmlPullParser.END_TAG, null, "team");
        return builder.build();
    }

    private static String parseTeamLogos(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "team_logos");
        String url = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                if ("team_logo".equals(parser.getName())) {
                    url = parseTeamLogo(parser);
                } else {
                    Util.skipCurrentTag(parser);
                }
            }
        }
        parser.require(XmlPullParser.END_TAG, null, "team_logos");
        return url;
    }

    private static String parseTeamLogo(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "team_logo");
        String url = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                if ("url".equals(parser.getName())) {
                    url = parser.nextText();
                } else {
                    Util.skipCurrentTag(parser);
                }
            }
        }
        parser.require(XmlPullParser.END_TAG, null, "team_logo");
        return url;
    }

    private static String parseTeamPoints(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "team_points");
        String score = null;
        while (parser.next() != XmlPullParser.END_TAG && !"team_points".equals(parser.getName())) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                if ("total".equals(parser.getName())) {
                    score = parser.nextText();
                } else {
                    Util.skipCurrentTag(parser);
                }
            }
        }
        parser.require(XmlPullParser.END_TAG, null, "team_points");
        return score;
    }
}
