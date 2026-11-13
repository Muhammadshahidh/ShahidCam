package com.shahid.cam.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.shahid.cam.Constants;
import com.shahid.cam.SharedPreferencesManager;
import com.shahid.cam.shahidrec.ui.ShahidRecHomeFragment;
import com.shahid.cam.forensics.ui.ForensicIntelligenceFragment;

/**
 * ViewPagerAdapter manages fragments for the main ViewPager.
 * Creates appropriate HomeFragment based on current recording mode (ShahidCam/ShahidRec).
 */
public class ViewPagerAdapter extends FragmentStateAdapter {

    private final FragmentActivity fragmentActivity;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        this.fragmentActivity = fragmentActivity;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                // Home tab - check current mode and create appropriate fragment
                return createHomeFragment();
            case 1:
                return new RecordsFragment();
            case 2:
                return new RemoteFragment();
            case 3:
                return new ShahidEditorMiniFragment();
            case 4:
                // Phase 1: Use new SettingsHomeFragment (legacy fragment accessible from inside)
                return new SettingsHomeFragment();
            case 5:
                return new ForensicIntelligenceFragment();
            default:
                return new HomeFragment();
        }
    }

    /**
     * Creates the appropriate HomeFragment based on current recording mode.
     * @return HomeFragment or ShahidRecHomeFragment depending on mode
     */
    private Fragment createHomeFragment() {
        SharedPreferencesManager prefsManager = 
            SharedPreferencesManager.getInstance(fragmentActivity);
        
        String currentMode = prefsManager.getCurrentRecordingMode();
        
        if (Constants.MODE_SHAHIDREC.equals(currentMode)) {
            // ShahidRec mode - create screen recording fragment
            return ShahidRecHomeFragment.newInstance();
        } else {
            // ShahidCam mode (default) - create camera recording fragment
            return new HomeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 6; // Home, Records, Remote, ShahidEditor Mini, Settings, Lab
    }
}
