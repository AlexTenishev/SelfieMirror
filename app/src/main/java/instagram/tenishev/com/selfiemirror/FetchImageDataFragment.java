package instagram.tenishev.com.selfiemirror;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FetchImageDataFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FetchImageDataFragment extends Fragment {
    private static final String TAG = FetchImageDataFragment.class.getSimpleName();

    public static final String TAG_ID = "fetch";

    private boolean     mShouldUseSelfData = true;
    private OnFragmentInteractionListener mListener;
    ArrayList<ImageItem> mItems = new ArrayList<ImageItem>();

    private static final String SHOULD_USE_SELF_STATE_PARAM = "mShouldUseSelfData";
    private static final String IMAGE_TYPE_INDEX = "low_resolution";
    private static final String FILTER_TAG = "#selfie";
    private static final int    MEDIA_CHUNK_LIMIT = 50;
    private SelfieImageListAdapter mAdapter;
    private Thread mWorkingThread;
    // FIXME: since we not using database here it is reasonable to limit the number of fetched items
    private static final int TOTAL_ITEMS_REASONABLE_LIMIT = 500;

    private class FetchImagesRunnable implements Runnable {

        private URL mNextUrl;

        public FetchImagesRunnable(final boolean shouldUseSelfData) {
            try {

                mNextUrl = new URL(
                        Constants.InstagrammApp.API_URL +
                        ( shouldUseSelfData ? Constants.InstagrammApp.MEDIA_URL : Constants.InstagrammApp.TAG_SELFIE_MEDIA ) +
                        "?access_token=" + InstaSession.get(getActivity()).getAccessToken() + "&count=" + MEDIA_CHUNK_LIMIT
                );
            } catch (MalformedURLException mue) {
                mue.printStackTrace();
            }
        }

        @Override
        public void run() {
            if( Constants.D ) {
                Log.i(TAG, "Starting fetch data");
            }
            boolean shouldContinueToRequestData = true;
            while( shouldContinueToRequestData ) {
                if( Constants.D ) {
                    Log.i(TAG, "Opening Token URL " + mNextUrl.toString());
                }
                try {
                    HttpURLConnection urlConnection = (HttpURLConnection) mNextUrl.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.setDoInput(true);
                    final String response = streamToString(urlConnection.getInputStream());
                    if( Constants.D ) {
                        Log.i(TAG, "response " + response);
                    }

                    JSONObject jsonObj = (JSONObject) new JSONTokener(response).nextValue();

                    //get operation status
                    final String code = jsonObj.getJSONObject("meta").getString("code");
                    if( Constants.D ) {
                        Log.i(TAG, "meta:code: " + code);
                    }
                    shouldContinueToRequestData = code.equals("200");
                    if( shouldContinueToRequestData ) {
                        JSONObject pagination = jsonObj.getJSONObject("pagination");
                        if( pagination.has("next_url") ) {
                            mNextUrl = new URL(pagination.getString("next_url"));
                            shouldContinueToRequestData = parseDataChunk(jsonObj.getJSONArray("data"));
                        } else {
                            shouldContinueToRequestData = false;
                            parseDataChunk(jsonObj.getJSONArray("data"));
                            if( Constants.D ) {
                                Log.i(TAG, "No more data. Finishing fetching json data");
                            }
                        }
                    }
                } catch(IOException ioe) {
                    ioe.printStackTrace();
                    shouldContinueToRequestData = false;
                } catch(JSONException je) {
                    je.printStackTrace();
                    shouldContinueToRequestData = false;
                }


            }


        }

        private boolean parseDataChunk(JSONArray data) {
            boolean result = true;
            ArrayList<ImageItem> items = new ArrayList<ImageItem>();
            try {
                for (int i = 0; i < data.length(); ++i) {
                    JSONObject item = data.getJSONObject(i);
                    if (item.getString("type").equals("image")) {
                        final String timestampStr = item.getString("created_time");
                        if( !item.isNull("caption") ) {
                            final String captionData = item.getJSONObject("caption").getString("text");
                            if (captionData.toLowerCase().contains(FILTER_TAG)) {
                                JSONObject images = item.getJSONObject("images");
                                JSONObject imageDesc = images.getJSONObject(IMAGE_TYPE_INDEX);

                                // get image data:
                                final String url = imageDesc.getString("url");

                                ImageItem imageItem = new ImageItem();
                                imageItem.url = url;
                                imageItem.timestamp = timestampStr;
                                imageItem.caption = captionData;
                                items.add(imageItem);
                            }
                        }
                    }
                }
            } catch( JSONException je) {
                je.printStackTrace();
                result = false;
            }

            if( result && items.size() > 0 ) {
                mItems.addAll(items);
                if( Constants.D ) {
                    Log.i(TAG, "add items chunk with " + items.size() + " elements");
                }
                if( mAdapter != null ) {
                    if( mAdapter.getCount() > 0 ) {
                        mAdapter.addData(items);
                    } else {
                        mAdapter.setData(mItems);
                    }
                }
                result &= mItems.size() <= TOTAL_ITEMS_REASONABLE_LIMIT;
            }

            return result;
        }

        private final String streamToString(final InputStream is) throws IOException {
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
    }

    public FetchImageDataFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param shouldUseSelfData whether or not fetch own data or public media data
     * @return A new instance of fragment FetchImageDataFragment.
     */
    public static FetchImageDataFragment newInstance(final boolean shouldUseSelfData) {
        FetchImageDataFragment fragment = new FetchImageDataFragment();
        Bundle args = new Bundle();
        args.putBoolean(SHOULD_USE_SELF_STATE_PARAM, shouldUseSelfData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mShouldUseSelfData = getArguments().getBoolean(SHOULD_USE_SELF_STATE_PARAM);
        }
        // Tell the framework to try to keep this fragment around
        // during a configuration change.
        setRetainInstance(true);
        mWorkingThread = new Thread(new FetchImagesRunnable(mShouldUseSelfData));
        mWorkingThread.start();
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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Fragment targetFragment = getTargetFragment();
        if( targetFragment != null ) {
            if( targetFragment.getView() != null ) {
                ListView lvSelfie = (ListView) targetFragment.getView().findViewById(R.id.selfie_list);
                if( lvSelfie != null && lvSelfie.getAdapter() instanceof  SelfieImageListAdapter ) {
                    mAdapter = (SelfieImageListAdapter) lvSelfie.getAdapter();
                    mAdapter.setData(mItems);
                }
            }
        } else {
            mAdapter = null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        setTargetFragment(null, -1);
    }
}
