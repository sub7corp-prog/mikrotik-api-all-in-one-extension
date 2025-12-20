package android.app;

public class Activity {
    public void runOnUiThread(Runnable action) {
        if (action != null) {
            action.run();
        }
    }
}
