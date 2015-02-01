package com.jeffpdavidson.fantasywear.api.auth;

import android.text.TextUtils;
import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.jeffpdavidson.fantasywear.BuildConfig;
import com.jeffpdavidson.fantasywear.annotations.VisibleForTesting;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Base class for an OAuth 1.0a authenticated Volley request.
 *
 * OAuth headers are added to every request - more may be added by overriding
 * {@link #getOAuthHeaders(Token)} - and the request is signed with the token provided in
 * {@link #getToken()}. Uses HMAC-SHA1 signatures and the Authorization header.
 *
 * This deliberately only supports the subset of OAuth 1.0a used by Yahoo (and within that,
 * FantasyWear), and may need to be extended to support other services.
 *
 * @param <T> The type of the response object
 */
abstract class OAuthRequest<T> extends Request<T> {
    private static final String HEADER = "Authorization";
    private static final String PREAMBLE = "OAuth ";
    private static final String HEADER_SEPARATOR = ", ";
    private static final String VERSION = "1.0";
    private static final String ENCODING = "UTF-8";
    private static final String SIGNATURE_METHOD = "HMAC-SHA1";
    private static final String SIGNATURE_SEPARATOR = "&";
    private static final String ALGORITHM_HMAC_SHA1 = "HmacSHA1";

    // Keys for OAuth request parameters that are sent for all requests.
    private static final String KEY_CONSUMER_KEY = "oauth_consumer_key";
    private static final String KEY_TIMESTAMP = "oauth_timestamp";
    private static final String KEY_NONCE = "oauth_nonce";
    private static final String KEY_VERSION = "oauth_version";
    private static final String KEY_TOKEN = "oauth_token";
    private static final String KEY_SIGNATURE_METHOD = "oauth_signature_method";
    private static final String KEY_SIGNATURE = "oauth_signature";

    private static final SecureRandom mRandom = new SecureRandom();

    private final Listener<T> mListener;

    public OAuthRequest(int method, String url, Listener<T> listener, ErrorListener errorListener) {
        super(method, url, errorListener);
        mListener = listener;
    }

    @Override
    protected void deliverResponse(T response) {
        mListener.onResponse(response);
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Token token = getToken();

        // Put together all of the OAuth headers, except the signature.
        Map<String, String> oAuthHeaders = getOAuthHeaders(token);
        StringBuilder combinedHeaders = new StringBuilder(PREAMBLE);
        StringBuilder normalizedHeaders = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> header : oAuthHeaders.entrySet()) {
            if (!first) {
                combinedHeaders.append(HEADER_SEPARATOR);
                normalizedHeaders.append(SIGNATURE_SEPARATOR);
            } else {
                first = false;
            }
            String encodedValue = ParameterEncoding.encode(header.getValue());
            combinedHeaders.append(String.format("%s=\"%s\"", header.getKey(), encodedValue));
            normalizedHeaders.append(String.format("%s=%s", header.getKey(), encodedValue));
        }

        // Calculate the OAuth signature, and add it as a final OAuth header.
        String signature = calculateSignature(token, normalizedHeaders.toString());
        combinedHeaders.append(HEADER_SEPARATOR);
        combinedHeaders.append(String.format("%s=\"%s\"", KEY_SIGNATURE,
                ParameterEncoding.encode(signature)));

        // Send the combined set of OAuth headers as the Authorization header.
        Map<String, String> headers = new HashMap<>();
        headers.put(HEADER, combinedHeaders.toString());
        return headers;
    }

    protected abstract Token getToken() throws AuthFailureError;

    protected SortedMap<String, String> getOAuthHeaders(Token token) throws AuthFailureError {
        SortedMap<String, String> headers = new TreeMap<>();
        headers.put(KEY_VERSION, VERSION);
        headers.put(KEY_CONSUMER_KEY, BuildConfig.CONSUMER_KEY);

        String currentTimeSec =
                Long.toString(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        headers.put(KEY_TIMESTAMP, currentTimeSec);
        // Nonce should be unique per timestamp, but unpredictable for any given timestamp.
        headers.put(KEY_NONCE, currentTimeSec.concat(Integer.toString(mRandom.nextInt())));

        headers.put(KEY_SIGNATURE_METHOD, SIGNATURE_METHOD);

        // Append token if non-empty. May be empty when requesting a new token.
        if (!TextUtils.isEmpty(token.token)) {
            headers.put(KEY_TOKEN, token.token);
        }

        return headers;
    }

    @VisibleForTesting
    protected String getConsumerSecret() {
        return BuildConfig.CONSUMER_SECRET;
    }

    private String calculateSignature(Token token, String normalizedHeaders) {
        // Calculate the normalized request to be signed.
        String baseString = String.format("%s&%s&%s",
                getMethodString(),
                ParameterEncoding.encode(getUrl()),
                ParameterEncoding.encode(normalizedHeaders));

        // Determine the signing key from the consumer secret and token secret.
        String tokenSecret = token.token_secret;
        if (tokenSecret == null) {
            tokenSecret = "";
        }
        String keyString = String.format("%s&%s",
                ParameterEncoding.encode(getConsumerSecret()),
                ParameterEncoding.encode(tokenSecret));

        // Convert key and message to byte arrays.
        byte[] key;
        byte[] msg;
        try {
            key = keyString.getBytes(ENCODING);
            msg = baseString.getBytes(ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 must be a supported encoding", e);
        }

        // Sign the message.
        SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM_HMAC_SHA1);
        Mac mac;
        try {
            mac = Mac.getInstance(ALGORITHM_HMAC_SHA1);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("HMAC-SHA1 must be a supported algorithm", e);
        }
        try {
            mac.init(keySpec);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("Got InvalidKeyException despite using a valid key", e);
        }
        byte[] signature = mac.doFinal(msg);

        // Return the signature as a base64-encoded string.
        return Base64.encodeToString(signature, Base64.NO_WRAP);
    }

    private String getMethodString() {
        int method = getMethod();
        switch (method) {
            case Method.GET:
                return "GET";
            case Method.POST:
                return "POST";
            default:
                throw new UnsupportedOperationException("Unsupported method: " + method);
        }
    }
}
