package instagram.tenishev.com.selfiemirror;

import android.app.Activity;
import android.app.FragmentManager;
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
    private FetchImageDataFragment mFetchImageDataFrag;

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
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(SHOULD_USE_SELF_STATE_PARAM, mShouldUseSelfData);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentManager fm = getActivity().getFragmentManager();
        // Check to see if we have retained the worker fragment.
        mFetchImageDataFrag = (FetchImageDataFragment) fm.findFragmentByTag(FetchImageDataFragment.TAG_ID);
        // If not retained (or first time running), we need to create it.
        if( mFetchImageDataFrag == null ) {
            mFetchImageDataFrag = FetchImageDataFragment.newInstance(mShouldUseSelfData);
            // Tell it who it is working with.
            mFetchImageDataFrag.setTargetFragment(this, 0);
            fm.beginTransaction().add(mFetchImageDataFrag, FetchImageDataFragment.TAG_ID).commit();
        } else {
            mFetchImageDataFrag.setTargetFragment(this, 0);
        }
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

        rootView.setFocusableInTouchMode(true);
        rootView.requestFocus();
        rootView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if( Constants.D ) {
                    Log.i(TAG, "keyCode: " + keyCode);
                }
                if( keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP ) {
                    if( Constants.D ) {
                        Log.i(TAG, "onKey Back pressed, stack size is: " + getFragmentManager().getBackStackEntryCount());
                    }
                    // here we should destroy retained fragment
                    // Check to see if we have retained the worker fragment.
                    if( mFetchImageDataFrag != null ) {
                        FragmentManager fm = getActivity().getFragmentManager();
                        fm.beginTransaction().remove(mFetchImageDataFrag).commit();
                        mFetchImageDataFrag = null;
                    }
                }
                return false;
            }
        });
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
        if( mFetchImageDataFrag != null ) {
            mFetchImageDataFrag = null;
        }
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
}
