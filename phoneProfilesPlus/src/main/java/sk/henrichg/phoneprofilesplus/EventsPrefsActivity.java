package sk.henrichg.phoneprofilesplus;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

public class EventsPrefsActivity extends AppCompatActivity {

    private int resultCode = RESULT_CANCELED;

    boolean showSaveMenu = false;

    private Toolbar toolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // must by called before super.onCreate() for PreferenceActivity
        GlobalGUIRoutines.setTheme(this, false, true/*, false*/);
        GlobalGUIRoutines.setLanguage(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preferences);

        toolbar = findViewById(R.id.activity_preferences_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setElevation(GlobalGUIRoutines.dpToPx(1));
        }

        EventsPrefsFragment preferenceFragment = new EventsPrefsActivity.EventsPrefsRoot();

        if (savedInstanceState == null) {
            //loadPreferences(newProfileMode, predefinedProfileIndex);

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.activity_preferences_settings, preferenceFragment)
                    .commit();
        }
        else {
            profile_id = savedInstanceState.getLong("profile_id", 0);
            newProfileMode = savedInstanceState.getInt("newProfileMode", EditorProfileListFragment.EDIT_MODE_UNDEFINED);
            predefinedProfileIndex = savedInstanceState.getInt("predefinedProfileIndex", 0);

            showSaveMenu = savedInstanceState.getBoolean("showSaveMenu", false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        if (showSaveMenu) {
            // for shared profile is not needed, for shared profile is used PPApplication.SHARED_PROFILE_PREFS_NAME
            // and this is used in Profile.getSharedProfile()
            //if (profile_id != Profile.SHARED_PROFILE_ID) {
            toolbar.inflateMenu(R.menu.profile_preferences_save);
            //}
        }
        else {
            // no menu for shared profile
            //if (profile_id != Profile.SHARED_PROFILE_ID) {
            toolbar.inflateMenu(R.menu.profile_preferences);
            //}
        }
        return true;
    }

    private static void onNextLayout(final View view, final Runnable runnable) {
        final ViewTreeObserver observer = view.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                final ViewTreeObserver trueObserver;

                if (observer.isAlive()) {
                    trueObserver = observer;
                } else {
                    trueObserver = view.getViewTreeObserver();
                }

                trueObserver.removeOnGlobalLayoutListener(this);

                runnable.run();
            }
        });
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean ret = super.onPrepareOptionsMenu(menu);

        //if (profile_id != Profile.SHARED_PROFILE_ID) {
        // no menu for shared profile

        onNextLayout(toolbar, new Runnable() {
            @Override
            public void run() {
                showTargetHelps();
            }
        });
        //}

        /*final Handler handler = new Handler(getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showTargetHelps();
            }
        }, 1000);*/

        return ret;
    }

    private void finishActivity() {
        if (showSaveMenu) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.not_saved_changes_alert_title);
            dialogBuilder.setMessage(R.string.not_saved_changes_alert_message);
            dialogBuilder.setPositiveButton(R.string.alert_button_yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    savePreferences(newProfileMode, predefinedProfileIndex);
                    resultCode = RESULT_OK;
                    finish();
                }
            });
            dialogBuilder.setNegativeButton(R.string.alert_button_no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            AlertDialog dialog = dialogBuilder.create();
                /*dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        Button positive = ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                        if (positive != null) positive.setAllCaps(false);
                        Button negative = ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                        if (negative != null) negative.setAllCaps(false);
                    }
                });*/
            if (!isFinishing())
                dialog.show();
        }
        else
            finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (getSupportFragmentManager().getBackStackEntryCount() == 0)
                    finishActivity();
                else
                    getSupportFragmentManager().popBackStack();
                return true;
            case R.id.profile_preferences_save:
                savePreferences(newProfileMode, predefinedProfileIndex);
                resultCode = RESULT_OK;
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.activity_preferences_settings);
        if (fragment != null)
            ((ProfilesPrefsFragment)fragment).doOnActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0)
            finishActivity();
        else
            super.onBackPressed();
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putLong("profile_id", profile_id);
        savedInstanceState.putInt("newProfileMode", newProfileMode);
        savedInstanceState.putInt("predefinedProfileIndex", predefinedProfileIndex);

        savedInstanceState.putBoolean("showSaveMenu", showSaveMenu);
    }

    @Override
    public void finish() {
        // for startActivityForResult
        Intent returnIntent = new Intent();
        returnIntent.putExtra(PPApplication.EXTRA_PROFILE_ID, profile_id);
        returnIntent.putExtra(EditorProfilesActivity.EXTRA_NEW_PROFILE_MODE, newProfileMode);
        returnIntent.putExtra(EditorProfilesActivity.EXTRA_PREDEFINED_PROFILE_INDEX, predefinedProfileIndex);
        returnIntent.putExtra(PhoneProfilesPrefsActivity.EXTRA_RESET_EDITOR, sk.henrichg.phoneprofilesplus.Permissions.grantRootChanged);
        sk.henrichg.phoneprofilesplus.Permissions.grantRootChanged = false;
        setResult(resultCode,returnIntent);

        super.finish();
    }

    private void showTargetHelps() {

    }

