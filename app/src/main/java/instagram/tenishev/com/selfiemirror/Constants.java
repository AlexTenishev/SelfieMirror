package instagram.tenishev.com.selfiemirror;

/**
 * Constants class
 *
 * Here all the constants that we will use globally
 */
public class Constants {

    public static final boolean D = true;

    public static class InstagrammApp {
        public static final String CLIENT_ID = "8f41ea22b6304077a75c996eeedf284f";
        public static final String CALLBACK_URL = "selfiemirror://authorize";

        public static final String AUTH_URL = "https://instagram.com/oauth/authorize/";
        public static final String API_URL = "https://api.instagram.com/v1";
        public static final String USER_INFO = "/users/self";
        public static final String TAG_SELFIE_MEDIA = "/tags/selfie/media/recent";
        public static final String MEDIA_URL = "/users/self/media/recent/";
    }

    public static final String UTF8 = "UTF-8";
}
