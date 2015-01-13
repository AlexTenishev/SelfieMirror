package instagram.tenishev.com.selfiemirror;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * InstaSession class
 *
 * responsible for caching instagram user data
 */
public class InstaSession {

    private static final String SHARED = "instaprefs";

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    private static final String API_USERNAME = "username";
    private static final String API_ID = "id";
    private static final String API_NAME = "name";
    private static final String API_ACCESS_TOKEN = "access_token";

    private InstaSession(Context context) {
        sharedPref = context.getSharedPreferences(SHARED, Context.MODE_PRIVATE);
        editor = sharedPref.edit();
    }

    private static InstaSession sInstance = null;
    private static final Object sInstanceLock = new Object();

    public synchronized static InstaSession get(Context context) {
        if (sInstance == null) {
            synchronized (sInstanceLock) {
                if (sInstance == null) {
                    sInstance = new InstaSession(context);
                }
            }
        }
        return sInstance;
    }

    public String getAccessToken() {
        return sharedPref.getString(API_ACCESS_TOKEN, null);
    }
    public void setAccessToken(final String accessToken) {
        editor.putString(API_ACCESS_TOKEN, accessToken);
        editor.commit();
    }

    /**
     * Reset access token and user name
     */
    public void resetAccessToken() {
        editor.putString(API_ID, null);
        editor.putString(API_NAME, null);
        editor.putString(API_ACCESS_TOKEN, null);
        editor.putString(API_USERNAME, null);
        editor.commit();
    }

    public String getUsername() {
        return sharedPref.getString(API_USERNAME, null);
    }
    public void setUsername(final String username) {
        editor.putString(API_USERNAME, username);
        editor.commit();
    }

    public String getUserId() {
        return sharedPref.getString(API_ID, null);
    }
    public void setUserId(final String userId) {
        editor.putString(API_ID, userId);
        editor.commit();
    }

    public String getName() {
        return sharedPref.getString(API_NAME, null);
    }
    public void setName(final String name) {
        editor.putString(API_NAME, name);
        editor.commit();
    }

    public boolean hasAccessToken() {
        return ( getAccessToken() == null ) ? false : true;
    }
}
