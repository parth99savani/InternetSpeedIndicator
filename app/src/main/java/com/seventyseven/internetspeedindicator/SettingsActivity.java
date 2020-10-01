package com.seventyseven.internetspeedindicator;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.seventyseven.internetspeedindicator.Service.IndicatorServiceHelper;
import com.seventyseven.internetspeedindicator.Utils.Settings;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private SharedPreferences mSharedPref;
        private Context mContext;
        private Preference btnRateApp,btnShareApp,btnAbout,btnPolicy;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            mContext = getActivity();

            mSharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);

            if (mSharedPref.getBoolean(Settings.KEY_INDICATOR_ENABLED, true)) {
                startIndicatorService();
            }

            mSharedPref.edit().putBoolean(Settings.PERMISSION_ALLOWED,true).commit();

            btnRateApp = (Preference) findPreference("btnRateApp");
            btnShareApp = (Preference) findPreference("btnShareApp");
            btnAbout = (Preference) findPreference("btnAbout");
            btnPolicy = (Preference) findPreference("btnPolicy");

            btnRateApp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=" + mContext.getPackageName())));
                    } catch (ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=" + mContext.getPackageName())));
                    }
                    return true;
                }
            });

            btnShareApp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent a = new Intent(Intent.ACTION_SEND);

                    //this is to get the app link in the playstore without launching your app.
                    final String appPackageName = mContext.getPackageName();
                    String strAppLink = "";

                    try {
                        strAppLink = "https://play.google.com/store/apps/details?id=" + appPackageName;
                    } catch (ActivityNotFoundException anfe) {
                        strAppLink = "https://play.google.com/store/apps/details?id=" + appPackageName;
                    }
                    // this is the sharing part
                    a.setType("text/link");
                    String shareBody = "KB/s - Internet Speed Indicator\n\nHey! Download this app for free & Show internet speed notification." +
                            "\n" + "" + strAppLink;
                    String shareSub = "KB/s - Internet Speed Indicator";
                    a.putExtra(Intent.EXTRA_SUBJECT, shareSub);
                    a.putExtra(Intent.EXTRA_TEXT, shareBody);
                    startActivity(Intent.createChooser(a, "Share 'KB/s - Internet Speed Indicator' Using"));
                    return true;
                }
            });

            btnAbout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(getActivity(),AboutActivity.class));
                    return true;
                }
            });

            btnPolicy.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mContext.getString(R.string.policy))));
                    return true;
                }
            });

        }

        private final SharedPreferences.OnSharedPreferenceChangeListener mSettingsListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(Settings.KEY_INDICATOR_ENABLED)) {
                    if (mSharedPref.getBoolean(Settings.KEY_INDICATOR_ENABLED, true)) {
                        startIndicatorService();
                    } else {
                        stopIndicatorService();
                    }
                } else if (!key.equals(Settings.KEY_START_ON_BOOT)
                        && !key.equals(Settings.KEY_INDICATOR_STARTED)) {
                    startIndicatorService();
                }
            }
        };

        @Override
        public void onResume() {
            super.onResume();
            mSharedPref.registerOnSharedPreferenceChangeListener(mSettingsListener);
        }

        @Override
        public void onPause() {
            super.onPause();
            mSharedPref.unregisterOnSharedPreferenceChangeListener(mSettingsListener);
        }

        private void startIndicatorService() {
            IndicatorServiceHelper.startService(mContext);
        }

        private void stopIndicatorService() {
            IndicatorServiceHelper.stopService(mContext);
        }
    }
}