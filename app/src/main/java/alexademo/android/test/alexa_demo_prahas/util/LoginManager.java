package alexademo.android.test.alexa_demo_prahas.util;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Callback;

import android.util.Log;

import org.json.JSONObject;


import java.io.IOException;

import okhttp3.Call;
import okhttp3.Response;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Base64;

import com.amazon.identity.auth.device.api.workflow.RequestContext;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import alexademo.android.test.alexa_demo_prahas.connect.ClientUtil;

public class LoginManager {
    private static final String TAG = LoginManager.class.getName();
    private static Context mContext;
    private static SharedPreferences mPref;
    private static final String KEY_TOKEN = "key_token";
    private final static String KEY_GRANT_TYPE = "grant_type";
    private final static String KEY_CODE = "code";
    private final static String KEY_REDIRECT_URI = "redirect_uri";
    private final static String KEY_CLIENT_ID = "client_id";
    private final static String KEY_CODE_VERIFIER = "code_verifier";
    private final static String KEY_REFRESH_TOKEN = "refresh_token";
    private final static String KEY_EXPIRE_TIME = "expire_time";

    public static String androidId;
    public static String userCode;
    public static String deviceCode;
    public static OkHttpClient client;


    private static Handler mHandler;

    public static void init(Context context) {
        mContext = context;
        mPref = context.getSharedPreferences("alexa_demo", Context.MODE_PRIVATE);
        mHandler = new Handler();
    }

    public static boolean isLogin() {
        return mPref.contains(KEY_TOKEN);
    }

    public static void logout() {
        mPref.edit().remove(KEY_TOKEN).apply();
    }

    public static String getToken() {
        return mPref.getString(KEY_TOKEN, "");
    }

    public static long getExpireTime() {
        return mPref.getLong(KEY_EXPIRE_TIME, 0);
    }

    public static void doLogin(RequestContext requestContext, final LoginCallback callback) {

        final String authorizationCode = "a";
        final String redirectUri = "b";
        final String clientId = "c";

        mPref.edit().putString(KEY_CLIENT_ID, clientId).commit();
//
        doFetchAccessToken(authorizationCode, redirectUri, clientId, callback);

//        requestContext.registerListener(new AuthorizeListener() {
//            /* Authorization was completed successfully. */
//            @Override
//            public void onSuccess(AuthorizeResult result) {
//                final String authorizationCode = result.getAuthorizationCode();
//                final String redirectUri = result.getRedirectURI();
//                final String clientId = result.getClientId();
//
//                Log.e("AUTH STUFF", authorizationCode + " || " + redirectUri + " || " + clientId);
//
//                mPref.edit().putString(KEY_CLIENT_ID, clientId).commit();
//
//                doFetchAccessToken(authorizationCode, redirectUri, clientId, callback);
//            }
//
//            /*
//             * There was an error during the attempt to authorize the
//             * application.
//             */
//            @Override
//            public void onError(AuthError ae) {
//                Log.e(TAG, "onError result=" + ae.getMessage());
//                mHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        callback.onFail();
//                    }
//                });
//            }
//
//            /* Authorization was cancelled before it could be completed. */
//            @Override
//            public void onCancel(AuthCancellation cancellation) {
//                Log.e(TAG, "onCancel");
//            }
//        });

        //
        // String authorizationCode = "ANAQRVdycYvfrhsKeJXq";
        // String redirectUri = "amzn://alexademo.ellison.test.alexa_demo_prahas";
        // String clientId =
        // "amzn1.application-oa2-client.89292917aabc4026af7677929078f6df";

        final JSONObject scopeData = new JSONObject();
        final JSONObject productInstanceAttributes = new JSONObject();

        // this code is responsible for registering your device - opens amazon to
        // register your device

//        try {
//            String android_id = Settings.Secure.getString(mContext.getContentResolver(),
//                    Settings.Secure.ANDROID_ID);
//            productInstanceAttributes.put("deviceSerialNumber", android_id);
//            scopeData.put("productInstanceAttributes", productInstanceAttributes);
//            scopeData.put("productID", Constant.PRODUCT_ID);
//
//            AuthorizationManager.authorize(new AuthorizeRequest.Builder(requestContext)
//                    .addScope(ScopeFactory.scopeNamed("alexa:all", scopeData))
//                    .forGrantType(AuthorizeRequest.GrantType.AUTHORIZATION_CODE)
//                    .withProofKeyParameters(getCodeChallenge(), "S256")
//                    .shouldReturnUserData(false)
//                    .build());
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }




