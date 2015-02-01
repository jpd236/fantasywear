package com.jeffpdavidson.fantasywear.api.auth;

import com.android.volley.NetworkResponse;
import com.android.volley.Request.Method;
import com.android.volley.Response;

import junit.framework.TestCase;

import java.util.SortedMap;
import java.util.TreeMap;

public class OAuthRequestTest extends TestCase {
    /**
     * Test the signature calculation algorithm.
     *
     * This example data is taken from the OAuth 1.0a specification at http://oauth.net/core/1.0a/,
     * section Appendix A.5, "Accessing Protected Resources".  However, the GET parameters have been
     * moved into the headers section, because OAuthRequest does not support GET parameters as
     * FantasyWear never uses them.
     */
    public void testGetHeaders() throws Exception {
        OAuthRequest<String> request = new OAuthRequest<String>(Method.GET,
                "http://photos.example.net/photos", null, null) {
            @Override
            protected SortedMap<String, String> getOAuthHeaders(Token token) {
                SortedMap<String, String> headers = new TreeMap<>();
                headers.put("oauth_consumer_key", "dpf43f3p2l4k3l03");
                headers.put("oauth_token", "nnch734d00sl2jdk");
                headers.put("oauth_signature_method", "HMAC-SHA1");
                headers.put("oauth_timestamp", "1191242096");
                headers.put("oauth_nonce", "kllo9940pd9333jh");
                headers.put("oauth_version", "1.0");
                headers.put("file", "vacation.jpg");
                headers.put("size", "original");
                return headers;
            }

            @Override
            protected Token getToken() {
                return new Token.Builder().token_secret("pfkkdhi9sl3r4s00").build();
            }

            @Override
            protected String getConsumerSecret() {
                return "kd94hf93k423kf44";
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse networkResponse) {
                return null;
            }
        };

        assertTrue(request.getHeaders().get("Authorization")
                .contains("oauth_signature=\"tR3%2BTy81lMeYAr%2FFid0kMTYa%2FWM%3D\""));
    }
}
