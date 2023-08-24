package alexademo.android.test.alexa_demo_prahas;

import android.Manifest;
import android.media.AudioManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.content.Context;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.amazon.identity.auth.device.api.workflow.RequestContext;
//import com.oracle.tools.packager.Log;

import org.json.JSONException;
import org.json.JSONObject;

import android.provider.Settings;

import java.io.UnsupportedEncodingException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Callback;
import okhttp3.RequestBody;

import android.util.Log;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Response;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import alexademo.android.test.alexa_demo_prahas.connect.ApiResponse;
import alexademo.android.test.alexa_demo_prahas.connect.AvsItem;
import alexademo.android.test.alexa_demo_prahas.connect.AvsSpeakItem;
import alexademo.android.test.alexa_demo_prahas.connect.AvsTemplateItem;
import alexademo.android.test.alexa_demo_prahas.connect.AvsSpeakerItem;
import alexademo.android.test.alexa_demo_prahas.connect.ConnectManager;
import alexademo.android.test.alexa_demo_prahas.ui.AboutDialog;
import alexademo.android.test.alexa_demo_prahas.ui.InfoTemplateView;
import alexademo.android.test.alexa_demo_prahas.ui.WeatherTemplateView;
import alexademo.android.test.alexa_demo_prahas.util.AudioPlayer;
import alexademo.android.test.alexa_demo_prahas.util.DateTimeUtil;
import alexademo.android.test.alexa_demo_prahas.util.LoginManager;
import ee.ioc.phon.android.speechutils.RawAudioRecorder;

public class MainActivity extends AppCompatActivity {

    public static String androidId;
    public static String userCode;
    public static String deviceCode;
    OkHttpClient client;

    private String TAG = MainActivity.class.getName();
    private RequestContext mRequestContext;
    private View mLoginButton, mPressButton, mPulseView, mProcessingView;

    private RawAudioRecorder mRecorder;
    private static final int AUDIO_RATE = 16000;
    private boolean isRecording = false;

    private ConnectManager mConnectManager;
    private InfoTemplateView mInfoTemplateView;
    private WeatherTemplateView mWeatherTemplateView;
    private AudioPlayer mAudioPlayer;

    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        client = new OkHttpClient();
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        LoginManager.init(this);

        mAudioPlayer = new AudioPlayer(this);
        mRequestContext = RequestContext.create(this);
        mConnectManager = new ConnectManager(this);

        mWeatherTemplateView = findViewById(R.id.weather_template_view);
        mInfoTemplateView = findViewById(R.id.template_view);
        mLoginButton = findViewById(R.id.login_button);
        mPressButton = findViewById(R.id.press_button);
        mPulseView = findViewById(R.id.avi);
        mProcessingView = findViewById(R.id.processing_view);

        mLoginButton.setVisibility(LoginManager.isLogin() ? View.GONE : View.VISIBLE);
        mPressButton.setVisibility(LoginManager.isLogin() ? View.VISIBLE : View.GONE);

