package instagram.tenishev.com.selfiemirror;


import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * UserInfoFragment class
 * This is sub-segue fragment to main functionality
 */
public class UserInfoFragment extends Fragment implements View.OnClickListener {

    private final String TAG = UserInfoFragment.class.getSimpleName();

    private OnFragmentInteractionListener mListener;
    private View        infoView;
    private TextView    tvUserId;
    private TextView    tvUserName;
    private TextView    tvUserFullname;
    private RadioGroup  requestTypeGroup;
    private ProgressBar progressBar;
    private Button      btnNext;

    private Runnable mGetUserInfoRunnable = new Runnable() {
        @Override
        public void run() {
            if( Constants.D ) {
                Log.i(TAG, "Getting access token");
            }
            try {
                URL url = new URL(Constants.InstagrammApp.API_URL + Constants.InstagrammApp.USER_INFO + "?access_token=" + InstaSession.get(UserInfoFragment.this.getActivity()).getAccessToken());
                if( Constants.D ) {
                    Log.i(TAG, "Opening Token URL " + url.toString());
                }
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setDoInput(true);
                String response = streamToString(urlConnection.getInputStream());
                if( Constants.D ) {
                    Log.i(TAG, "response " + response);
                }
                JSONObject jsonObj = (JSONObject) new JSONTokener(response).nextValue();

                String id = jsonObj.getJSONObject("data").getString("id");
                String user = jsonObj.getJSONObject("data").getString("username");
                String name = jsonObj.getJSONObject("data").getString("full_name");

                final InstaSession session = InstaSession.get(getActivity());
                session.setUserId(id);
                session.setName(user);
                session.setUsername(name);

//                UserInfoFragment.this.
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvUserId.setText("" + InstaSession.get(getActivity()).getUserId());
                        tvUserName.setText("" + InstaSession.get(getActivity()).getName());
                        tvUserFullname.setText("" + InstaSession.get(getActivity()).getUsername());
                        progressBar.setVisibility(View.INVISIBLE);
                        infoView.setVisibility(View.VISIBLE);
                    }
                });

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private String streamToString(InputStream is) throws IOException {
            String str = "";

            if (is != null) {
                StringBuilder sb = new StringBuilder();
                String line;

                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(is));

                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }

                    reader.close();
                } finally {
                    is.close();
                }

                str = sb.toString();
            }

            return str;
        }
    };
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment UserInfoFragment.
     */
    public static UserInfoFragment newInstance() {
        UserInfoFragment fragment = new UserInfoFragment();
        return fragment;
    }

    public UserInfoFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.user_info_fragment, container, false);
        tvUserId = (TextView) rootView.findViewById(R.id.user_id_val);
        tvUserName = (TextView) rootView.findViewById(R.id.user_name_val);
        tvUserFullname  = (TextView) rootView.findViewById(R.id.user_fullname_val);

        tvUserId.setText("" + InstaSession.get(getActivity()).getUserId());
        tvUserName.setText("" + InstaSession.get(getActivity()).getName());
        tvUserFullname.setText("" + InstaSession.get(getActivity()).getUsername());

        btnNext = (Button) rootView.findViewById(R.id.next);
        btnNext.setOnClickListener(this);
        progressBar = (ProgressBar) rootView.findViewById(R.id.progress);
        progressBar.setVisibility(View.VISIBLE);

        requestTypeGroup = (RadioGroup) rootView.findViewById(R.id.request_type);

        infoView = rootView.findViewById(R.id.info);

        new Thread(mGetUserInfoRunnable).start();

        rootView.setFocusableInTouchMode(true);
        rootView.requestFocus();
        rootView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.i(TAG, "keyCode: " + keyCode);
                if( keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP ) {
                    if( Constants.D ) {
                        Log.i(TAG, "onKey Back pressed, stack size is: " + getFragmentManager().getBackStackEntryCount());
                    }

                    if (mListener != null) {
                        mListener.onFragmentInteraction(Constants.Command.BackFromUserInfo, null);
                    }
                    return true;
                } else {
                    return false;
                }
            }
        });

        return rootView;
    }


    @Override
    public void onClick(View view) {
        if( view.getId() == R.id.next ) {
            if( mListener != null ) {
                mListener.onFragmentInteraction(Constants.Command.ShowTaggedData, new Boolean(requestTypeGroup.getCheckedRadioButtonId() == R.id.request_type_self));
            }
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
