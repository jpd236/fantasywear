package com.jeffpdavidson.fantasywear.util;

import android.content.Intent;
import android.os.Parcelable;

import com.squareup.wire.Message;

import java.lang.reflect.Array;

/**
 * Utilities for reading/writing structured data to/from Intents via extras.
 *
 * NOTE: This class is designed for simplicity in writing test code and not performance. Do not use
 * in release code.
 */
public final class IntentExtraUtils {
    private IntentExtraUtils() {}

    public static <T extends Message> void putExtra(Intent intent, String key, T[] messages) {
        String[] encodedMessages = new String[messages.length];
        for (int i = 0; i < messages.length; i++) {
            encodedMessages[i] = WireUtil.encodeToString(messages[i]);
        }
        intent.putExtra(key, encodedMessages);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Message> T[] getMessageArrayExtra(Intent intent, String key,
            Class<T> messageClass) {
        String[] encodedMessages = intent.getStringArrayExtra(key);
        T[] messages = (T[]) Array.newInstance(messageClass, encodedMessages.length);
        for (int i = 0; i < encodedMessages.length; i++) {
            messages[i] = WireUtil.decodeFromString(encodedMessages[i], messageClass);
        }
        return messages;
    }

    @SuppressWarnings({"unchecked", "SuspiciousSystemArraycopy"})
    public static <T extends Parcelable> T[] getParcelableArrayExtra(Intent intent, String key,
            Class<T> parcelableClass) {
        Parcelable[] parcelables = intent.getParcelableArrayExtra(key);
        T[] array = (T[]) Array.newInstance(parcelableClass, parcelables.length);
        System.arraycopy(parcelables, 0, array, 0, parcelables.length);
        return array;
    }
}
