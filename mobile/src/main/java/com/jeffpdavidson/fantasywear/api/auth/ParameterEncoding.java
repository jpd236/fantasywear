package com.jeffpdavidson.fantasywear.api.auth;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Pattern;

final class ParameterEncoding {
    private static final String ENCODING = "UTF-8";

    // Exceptions to standard URL encoding.
    private static final Pattern PATTERN_PLUS = Pattern.compile("\\+");     // replaced with %20
    private static final Pattern PATTERN_ASTERISK = Pattern.compile("\\*"); // replaced with %2A
    private static final Pattern PATTERN_TILDE = Pattern.compile("%7E");    // replaced with ~

    private ParameterEncoding() {}

    static String encode(String str) {
        String encoded;
        try {
            encoded = URLEncoder.encode(str, ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 must be a supported encoding", e);
        }
        encoded = PATTERN_PLUS.matcher(encoded).replaceAll("%20");
        encoded = PATTERN_ASTERISK.matcher(encoded).replaceAll("%2A");
        encoded = PATTERN_TILDE.matcher(encoded).replaceAll("~");
        return encoded;
    }

    static String decode(String str) {
        try {
            return URLDecoder.decode(str, ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 must be a supported encoding", e);
        }
    }
}
