package com.jeffpdavidson.fantasywear.protocol;

/** Constants defining path prefixes for communication to/from the wear device. */
public final class Paths {
    private Paths() {}

    /** Path for league data. Use {@link LeagueData} rather than using this directly. */
    static final String LEAGUE = "league";

    /** Path used with the MessageApi for the wear device to request a sync on the host device. */
    public static final String SYNC = "sync";

    /**
     * Path used with the MessageApi for the wear device to acknowledge a sync.
     *
     * Used to work around an apparent Wear bug where the first few DataItems don't immediately
     * reach the Wear device. For the initial sync of a league, we send messages every few seconds
     * until we receive an ACK from the wear device.
     */
    public static final String ACK = "ack";
}
