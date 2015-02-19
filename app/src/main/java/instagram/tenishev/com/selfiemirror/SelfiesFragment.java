package instagram.tenishev.com.selfiemirror;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
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
 * SelfiesFragment class
 *
 * It is the heart of the functionality. All selfies would be shown here
 */
public class SelfiesFragment extends Fragment implements AdapterView.OnItemClickListener {
    private final static String TAG = SelfiesFragment.class.getSimpleName();

    private OnFragmentInteractionListener mListener;
    private ListView    lvSelfie;
    private ImageView   bigImage;
    private boolean     mShouldUseSelfData = true;

    private static final String SHOULD_USE_SELF_STATE_PARAM = "mShouldUseSelfData";
    private static final String IMAGE_TYPE_INDEX = "low_resolution";
    private static final String FILTER_TAG = "#selfie";
    private static final int    MEDIA_CHUNK_LIMIT = 50;
    private Thread mWorkingThread;

    // FIXME: since we not using database here it is reasonable to limit the number of fetched items
    private static final int TOTAL_ITEMS_REASONABLE_LIMIT = 500;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SelfiesFragment.
     */
    public static SelfiesFragment newInstance(final boolean shouldUseSelfData) {
        SelfiesFragment fragment = new SelfiesFragment();
        Bundle args = new Bundle();
        args.putBoolean(SHOULD_USE_SELF_STATE_PARAM, shouldUseSelfData);
        fragment.setArguments(args);
        return fragment;
    }

    public SelfiesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if( savedInstanceState != null ) {
            mShouldUseSelfData = savedInstanceState.getBoolean(SHOULD_USE_SELF_STATE_PARAM);
        } else if (getArguments() != null) {
            mShouldUseSelfData = getArguments().getBoolean(SHOULD_USE_SELF_STATE_PARAM);
        }
        mWorkingThread = new Thread(new FetchImagesRunnable(mShouldUseSelfData));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(SHOULD_USE_SELF_STATE_PARAM, mShouldUseSelfData);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.selfies_fragment, container, false);

        lvSelfie = (ListView) rootView.findViewById(R.id.selfie_list);
        bigImage = (ImageView) rootView.findViewById(R.id.selfie_image);

        bigImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bigImage.setVisibility(View.INVISIBLE);
            }
        });

        final SelfieImageListAdapter adapter = new SelfieImageListAdapter(getActivity(), new ArrayList<ImageItem>());
        lvSelfie.setAdapter(adapter);
        lvSelfie.setOnItemClickListener(this);

        if( !mWorkingThread.isAlive() ) {
            mWorkingThread.start();
        }
        return rootView;
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
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if( view.getTag() != null ) {
            ImageViewHolder vh = (ImageViewHolder) view.getTag();
            final Bitmap bitmap = vh.image.getDrawable() instanceof BitmapDrawable ? ((BitmapDrawable)vh.image.getDrawable()).getBitmap() : null;
            if( bitmap != null ) {
                bigImage.setImageBitmap(bitmap);
                bigImage.setVisibility(View.VISIBLE);
            }
        }
    }

    private class FetchImagesRunnable implements Runnable {

        private URL mNextUrl;

        public FetchImagesRunnable(final boolean shouldUseSelfData) {
            try {

                mNextUrl = new URL(
                    Constants.InstagrammApp.API_URL +
                    ( shouldUseSelfData ? Constants.InstagrammApp.MEDIA_URL : Constants.InstagrammApp.TAG_SELFIE_MEDIA ) +
                    "?access_token=" + InstaSession.get(SelfiesFragment.this.getActivity()).getAccessToken() + "&count=" + MEDIA_CHUNK_LIMIT
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
                if( Constants.D ) {
                    Log.i(TAG, "add items chunk with " + items.size() + " elements");
                }
                if( lvSelfie.getAdapter() instanceof SelfieImageListAdapter ) {
                    ((SelfieImageListAdapter)lvSelfie.getAdapter()).addData(items);
                    result &= lvSelfie.getAdapter().getCount() <= TOTAL_ITEMS_REASONABLE_LIMIT;
                }
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
}
