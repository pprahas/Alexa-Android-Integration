package alexademo.android.test.alexa_demo_prahas.connect;

public abstract class AvsItem {
    String token;
    public AvsItem(String token){
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}