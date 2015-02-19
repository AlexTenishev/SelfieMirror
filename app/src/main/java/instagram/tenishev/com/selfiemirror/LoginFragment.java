package instagram.tenishev.com.selfiemirror;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * LoginFragment class
 * here all about login logic and moving further
 */
public class LoginFragment extends Fragment {

    private static final String TAG = LoginFragment.class.getSimpleName();

    private OnFragmentInteractionListener mListener;
    private WebView mLoginWebView;
    private ProgressBar mProgressBar;
    private final String mAuthUrl;

    /**
     * Use this factory method to create a new instance of this fragment
     *
     * @return A new instance of fragment LoginFragment
     */
    public static LoginFragment newInstance() {
        LoginFragment fragment = new LoginFragment();
        return fragment;
    }

    public LoginFragment() {
        // Required empty public constructor
        mAuthUrl = Constants.InstagrammApp.AUTH_URL +
                "?client_id=" + Constants.InstagrammApp.CLIENT_ID +
                "&redirect_uri=" + Constants.InstagrammApp.CALLBACK_URL +
                "&response_type=token";
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.login_fragment, container, false);
        mLoginWebView = (WebView) rootView.findViewById(R.id.loginWebView);


        CookieSyncManager.createInstance(getActivity());
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        mLoginWebView.setWebViewClient(new OAuthWebViewClient());
        mLoginWebView.getSettings().setJavaScriptEnabled(true);
        mLoginWebView.loadUrl(mAuthUrl);

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.uri_progress);
        mProgressBar.setVisibility(View.VISIBLE);

        return rootView;
    }

    private class OAuthWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            String decodedCallbackUri = null;
            try {
                decodedCallbackUri = URLDecoder.decode(Constants.InstagrammApp.CALLBACK_URL, Constants.UTF8);
            } catch(UnsupportedEncodingException e) {
                decodedCallbackUri = null;
            }
            if (url.startsWith(Constants.InstagrammApp.CALLBACK_URL)
                    || ( decodedCallbackUri != null && url.startsWith(decodedCallbackUri) ) ) {
                String urls[] = url.split("=");

                final String mAccessToken = urls[1];
                String tokenParts[] = mAccessToken != null ? mAccessToken.split("\\.") : null;
                final String userId = (tokenParts != null && tokenParts.length > 0 ) ? tokenParts[0] : null;
                InstaSession.get(getActivity()).setAccessToken(mAccessToken);
                InstaSession.get(getActivity()).setUserId(userId);

                if (mListener != null) {
                    mListener.onFragmentInteraction(Constants.Command.ShowUserInfo, null);
                }
                return true;
            }
            return false;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                                    String description, String failingUrl) {
            if( Constants.D ) {
                Log.d(TAG, "Page error: " + description + ", errorCode: " + errorCode + ", failingUrl: " + failingUrl);
            }

            // TODO: add error view describing what happend
            super.onReceivedError(view, errorCode, description, failingUrl);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if( Constants.D ) {
                Log.d(TAG, "Loading URL: " + url);
            }

            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if( Constants.D ) {
                Log.d(TAG, "onPageFinished URL: " + url);
            }
            mProgressBar.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