    public static void doRefreshToken(final LoginCallback callback) {
        Log.e(TAG, "doRefreshToken");
        String url = "https://api.amazon.com/auth/O2/token";
        // set up our arguments for the api call, these will be the call headers
        FormBody.Builder builder = new FormBody.Builder()
                .add(KEY_GRANT_TYPE, "refresh_token")
                .add(KEY_REFRESH_TOKEN, mPref.getString(KEY_REFRESH_TOKEN, ""));
        builder.add(KEY_CLIENT_ID, mPref.getString(KEY_CLIENT_ID, ""));

        OkHttpClient client = ClientUtil.getTLS12OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .post(builder.build())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String s = response.body().string();
                // final TokenResponse tokenResponse = new Gson().fromJson(s,
                // TokenResponse.class);
                final TokenResponse tokenResponse = getHardcodedTokenResponse();

                Log.e(TAG, "doRefreshToken at=" + tokenResponse.access_token);
                Log.e(TAG, "doRefreshToken rt=" + tokenResponse.refresh_token);

                mPref.edit().putString(KEY_TOKEN, tokenResponse.access_token).commit();
                mPref.edit().putString(KEY_REFRESH_TOKEN, tokenResponse.refresh_token).commit();
                mPref.edit().putLong(KEY_EXPIRE_TIME, System.currentTimeMillis() + tokenResponse.expires_in * 1000)
                        .commit();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess();
                    }
                });
            }
        });
    }


    public static void putAccessToken(String accessToken, String refreshToken, long expireTime){

        final TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.access_token = accessToken;
        tokenResponse.refresh_token = refreshToken;
        tokenResponse.expires_in = expireTime;
        tokenResponse.token_type = "Bearer";

        mPref.edit().putString(KEY_TOKEN, tokenResponse.access_token).commit();
        mPref.edit().putString(KEY_REFRESH_TOKEN, tokenResponse.refresh_token).commit();
        mPref.edit().putLong(KEY_EXPIRE_TIME, System.currentTimeMillis() + tokenResponse.expires_in * 1000)
                .commit();

    }
    private static void doFetchAccessToken(String authorizationCode, String redirectUri, String clientId,
            final LoginCallback callback) {
        // this url shouldn't be hardcoded, but it is, it's the Amazon auth access token
        // endpoint
        String url = "https://api.amazon.com/auth/O2/token";

        // set up our arguments for the api call, these will be the call headers
        FormBody.Builder builder = new FormBody.Builder()
                .add(KEY_GRANT_TYPE, "authorization_code")
                .add(KEY_CODE, authorizationCode);
        builder.add(KEY_REDIRECT_URI, redirectUri);
        builder.add(KEY_CLIENT_ID, clientId);
        builder.add(KEY_CODE_VERIFIER, getCodeVerifier());

        OkHttpClient client = ClientUtil.getTLS12OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .post(builder.build())
                .build();

        final TokenResponse tokenResponse = getHardcodedTokenResponse();
        Log.e(TAG, "getAccessToken=" + tokenResponse.access_token + " ex=" + tokenResponse.expires_in);
        Log.e("INSIDE ON RESPONSE", "INSIDE ON RESPONSE");
        mPref.edit().putString(KEY_TOKEN, tokenResponse.access_token).commit();
        mPref.edit().putString(KEY_REFRESH_TOKEN, tokenResponse.refresh_token).commit();
        mPref.edit().putLong(KEY_EXPIRE_TIME, System.currentTimeMillis() + tokenResponse.expires_in * 1000)
                .commit();

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess();
            }
        });

