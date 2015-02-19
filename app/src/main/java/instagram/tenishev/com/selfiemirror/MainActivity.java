package instagram.tenishev.com.selfiemirror;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

public class MainActivity extends Activity implements OnFragmentInteractionListener {
    public static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        if (savedInstanceState == null) {
            if( InstaSession.get(this).hasAccessToken() ) {
                getFragmentManager().beginTransaction()
                        .add(R.id.container, UserInfoFragment.newInstance())
                        .commit();
            } else {
                getFragmentManager().beginTransaction()
                        .add(R.id.container, LoginFragment.newInstance())
                        .commit();
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(int command, Object object) {
        switch( command ) {
            case Constants.Command.ShowTaggedData:
            {
                final boolean shouldUseSelfData = (Boolean) object;
                getFragmentManager().beginTransaction()
                    .replace(R.id.container, SelfiesFragment.newInstance(shouldUseSelfData))
                    .addToBackStack("" + Constants.Command.ShowTaggedData)
                    .commit();
            }
            break;
            case Constants.Command.BackFromUserInfo:
            {
                // clear cookies
                CookieSyncManager.createInstance(this);
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.removeAllCookie();

                if( getFragmentManager().getBackStackEntryCount() > 0 ) {
                    if( Constants.D ) {
                        Log.i(TAG, "pop back from fragment stack");
                    }
                    getFragmentManager().popBackStack();
                } else {
                    getFragmentManager().beginTransaction()
                            .replace(R.id.container, LoginFragment.newInstance())
                            .commit();
                }
            }
            break;
            case Constants.Command.ShowUserInfo:
            {
                getFragmentManager().beginTransaction()
                        .replace(R.id.container, UserInfoFragment.newInstance())
                        .addToBackStack("" + Constants.Command.ShowUserInfo)
                        .commit();
            }
            break;
            default:
                throw new IllegalArgumentException("Command: " + command + " could not be processed!");
        }
    }
}
