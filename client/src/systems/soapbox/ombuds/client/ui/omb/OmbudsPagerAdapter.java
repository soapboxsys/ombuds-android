package systems.soapbox.ombuds.client.ui.omb;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;

import systems.soapbox.ombuds.client_test.R;

/**
 * Created by askuck on 12/22/15.
 */
public class OmbudsPagerAdapter extends FragmentPagerAdapter {

    public static final int NUM_ITEMS = 3;
    public static final int ALL_POS = 0;
    public static final int WATCHING_POS = 1;
    public static final int PROFILE_POS = 2;

    private Context context;

    public OmbudsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case ALL_POS:
                return AllFragment.newInstance();
            case WATCHING_POS:
                return WatchingFragment.newInstance();
            case PROFILE_POS:
                return ProfileFragment.newInstance();
            default:
                return null;
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case ALL_POS:
                return context.getString(R.string.all_frag);
            case WATCHING_POS:
                return context.getString(R.string.watching_frag);
            case PROFILE_POS:
                return context.getString(R.string.profile_frag);
            default:
                return "";
        }
    }

    @Override
    public int getCount() {
        return NUM_ITEMS;
    }

}