//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, final IOException e) {
//                e.printStackTrace();
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                String s = response.body().string();
//                // final TokenResponse tokenResponse = new Gson().fromJson(s,
//                // TokenResponse.class);
//                final TokenResponse tokenResponse = getHardcodedTokenResponse();
//                Log.e(TAG, "getAccessToken=" + tokenResponse.access_token + " ex=" + tokenResponse.expires_in);
//                Log.e("INSIDE ON RESPONSE", "INSIDE ON RESPONSE");
//                mPref.edit().putString(KEY_TOKEN, tokenResponse.access_token).commit();
//                mPref.edit().putString(KEY_REFRESH_TOKEN, tokenResponse.refresh_token).commit();
//                mPref.edit().putLong(KEY_EXPIRE_TIME, System.currentTimeMillis() + tokenResponse.expires_in * 1000)
//                        .commit();
//                mHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        callback.onSuccess();
//                    }
//                });
//            }
//        });
    }

    private static String getCodeVerifier() {
        if (mPref.contains(KEY_CODE_VERIFIER)) {
            return mPref.getString(KEY_CODE_VERIFIER, "");
        }
        String verifier = createCodeVerifier();
        mPref.edit().putString(KEY_CODE_VERIFIER, verifier).apply();
        return verifier;
    }

    static String createCodeVerifier() {
        return createCodeVerifier(128);
    }

    /**
     * Create a String hash based on the code verifier, this is used to verify the
     * Token exchanges
     * 
     * @return
     */
    private static String getCodeChallenge() {
        String verifier = getCodeVerifier();
        return base64UrlEncode(getHash(verifier));
    }

    /**
     * Create a new code verifier for our token exchanges
     *
     * @return the new code verifier
     */
    static String createCodeVerifier(int count) {
        char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Encode a byte array into a string, while trimming off the last characters, as
     * required by the Amazon token server
     * <p>
     * See: http://brockallen.com/2014/10/17/base64url-encoding/
     *
     * @param arg our hashed string
     * @return a new Base64 encoded string based on the hashed string
     */
    private static String base64UrlEncode(byte[] arg) {
        String s = Base64.encodeToString(arg, 0); // Regular base64 encoder
        s = s.split("=")[0]; // Remove any trailing '='s
        s = s.replace('+', '-'); // 62nd char of encoding
        s = s.replace('/', '_'); // 63rd char of encoding
        return s;
    }

    /**
     * Hash a string based on the SHA-256 message digest
     *
     * @param password
     * @return
     */
    private static byte[] getHash(String password) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        }
        digest.reset();
        return digest.digest(password.getBytes());
    }

    public interface LoginCallback {
        void onSuccess();

        void onFail();
    }

    public static class TokenResponse {
        public String access_token;
        public String refresh_token;
        public String token_type;
        public long expires_in;
    }

    private static TokenResponse getHardcodedTokenResponse() {
        TokenResponse response = new TokenResponse();
        response.access_token = "Atza|IwEBIKZp6VDbPYNfhLxPrJtNc3X6N_ncfVFutoWD6Me4dtV5FZx51DOisholnfhYr3Pru7lxdYmHqO__xyuIubH05ecRnWjOr7FUGaE6H5yp-fsYXXNPvDR5-NK0T8Wmoytlgr-16bjYwF5eBqEUn2DwoIAH9hIzg9PlqNVtgxLXtmS7vqHB1K8mu-wtG8WCBaMDg9lFpgOmxm7ogatIHKSLdZ1sKH2q_noOMO23FFhd_wCsPuvmS0xHzy-iFZsAYGeH2kbG4p6Ek8644d7K472kND-Wq21htyRVVAxnEcs2e5gZL8aXveNfyis4hznFPJkof4bnQ0lcCF87497ZpzIAUnxHL38OGvB7PGDlGKsytFe_tA";
        response.refresh_token = "Atzr|IwEBIPh_-SLM2XfXujJmv0KlRze5VCovA4pth-legEM9Ja2hJLI5BlYWYa-hzljKy033rMjleBzd4h0UF2mHI5gHBDRo5BF5lgC2UCbnhm85b18_XetwHTsHYNnq6pUDWChYH4CIsp_5pPOX6EzNFOyVDh1xRZnfYC3KvQkiuov5rM8lAplcdYuMTQ1y5cbFzTxwplLwneO3pYeNtUVt30kUPLooe4st9acdU_a4PYW5fuYjuF0kOg3evdeykR99wQXFiklH_loVkRkNuicogX7Abr3qllsBLv33FES4KTrgAJ4_6e5zdHbtYU8PlFyPaSlKql04YpIicXCOYN8kJctJY-Ao9BIOxKyqtk7b1MuqvVYK4_WL4nV7TbaIWJ4aPklBeyQ";
        response.token_type = "Bearer";
        response.expires_in = 3600; // Set the expiration time in seconds
        return response;
    }

}
