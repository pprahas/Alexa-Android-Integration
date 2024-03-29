package alexademo.android.test.alexa_demo_prahas.connect;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import alexademo.android.test.alexa_demo_prahas.R;
import alexademo.android.test.alexa_demo_prahas.util.LoginManager;
import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ConnectManager {
    private String TAG = ConnectManager.class.getName();
    private Context mContext;

    public ConnectManager(Context context) {
        mContext = context;
    }

    public interface Callback {
        void onResponse(ApiResponse res);

    }

    public void sendRequest(byte[] fileBytes, Callback callback) {
        try {
            String url = new StringBuilder()
                    .append(mContext.getString(R.string.alexa_api))
                    .append("/")
                    .append(mContext.getString(R.string.alexa_api_version))
                    .append("/")
                    .append("events")
                    .toString();

            Request.Builder requestBuilder = new Request.Builder();
            requestBuilder.url(url);
            requestBuilder.addHeader("Authorization", "Bearer " + LoginManager.getToken());

            String event = Event.getSpeechRecognizerEvent();
            MultipartBody.Builder bodyBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("metadata", "metadata",
                            RequestBody.create(MediaType.parse("application/json; charset=UTF-8"), event));
            bodyBuilder.addFormDataPart("audio", "speech.wav",
                    RequestBody.create(MediaType.parse("application/octet-stream"), fileBytes));
            requestBuilder.post(bodyBuilder.build());
            Request request = requestBuilder.build();

            Call currentCall = ClientUtil.getTLS12OkHttpClient().newCall(request);

            try {
                Response response = currentCall.execute();
                if (response.code() == HttpURLConnection.HTTP_NO_CONTENT) {
                    Log.w(TAG, "Received a 204 response code from Amazon, is this expected?");
                }
                if (response.code() == 200)
                    Log.e("RESPONSE CODE IS", String.valueOf(response.code()));
                // responseBody = response.body()
                // Log.e
                final List<AvsItem> avsItems = response.code() == HttpURLConnection.HTTP_NO_CONTENT ? null
                        : ApiParser.parse(response.body().byteStream(), getBoundary(response));
                response.body().close();

                if (callback != null) {
                    ApiResponse res = new ApiResponse();
                    res.setAvsItems(avsItems);
                    res.setResponseCode(response.code());
                    if (response.code() == 204) {
                        res.setMessage("Received a 204 response code from Amazon");
                    }
                    if (response.code() == 200) {
                        Log.e("BODY OF THE RESPONSE IS", response.body().toString());
                    }
                    callback.onResponse(res);
                }

            } catch (IOException exp) {
                if (!currentCall.isCanceled()) {
                }
                exp.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected String getBoundary(Response response) throws IOException {
        Headers headers = response.headers();
        String header = headers.get("content-type");
        String boundary = "";

        if (header != null) {
            Pattern pattern = Pattern.compile("boundary=(.*?);");
            Matcher matcher = pattern.matcher(header);
            if (matcher.find()) {
                boundary = matcher.group(1);
            }
        } else {
            Log.i(TAG, "Body: " + response.body().string());
        }
        return boundary;
    }

}