//--------------------------------------------------------------------------------------------------

    static public class EventsPrefsRoot extends EventsPrefsFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.event_prefs_root, rootKey);
        }
    }

    static public class EventsPrefsStartOfEventsOthers extends EventsPrefsFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.event_prefs_start_of_event_others, rootKey);
        }
    }

    static public class EventsPrefsEndOfEventsOthers extends EventsPrefsFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.event_prefs_end_of_event_others, rootKey);
        }
    }

    static public class EventsPrefsTimeParameters extends EventsPrefsFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.event_prefs_time_sensor, rootKey);
        }
    }

    static public class EventsPrefsCalendarParameters extends EventsPrefsFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.event_prefs_calendar_sensor, rootKey);
        }
    }

    static public class EventsPrefsBatteryParameters extends EventsPrefsFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.event_prefs_battery_sensor, rootKey);
        }
    }

    static public class EventsPrefsCallParameters extends EventsPrefsFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.event_prefs_call_sensor, rootKey);
        }
    }

    static public class EventsPrefsSMSParameters extends EventsPrefsFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.event_prefs_sms_mms_sensor, rootKey);
        }
    }

    static public class EventsPrefsRadioSwitchParameters extends EventsPrefsFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.event_prefs_radio_switch_sensor, rootKey);
        }
    }

    static public class EventsPrefsLocationParameters extends EventsPrefsFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.event_prefs_location_sensor, rootKey);
        }
    }

    static public class EventsPrefsWifiParameters extends EventsPrefsFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.event_prefs_wifi_sensor, rootKey);
        }
    }

    static public class EventsPrefsBluetoothParameters extends EventsPrefsFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.event_prefs_bluetooth_sensor, rootKey);
        }
    }

    static public class EventsPrefsMobileCellsParameters extends EventsPrefsFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.event_prefs_mobile_cells_sensor, rootKey);
        }
    }

    static public class EventsPrefsAccessoriesParameters extends EventsPrefsFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.event_prefs_accessories_sensor, rootKey);
        }
    }

    static public class EventsPrefsScreenParameters extends EventsPrefsFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.event_prefs_screen_sensor, rootKey);
        }
    }

    static public class EventsPrefsNotificationsParameters extends EventsPrefsFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.event_prefs_notification_sensor, rootKey);
        }
    }

    static public class EventsPrefsApplicationsParameters extends EventsPrefsFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.event_prefs_application_sensor, rootKey);
        }
    }

    static public class EventsPrefsOrientationParameters extends EventsPrefsFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.event_prefs_orientation_sensor, rootKey);
        }
    }

    static public class EventsPrefsNFCParameters extends EventsPrefsFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.event_prefs_nfc_sensor, rootKey);
        }
    }

    static public class EventsPrefsAlarmClockParameters extends EventsPrefsFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.event_prefs_alarm_clock_sensor, rootKey);
        }
    }

}
