package oauth2.token;

import java.awt.Desktop;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.json.JSONObject;

import oauth2.token.server.CallbackServer;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TokenManager {
  private String clientId;
  private String clientSecret;
  private String scopes;
  private String authorizationURLString;
  private String tokenURLString;
  private final String responseType = "code";
  private final String grantType = "authorization_code";
  private final String callbackServerURI = "http://localhost:8080";

  public TokenManager(
      String clientId,
      String clientSecret,
      String scopes,
      String authorizationURLString,
      String tokenURLString) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.scopes = scopes;
    this.authorizationURLString = authorizationURLString;
    this.tokenURLString = tokenURLString;
  }

  public Map<String, String> getTokens(String authorizationCode) throws Exception {
    Map<String, String> formData = new HashMap<String, String>();
    formData.put("client_id", clientId);
    formData.put("client_secret", clientSecret);
    formData.put("grant_type", grantType);
    formData.put("code", authorizationCode);
    formData.put("redirect_uri", callbackServerURI);

    OkHttpClient okHttpClient = new OkHttpClient();
    RequestBody tokenRequestBody =
        RequestBody.create(
            MediaType.get("application/x-www-form-urlencoded"), getURLEncodedFormData(formData));
    Request tokenRequest = new Request.Builder().url(tokenURLString).post(tokenRequestBody).build();
    Response tokenResponse = okHttpClient.newCall(tokenRequest).execute();
    JSONObject tokenResponseJSON = new JSONObject(tokenResponse.body().string());
    Map<String, String> tokens = new HashMap<String, String>();
    tokens.put("accessToken", tokenResponseJSON.getString("access_token"));
    tokens.put("refreshToken", tokenResponseJSON.getString("refresh_token"));
    return tokens;
  }

  private String getURLEncodedFormData(Map<String, String> formData) {
    StringBuilder urlEncodedFormData = new StringBuilder();
    formData.forEach(
        (parameter, value) -> urlEncodedFormData.append(parameter + "=" + value + "&"));
    return urlEncodedFormData.substring(0, urlEncodedFormData.length());
  }

  public String getAuthorizationCode() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    CallbackServer callbackServer = CallbackServer.getInstance();
    callbackServer.start(latch);
    Desktop.getDesktop().browse(getAuthorizationURI());
    latch.await();
    return callbackServer.getAuthorizationCode();
  }

  private URI getAuthorizationURI() throws Exception {
    HttpUrl authorizationURL =
        HttpUrl.parse(authorizationURLString)
            .newBuilder()
            .addQueryParameter("client_id", clientId)
            .addQueryParameter("response_type", responseType)
            .addQueryParameter("redirect_uri", callbackServerURI)
            .addQueryParameter("scope", scopes)
            .build();
    return authorizationURL.uri();
  }
}