        try {
            postDeviceAuthorizationRequest();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // while(deviceCode == null){
        //
        // }

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // LoginManager.doLogin(mRequestContext, new LoginManager.LoginCallback() {
                //
                // @Override
                // public void onSuccess() {
                // setLoginStatus();
                //
                // long expireTime = LoginManager.getExpireTime();
                // Toast.makeText(MainActivity.this,
                // "Login Success, Token Expires At " + DateTimeUtil.getDateString(expireTime),
                // Toast.LENGTH_LONG).show();
                // }
                //
                // @Override
                // public void onFail() {
                // Toast.makeText(MainActivity.this, "Login Fail", Toast.LENGTH_LONG).show();
                // }
                // });

                requestDeviceTokens();

            }
        });

        mPressButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // long press to listen
                        startListening();
                        break;

                    case MotionEvent.ACTION_MOVE:
                        break;

                    case MotionEvent.ACTION_UP:
                        // release to stop recording and send request to alexa
                        stopListening();
                        break;

                    default:
                        break;
                }

                return true;
            }
        });

        setLoginStatus();
    }

    public void postDeviceAuthorizationRequest() throws UnsupportedEncodingException {

        String productId = "alexa_android_app";
        String deviceSerialNumber = androidId;
        String postUrl = "https://api.amazon.com/auth/O2/create/codepair";

        String scopeJson = "{\"alexa:all\":{\"productID\":\"" + productId
                + "\",\"productInstanceAttributes\":{\"deviceSerialNumber\":\"" + deviceSerialNumber + "\"}}}";

        RequestBody requestBody = new FormBody.Builder()
                .add("response_type", "device_code")
                .add("client_id", "amzn1.application-oa2-client.eb09d3db4b514acd80f50049f1d0b640")
                .add("scope", "alexa:all")
                .add("scope_data", scopeJson)
                .build();

        Request request = new Request.Builder().url(postUrl).header("Content-Type", "application/x-www-form-urlencoded")
                .post(requestBody).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String responseBody = response.body().string();
                            android.util.Log.d("RESPONSE IS", responseBody);
                            JSONObject jsonObject = new JSONObject(responseBody);
                            userCode = jsonObject.getString("user_code");
                            deviceCode = jsonObject.getString("device_code");
                            android.util.Log.d("USER CODE", userCode);
                            android.util.Log.d("DEVICE CODE", deviceCode);
                            Toast.makeText(MainActivity.this,
                                    "Enter " + userCode + " at amazon.com/code",
                                    Toast.LENGTH_LONG).show();

                            Toast.makeText(MainActivity.this,
                                    "Enter " + userCode + " at amazon.com/code",
                                    Toast.LENGTH_LONG).show();

                            Toast.makeText(MainActivity.this,
                                    "Enter " + userCode + " at amazon.com/code",
                                    Toast.LENGTH_LONG).show();

                            Toast.makeText(MainActivity.this,
                                    "Enter " + userCode + " at amazon.com/code",
                                    Toast.LENGTH_LONG).show();

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        });

    }

    public int requestDeviceTokens() {
        String postUrl = "https://api.amazon.com/auth/O2/token";

        RequestBody requestBody = new FormBody.Builder()
                .add("grant_type", "device_code")
                .add("device_code", deviceCode)
                .add("user_code", userCode)
                .build();

        Request request = new Request.Builder().url(postUrl).header("Content-Type", "application/x-www-form-urlencoded")
                .post(requestBody).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String responseBody = response.body().string();
                            // android.util.Log.d("RESPONSE IS", responseBody);
                            JSONObject jsonObject = new JSONObject(responseBody);
                            String accessToken = jsonObject.getString("access_token");
                            String refreshToken = jsonObject.getString("refresh_token");
                            long expireTime = jsonObject.getLong("expires_in");
                            // android.util.Log.d("USER CODE", userCode);
                            // android.util.Log.d("DEVICE CODE", deviceCode);
                            android.util.Log.d("ACCESS TOKEN IS", accessToken);
                            LoginManager.putAccessToken(accessToken, refreshToken, expireTime);

                            // Log.d("TOKENS ARE", response.body().string());
                            // Toast.makeText(MainActivity.this,
                            // "Login Successful",
                            // Toast.LENGTH_LONG).show();
                            setLoginStatus();
                            long expireTimeFinal = LoginManager.getExpireTime();
                            Toast.makeText(MainActivity.this,
                                    "Login Success, Token Expires At " + DateTimeUtil.getDateString(expireTimeFinal),
                                    Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            Toast.makeText(MainActivity.this,
                                    "Login Not Successful",
                                    Toast.LENGTH_LONG).show();
                            throw new RuntimeException(e);
                        } catch (JSONException e) {
                            Toast.makeText(MainActivity.this,
                                    "Login Not Successful",
                                    Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }
                });

            }
        });
        return -1;
    }

    private void setLoginStatus() {
        // token expired
        if (LoginManager.isLogin() && System.currentTimeMillis() > LoginManager.getExpireTime()) {
            Toast.makeText(this, "Token Expired, Refreshing Token..", Toast.LENGTH_LONG).show();
            LoginManager.doRefreshToken(new LoginManager.LoginCallback() {
                @Override
                public void onSuccess() {
                    long expireTime = LoginManager.getExpireTime();
                    Toast.makeText(MainActivity.this,
                            "Token Refreshed, Token Expires At " + DateTimeUtil.getDateString(expireTime),
                            Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFail() {
                    LoginManager.logout();
                    setLoginStatus();
                    Toast.makeText(MainActivity.this, "Refresh Token Failed", Toast.LENGTH_LONG).show();
                }
            });
            return;
        }
        mLoginButton.setVisibility(LoginManager.isLogin() ? View.GONE : View.VISIBLE);
        mPressButton.setVisibility(LoginManager.isLogin() ? View.VISIBLE : View.GONE);
    }

    private void startListening() {
        mPulseView.setVisibility(View.VISIBLE);
        if (!isRecording) {
            if (mRecorder == null) {
                mRecorder = new RawAudioRecorder(AUDIO_RATE);
            }
            mRecorder.start();
            isRecording = true;
        }
    }

    private void stopListening() {
        mPulseView.setVisibility(View.GONE);
        mProcessingView.setVisibility(View.VISIBLE);
        if (mRecorder != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final byte[] recordBytes = mRecorder.getCompleteRecording();

                    mRecorder.stop();
                    mRecorder.release();
                    mRecorder = null;
                    isRecording = false;

                    mConnectManager.sendRequest(recordBytes, new ConnectManager.Callback() {
                        @Override
                        public void onResponse(final ApiResponse res) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (res.getResponseCode() != 200) {
                                        Toast.makeText(MainActivity.this,
                                                res.getResponseCode() + " " + res.getMessage(), Toast.LENGTH_LONG)
                                                .show();
                                    } else {
                                        // Log.e("BEFORE REPONSE IS BEING CALLED", res.getMessage());

                                        onAlexaResponse(res.getAvsItems());
                                    }
                                    mProcessingView.setVisibility(View.GONE);
                                }
                            });
                        }
                    });
                }
            }).start();
        }
    }

    private void onAlexaResponse(List<AvsItem> res) {
        if (res != null) {
            for (AvsItem item : res) {
                Log.e("THIS IS THE ITEM", item.toString());
                if (item instanceof AvsSpeakItem) {
                    mAudioPlayer.play((AvsSpeakItem) item);

                }
                if (item instanceof AvsTemplateItem) {
                    final AvsTemplateItem templateItem = (AvsTemplateItem) item;
                    if (templateItem.isBodyType()) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mInfoTemplateView.setVisibility(View.VISIBLE);
                                mInfoTemplateView.setData(templateItem);
                                Animation am = AnimationUtils.loadAnimation(MainActivity.this, R.anim.anim_slide_in);
                                mInfoTemplateView.setAnimation(am);
                                am.startNow();
                            }
                        }, 100);
                    }
                    if (templateItem.isWeatherType()) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mWeatherTemplateView.setVisibility(View.VISIBLE);
                                mWeatherTemplateView.setData(templateItem);
                                Animation am = AnimationUtils.loadAnimation(MainActivity.this, R.anim.anim_slide_in);
                                mWeatherTemplateView.setAnimation(am);
                                am.startNow();
                            }
                        }, 100);
                    }

                }
                if (item instanceof AvsSpeakerItem) {
                    // Toast.makeText(MainActivity.this,
                    // "User wanted to change the volume to "
                    // + String.valueOf(((AvsSpeakerItem) item).getPayLoad().getVolume()))
                    // .show();

                    int newVolume = (int) ((AvsSpeakerItem) item).getPayLoad().getVolume();

                    Log.e("THE VOLUME IS", String.valueOf(newVolume));

                    audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);

                    Toast.makeText(MainActivity.this,
                            "Volume changed to "
                                    + String.valueOf(newVolume),
                            Toast.LENGTH_LONG).show();

                }
            }
        }
    }

    private void checkAndRequestPermissions() {
        int permission1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);
        int permission2 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permission3 = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (permission3 != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (permission2 != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permission1 != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRequestContext.onResume();
        checkAndRequestPermissions();
    }

    @Override
    public void onBackPressed() {
        if (mInfoTemplateView.getVisibility() == View.VISIBLE) {
            mInfoTemplateView.setVisibility(View.GONE);
            return;
        }
        if (mWeatherTemplateView.getVisibility() == View.VISIBLE) {
            mWeatherTemplateView.setVisibility(View.GONE);
            return;
        }
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            new AboutDialog(MainActivity.this).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            trimCache();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void trimCache() {
        try {
            File dir = getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }
}
