package alexademo.android.test.alexa_demo_prahas.connect;

public class AvsSpeakerItem extends AvsItem {
    private Directive.Payload mPayLoad;

    public AvsSpeakerItem(String token, Directive.Payload payload) {
        super(token);
        mPayLoad = payload;
    }

    public Directive.Payload getPayLoad() {
        return mPayLoad;
    }

    public void setPayLoad(Directive.Payload payload) {
        this.mPayLoad = payload;
    }

}
