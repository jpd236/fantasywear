package com.jeffpdavidson.fantasywear.api.auth;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;

import junit.framework.TestCase;

import java.nio.charset.Charset;

public class OAuthTokenRequestTest extends TestCase {
    public void testParseNetworkResponse() throws Exception {
        OAuthTokenRequest request =
                new OAuthTokenRequest("", new Token.Builder().build(), null, null, null) {
                    @Override
                    protected long currentTimeMillis() {
                        return 10000 * 1000L;
                    }
                };
        String respStr = "oauth_token=aaa&oauth_token_secret=bbb&oauth_session_handle=ccc"
                + "&oauth_expires_in=5000&oauth_authorization_expires_in=10000"
                + "&xoauth_request_auth_url=" + ParameterEncoding.encode("http://test")
                + "&xoauth_yahoo_guid=12345&nonsense_header_should_not_crash=1";
        NetworkResponse netResp = new NetworkResponse(respStr.getBytes(Charset.forName("UTF-8")));
        Response<Token> resp = request.parseNetworkResponse(netResp);
        assertTrue(resp.isSuccess());
        Token token = new Token.Builder()
                .token("aaa")
                .token_secret("bbb")
                .session_handle("ccc")
                .expiration_time_sec(15000L)
                .authorization_expiration_time_sec(20000L)
                .request_auth_url("http://test")
                .yahoo_guid("12345")
                .build();
        assertEquals(token, resp.result);
    }
}
