package com.jeffpdavidson.fantasywear.util;

import android.util.Base64;

import com.jeffpdavidson.fantasywear.log.FWLog;
import com.squareup.wire.Message;
import com.squareup.wire.Wire;

import java.io.IOException;

/** Utilities for decoding/encoding Wire protobufs as Strings. */
public final class WireUtil {
    private static final Wire WIRE = new Wire();

    private WireUtil() {}

    /** Encode a message into a String that can be decoded with {@link #decodeFromString}. */
    public static <T extends Message> String encodeToString(T message) {
        return Base64.encodeToString(message.toByteArray(), Base64.DEFAULT);
    }

    /**
     * Decode a string created with {@link #encodeToString} back to the original message.
     *
     * Returns null if the string can't be decoded as the given message type.
     */
    public static <T extends Message> T decodeFromString(String str, Class<T> protoClass) {
        try {
            return WIRE.parseFrom(Base64.decode(str, Base64.DEFAULT), protoClass);
        } catch (IOException e) {
            FWLog.e(e, "Error decoding proto of type %s", protoClass.getSimpleName());
            return null;
        }
    }
}
