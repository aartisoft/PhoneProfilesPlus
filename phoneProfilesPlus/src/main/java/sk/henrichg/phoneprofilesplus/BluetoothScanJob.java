package sk.henrichg.phoneprofilesplus;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

class BluetoothScanJob extends Job {

    static final String JOB_TAG  = "BluetoothScanJob";
    static final String JOB_TAG_SHORT  = "BluetoothScanJob_short";

    public static BluetoothAdapter bluetooth = null;

    private static List<BluetoothDeviceData> tmpScanLEResults = null;

    private static final String PREF_EVENT_BLUETOOTH_SCAN_REQUEST = "eventBluetoothScanRequest";
    private static final String PREF_EVENT_BLUETOOTH_WAIT_FOR_RESULTS = "eventBluetoothWaitForResults";
    private static final String PREF_EVENT_BLUETOOTH_LE_SCAN_REQUEST = "eventBluetoothLEScanRequest";
    private static final String PREF_EVENT_BLUETOOTH_WAIT_FOR_LE_RESULTS = "eventBluetoothWaitForLEResults";
    private static final String PREF_EVENT_BLUETOOTH_ENABLED_FOR_SCAN = "eventBluetoothEnabledForScan";


    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        PPApplication.logE("BluetoothScanJob.onRunJob", "xxx");

        Context context = getContext();

        CallsCounter.logCounter(context, "BluetoothScanJob.onRunJob", "BluetoothScanJob_onRunJob");

        if (Event.isEventPreferenceAllowed(EventPreferencesBluetooth.PREF_EVENT_BLUETOOTH_ENABLED, context) !=
                PPApplication.PREFERENCE_ALLOWED) {
            BluetoothScanJob.cancelJob(context);
            return Result.SUCCESS;
        }

        boolean isPowerSaveMode = PPApplication.isPowerSaveMode;
        if (isPowerSaveMode && ApplicationPreferences.applicationEventLocationUpdateInPowerSaveMode(context).equals("2")) {
            PPApplication.logE("BluetoothScanJob.onRunJob", "update in power save mode is not allowed = cancel job");
            BluetoothScanJob.cancelJob(context);
            //removeAlarm(context/*, false*/);
            //removeAlarm(context/*, true*/);
            return Result.SUCCESS;
        }

        if (bluetooth == null)
            bluetooth = getBluetoothAdapter(context);

        if (Event.getGlobalEventsRunning(context))
        {
            startScanner(context, false);
        }

        BluetoothScanJob.scheduleJob(context, false, false);

        return Result.SUCCESS;
    }

    static void scheduleJob(Context context, boolean shortInterval, boolean forScreenOn) {
        PPApplication.logE("BluetoothScanJob.scheduleJob", "shortInterval="+shortInterval);

        if (Event.isEventPreferenceAllowed(EventPreferencesBluetooth.PREF_EVENT_BLUETOOTH_ENABLED, context)
                == PPApplication.PREFERENCE_ALLOWED) {

            JobManager jobManager = null;
            try {
                jobManager = JobManager.instance();
            } catch (Exception ignored) { }

            if (jobManager != null) {
                JobRequest.Builder jobBuilder;
                if (!shortInterval) {
                    jobManager.cancelAllForTag(JOB_TAG_SHORT);

                    int interval = ApplicationPreferences.applicationEventBluetoothScanInterval(context);
                    boolean isPowerSaveMode = PPApplication.isPowerSaveMode;
                    if (isPowerSaveMode && ApplicationPreferences.applicationEventBluetoothScanInPowerSaveMode(context).equals("1"))
                        interval = 2 * interval;

                    jobBuilder = new JobRequest.Builder(JOB_TAG);

                    if (TimeUnit.MINUTES.toMillis(interval) < JobRequest.MIN_INTERVAL) {
                        jobManager.cancelAllForTag(JOB_TAG);
                        jobBuilder.setExact(TimeUnit.MINUTES.toMillis(interval));
                    } else {
                        int requestsForTagSize = jobManager.getAllJobRequestsForTag(JOB_TAG).size();
                        PPApplication.logE("BluetoothScanJob.scheduleJob", "requestsForTagSize=" + requestsForTagSize);
                        if (requestsForTagSize == 0) {
                            if (TimeUnit.MINUTES.toMillis(interval) < JobRequest.MIN_INTERVAL)
                                jobBuilder.setPeriodic(JobRequest.MIN_INTERVAL);
                            else
                                jobBuilder.setPeriodic(TimeUnit.MINUTES.toMillis(interval));
                        } else
                            return;
                    }
                } else {
                    cancelJob(context);
                    jobBuilder = new JobRequest.Builder(JOB_TAG_SHORT);
                    if (forScreenOn)
                        jobBuilder.setExact(TimeUnit.SECONDS.toMillis(5));
                    else
                        jobBuilder.setExact(TimeUnit.SECONDS.toMillis(5));
                }

                PPApplication.logE("BluetoothScanJob.scheduleJob", "build and schedule");

                jobBuilder
                        .setUpdateCurrent(false) // don't update current, it would cancel this currently running job
                        .build()
                        .schedule();
            }
        }
        else
            PPApplication.logE("BluetoothScanJob.scheduleJob","BluetoothHardware=false");
    }

    static void cancelJob(Context context) {
        PPApplication.logE("BluetoothScanJob.cancelJob", "xxx");

        BluetoothScanJob.setScanRequest(context, false);
        BluetoothScanJob.setWaitForResults(context, false);
        BluetoothScanJob.setLEScanRequest(context, false);
        BluetoothScanJob.setWaitForLEResults(context, false);
        BluetoothScanJob.setBluetoothEnabledForScan(context, false);
        Scanner.setForceOneBluetoothScan(context, Scanner.FORCE_ONE_SCAN_DISABLED);
        Scanner.setForceOneLEBluetoothScan(context, Scanner.FORCE_ONE_SCAN_DISABLED);

        try {
            JobManager jobManager = JobManager.instance();
            jobManager.cancelAllForTag(JOB_TAG_SHORT);
            jobManager.cancelAllForTag(JOB_TAG);
        } catch (Exception ignored) {}
    }

    static boolean isJobScheduled() {
        PPApplication.logE("BluetoothScanJob.isJobScheduled", "xxx");

        try {
            JobManager jobManager = JobManager.instance();
            return (jobManager.getAllJobRequestsForTag(JOB_TAG).size() != 0) ||
                    (jobManager.getAllJobRequestsForTag(JOB_TAG_SHORT).size() != 0);
        } catch (Exception e) {
            return  false;
        }
    }

    //------------------------------------------------------------

    static BluetoothAdapter getBluetoothAdapter(Context context) {
        BluetoothAdapter adapter;
        if (android.os.Build.VERSION.SDK_INT < 18)
            adapter = BluetoothAdapter.getDefaultAdapter();
        else {
            BluetoothManager bluetoothManager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
            adapter = bluetoothManager.getAdapter();
        }
        return adapter;
    }

    public static void initialize(Context context)
    {
        setScanRequest(context, false);
        setLEScanRequest(context, false);
        setWaitForResults(context, false);
        setWaitForLEResults(context, false);
        setBluetoothEnabledForScan(context, false);

        if (Event.isEventPreferenceAllowed(EventPreferencesBluetooth.PREF_EVENT_BLUETOOTH_ENABLED, context) !=
                PPApplication.PREFERENCE_ALLOWED)
            return;

        if (bluetooth == null)
            bluetooth = getBluetoothAdapter(context);
        if (bluetooth == null)
            return;

        clearScanResults(context);

        /*SharedPreferences preferences = context.getSharedPreferences(PPApplication.APPLICATION_PREFS_NAME, Context.MODE_PRIVATE);
        Editor editor = preferences.edit();
        editor.putInt(PPApplication.PREF_EVENT_BLUETOOTH_LAST_STATE, -1);
        editor.commit();*/

        if (bluetooth.isEnabled())
        {
            fillBoundedDevicesList(context);
        }

    }

    static boolean getScanRequest(Context context)
    {
        ApplicationPreferences.getSharedPreferences(context);
        return ApplicationPreferences.preferences.getBoolean(PREF_EVENT_BLUETOOTH_SCAN_REQUEST, false);
    }

    static void setScanRequest(Context context, boolean startScan)
    {
        ApplicationPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
        editor.putBoolean(PREF_EVENT_BLUETOOTH_SCAN_REQUEST, startScan);
        editor.apply();
    }

    static boolean getLEScanRequest(Context context)
    {
        if (Scanner.bluetoothLESupported(context)) {
            ApplicationPreferences.getSharedPreferences(context);
            return ApplicationPreferences.preferences.getBoolean(PREF_EVENT_BLUETOOTH_LE_SCAN_REQUEST, false);
        }
        else
            return false;
    }

    static void setLEScanRequest(Context context, boolean startScan)
    {
        ApplicationPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
        editor.putBoolean(PREF_EVENT_BLUETOOTH_LE_SCAN_REQUEST, startScan);
        editor.apply();
    }

    static boolean getWaitForResults(Context context)
    {
        ApplicationPreferences.getSharedPreferences(context);
        return ApplicationPreferences.preferences.getBoolean(PREF_EVENT_BLUETOOTH_WAIT_FOR_RESULTS, false);
    }

    static void setWaitForResults(Context context, boolean startScan)
    {
        ApplicationPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
        editor.putBoolean(PREF_EVENT_BLUETOOTH_WAIT_FOR_RESULTS, startScan);
        editor.apply();
    }

    static boolean getWaitForLEResults(Context context)
    {
        if (Scanner.bluetoothLESupported(context)) {
            ApplicationPreferences.getSharedPreferences(context);
            return ApplicationPreferences.preferences.getBoolean(PREF_EVENT_BLUETOOTH_WAIT_FOR_LE_RESULTS, false);
        }
        else
            return false;
    }

    static void setWaitForLEResults(Context context, boolean startScan)
    {
        if (Scanner.bluetoothLESupported(context)) {
            ApplicationPreferences.getSharedPreferences(context);
            SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
            editor.putBoolean(PREF_EVENT_BLUETOOTH_WAIT_FOR_LE_RESULTS, startScan);
            editor.apply();
        }
    }

    static void startCLScan(Context context)
    {
        if (bluetooth == null)
            bluetooth = getBluetoothAdapter(context);

        if (bluetooth.isDiscovering())
            bluetooth.cancelDiscovery();

        Scanner.bluetoothDiscoveryStarted = false;

        if (Permissions.checkLocation(context)) {
            boolean startScan = bluetooth.startDiscovery();
            PPApplication.logE("@@@ BluetoothScanJob.startScan", "scanStarted=" + startScan);

            if (!startScan) {
                if (getBluetoothEnabledForScan(context)) {
                    PPApplication.logE("@@@ BluetoothScanJob.startScan", "disable bluetooth");
                    bluetooth.disable();
                }
            }
            setWaitForResults(context, startScan);
        }
        setScanRequest(context, false);
    }

    static void stopCLScan(Context context) {
        if (bluetooth == null)
            bluetooth = getBluetoothAdapter(context);
        if (bluetooth.isDiscovering())
            bluetooth.cancelDiscovery();
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    static void startLEScan(Context context)
    {
        if (Scanner.bluetoothLESupported(context)) {

            //BluetoothScanBroadcastReceiver.initTmpScanResults();

            if (bluetooth == null)
                bluetooth = getBluetoothAdapter(context);

            if (Permissions.checkLocation(context)) {

                boolean startScan = false;

                if ((android.os.Build.VERSION.SDK_INT >= 21)) {
                    if (Scanner.bluetoothLEScanner == null)
                        Scanner.bluetoothLEScanner = bluetooth.getBluetoothLeScanner();
                    //if (Scanner.bluetoothLEScanCallback21 == null)
                    //    Scanner.bluetoothLEScanCallback21 = new BluetoothLEScanCallback21(context);

                    //Scanner.leScanner.stopScan(Scanner.leScanCallback21);

                    ScanSettings.Builder builder = new ScanSettings.Builder();

                    tmpScanLEResults = null;

                    int forceScan = Scanner.getForceOneBluetoothScan(context);
                    if (forceScan == Scanner.FORCE_ONE_SCAN_FROM_PREF_DIALOG)
                        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
                    else
                        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);

                    if (bluetooth.isOffloadedScanBatchingSupported())
                        builder.setReportDelay(ApplicationPreferences.applicationEventBluetoothLEScanDuration(context) * 1000);
                    ScanSettings settings = builder.build();

                    List<ScanFilter> filters = new ArrayList<>();
                    try {
                        Scanner.bluetoothLEScanner.startScan(filters, settings, new BluetoothLEScanCallback21(context));
                        startScan = true;
                    } catch (Exception ignored) {}
                }
                else {
                    //if (Scanner.bluetoothLEScanCallback18 == null)
                    //    Scanner.bluetoothLEScanCallback18 = new BluetoothLEScanCallback18(context);

                    //bluetooth.stopLeScan(Scanner.leScanCallback18);

                    tmpScanLEResults = null;

                    startScan = bluetooth.startLeScan(new BluetoothLEScanCallback18(context));

                    if (!startScan) {
                        if (getBluetoothEnabledForScan(context)) {
                            bluetooth.disable();
                        }
                    }
                }

                setWaitForLEResults(context, startScan);
            }
            setLEScanRequest(context, false);
        }
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    static void stopLEScan(Context context) {
        if (Scanner.bluetoothLESupported(context)) {
            if (bluetooth == null)
                bluetooth = getBluetoothAdapter(context);

            if (bluetooth.getState() == BluetoothAdapter.STATE_ON) {
                if ((android.os.Build.VERSION.SDK_INT >= 21)) {
                    if (Scanner.bluetoothLEScanner == null)
                        Scanner.bluetoothLEScanner = bluetooth.getBluetoothLeScanner();
                    //if (Scanner.bluetoothLEScanCallback21 == null)
                    //    Scanner.bluetoothLEScanCallback21 = new BluetoothLEScanCallback21(context);
                    Scanner.bluetoothLEScanner.stopScan(new BluetoothLEScanCallback21(context));
                } else {
                    //if (Scanner.bluetoothLEScanCallback18 == null)
                    //    Scanner.bluetoothLEScanCallback18 = new BluetoothLEScanCallback18(context);
                    bluetooth.stopLeScan(new BluetoothLEScanCallback18(context));
                }
            }
        }
    }

    static void finishLEScan(Context context) {
        PPApplication.logE("BluetoothScanJob.finishLEScan","xxx");

        List<BluetoothDeviceData> scanResults = new ArrayList<>();

        if (tmpScanLEResults != null) {

            for (BluetoothDeviceData device : tmpScanLEResults) {
                scanResults.add(new BluetoothDeviceData(device.getName(), device.address, device.type, false, 0));
            }
            tmpScanLEResults = null;
        }

        saveLEScanResults(context, scanResults);
    }

    static void startScanner(Context context, boolean fromDialog)
    {
        PPApplication.logE("$$$ BluetoothScanJob.startScanner", "xxx");
        DataWrapper dataWrapper = new DataWrapper(context, false, false, 0);
        Profile profile = dataWrapper.getActivatedProfile();
        profile = Profile.getMappedProfile(profile, context);
        if (fromDialog || (profile == null) || (profile._applicationDisableBluetoothScanning != 1)) {
            if (fromDialog) {
                try {
                    Intent scanServiceIntent = new Intent(context, ScannerService.class);
                    scanServiceIntent.putExtra(ScannerService.EXTRA_SCANNER_TYPE, Scanner.SCANNER_TYPE_BLUETOOTH);
                    WakefulIntentService.sendWakefulWork(context, scanServiceIntent);
                } catch (Exception ignored) {
                }
            }
            else {
                Scanner scanner = new Scanner(context);
                scanner.doScan(Scanner.SCANNER_TYPE_BLUETOOTH);
            }
        }
        dataWrapper.invalidateDataWrapper();
    }

    /*
    static public void stopScan(Context context)
    {
        unlock();
        if (getBluetoothEnabledForScan(context))
            bluetooth.disable();
        setBluetoothEnabledForScan(context, false);
        setScanRequest(context, false);
        setWaitForResults(context, false);
        PPApplication.setForceOneBluetoothScan(context, false);
    }
    */

    static boolean getBluetoothEnabledForScan(Context context)
    {
        ApplicationPreferences.getSharedPreferences(context);
        return ApplicationPreferences.preferences.getBoolean(PREF_EVENT_BLUETOOTH_ENABLED_FOR_SCAN, false);
    }

    static void setBluetoothEnabledForScan(Context context, boolean setEnabled)
    {
        ApplicationPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
        editor.putBoolean(PREF_EVENT_BLUETOOTH_ENABLED_FOR_SCAN, setEnabled);
        editor.apply();
    }

    static int getBluetoothType(BluetoothDevice device) {
        if (android.os.Build.VERSION.SDK_INT >= 18)
            return device.getType();
        else
            return 1; // BluetoothDevice.DEVICE_TYPE_CLASSIC
    }

    static void fillBoundedDevicesList(Context context)
    {
        //if (boundedDevicesList == null)
        //    boundedDevicesList = new ArrayList<BluetoothDeviceData>();

        List<BluetoothDeviceData> boundedDevicesList  = new ArrayList<>();

        if (bluetooth == null)
            bluetooth = getBluetoothAdapter(context);

        Set<BluetoothDevice> boundedDevices = bluetooth.getBondedDevices();
        boundedDevicesList.clear();
        if (boundedDevices != null) {
            for (BluetoothDevice device : boundedDevices) {
                boundedDevicesList.add(new BluetoothDeviceData(device.getName(), device.getAddress(),
                        getBluetoothType(device), false, 0));
            }
        }
        saveBoundedDevicesList(context, boundedDevicesList);
    }

    private static final String SCAN_RESULT_COUNT_PREF = "count";
    private static final String SCAN_RESULT_DEVICE_PREF = "device";

    //public static void getBoundedDevicesList(Context context)
    static List<BluetoothDeviceData> getBoundedDevicesList(Context context)
    {
        synchronized (PPApplication.wifiScanResultsMutex) {
            //if (boundedDevicesList == null)
            //    boundedDevicesList = new ArrayList<BluetoothDeviceData>();

            //boundedDevicesList.clear();

            List<BluetoothDeviceData> boundedDevicesList = new ArrayList<>();

            SharedPreferences preferences = context.getSharedPreferences(PPApplication.BLUETOOTH_BOUNDED_DEVICES_LIST_PREFS_NAME, Context.MODE_PRIVATE);

            int count = preferences.getInt(SCAN_RESULT_COUNT_PREF, 0);

            Gson gson = new Gson();

            for (int i = 0; i < count; i++) {
                String json = preferences.getString(SCAN_RESULT_DEVICE_PREF + i, "");
                if (!json.isEmpty()) {
                    BluetoothDeviceData device = gson.fromJson(json, BluetoothDeviceData.class);
                    boundedDevicesList.add(device);
                }
            }

            return boundedDevicesList;
        }
    }

    private static void saveBoundedDevicesList(Context context, List<BluetoothDeviceData> boundedDevicesList)
    {
        synchronized (PPApplication.wifiScanResultsMutex) {
            //if (boundedDevicesList == null)
            //    boundedDevicesList = new ArrayList<BluetoothDeviceData>();

            SharedPreferences preferences = context.getSharedPreferences(PPApplication.BLUETOOTH_BOUNDED_DEVICES_LIST_PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();

            editor.clear();

            editor.putInt(SCAN_RESULT_COUNT_PREF, boundedDevicesList.size());

            Gson gson = new Gson();

            for (int i = 0; i < boundedDevicesList.size(); i++) {
                String json = gson.toJson(boundedDevicesList.get(i));
                editor.putString(SCAN_RESULT_DEVICE_PREF + i, json);
            }

            editor.apply();
        }
    }

    static List<BluetoothDeviceData> getScanResults(Context context)
    {
        synchronized (PPApplication.wifiScanResultsMutex) {
            List<BluetoothDeviceData> scanResults = new ArrayList<>();

            SharedPreferences preferences = context.getSharedPreferences(PPApplication.BLUETOOTH_CL_SCAN_RESULTS_PREFS_NAME, Context.MODE_PRIVATE);
            int count = preferences.getInt(SCAN_RESULT_COUNT_PREF, -1);

            if (count >= 0) {
                Gson gson = new Gson();
                for (int i = 0; i < count; i++) {
                    String json = preferences.getString(SCAN_RESULT_DEVICE_PREF + i, "");
                    if (!json.isEmpty()) {
                        BluetoothDeviceData device = gson.fromJson(json, BluetoothDeviceData.class);
                        scanResults.add(device);
                    }
                }
            }

            preferences = context.getSharedPreferences(PPApplication.BLUETOOTH_LE_SCAN_RESULTS_PREFS_NAME, Context.MODE_PRIVATE);
            count = preferences.getInt(SCAN_RESULT_COUNT_PREF, -1);

            if (count >= 0) {
                Gson gson = new Gson();
                for (int i = 0; i < count; i++) {
                    String json = preferences.getString(SCAN_RESULT_DEVICE_PREF + i, "");
                    if (!json.isEmpty()) {
                        BluetoothDeviceData device = gson.fromJson(json, BluetoothDeviceData.class);
                        scanResults.add(device);
                    }
                }
            }

            if (scanResults.size() == 0)
                return null;
            else
                return scanResults;
        }
    }

    private static void clearScanResults(Context context) {
        synchronized (PPApplication.wifiScanResultsMutex) {
            SharedPreferences preferences = context.getSharedPreferences(PPApplication.BLUETOOTH_CL_SCAN_RESULTS_PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();

            editor.clear();
            editor.putInt(SCAN_RESULT_COUNT_PREF, -1);

            editor.apply();

            preferences = context.getSharedPreferences(PPApplication.BLUETOOTH_LE_SCAN_RESULTS_PREFS_NAME, Context.MODE_PRIVATE);
            editor = preferences.edit();

            editor.clear();
            editor.putInt(SCAN_RESULT_COUNT_PREF, -1);

            editor.apply();
        }
    }

    private static void saveCLScanResults(Context context, List<BluetoothDeviceData> scanResults)
    {
        synchronized (PPApplication.wifiScanResultsMutex) {
            SharedPreferences preferences = context.getSharedPreferences(PPApplication.BLUETOOTH_CL_SCAN_RESULTS_PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();

            editor.clear();

            editor.putInt(SCAN_RESULT_COUNT_PREF, scanResults.size());

            Gson gson = new Gson();
            for (int i = 0; i < scanResults.size(); i++) {
                String json = gson.toJson(scanResults.get(i));
                editor.putString(SCAN_RESULT_DEVICE_PREF + i, json);
            }

            editor.apply();
        }
    }

    private static void saveLEScanResults(Context context, List<BluetoothDeviceData> scanResults)
    {
        synchronized (PPApplication.wifiScanResultsMutex) {
            SharedPreferences preferences = context.getSharedPreferences(PPApplication.BLUETOOTH_LE_SCAN_RESULTS_PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();

            editor.clear();

            editor.putInt(SCAN_RESULT_COUNT_PREF, scanResults.size());

            Gson gson = new Gson();
            for (int i = 0; i < scanResults.size(); i++) {
                String json = gson.toJson(scanResults.get(i));
                editor.putString(SCAN_RESULT_DEVICE_PREF + i, json);
            }

            editor.apply();
        }
    }

    /*
    public static void addScanResult(Context context, BluetoothDeviceData device) {
        List<BluetoothDeviceData> savedScanResults = getScanResults(context);
        if (savedScanResults == null)
            savedScanResults = new ArrayList<>();

        boolean found = false;
        for (BluetoothDeviceData _device : savedScanResults) {

            if (_device.address.equals(device.address)) {
                found = true;
                break;
            }
        }
        if (!found) {
            savedScanResults.add(new BluetoothDeviceData(device.name, device.address, device.type, false));
        }

        SharedPreferences preferences = context.getSharedPreferences(PPApplication.BLUETOOTH_SCAN_RESULTS_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.clear();

        editor.putInt(SCAN_RESULT_COUNT_PREF, savedScanResults.size());

        Gson gson = new Gson();
        for (int i = 0; i < savedScanResults.size(); i++)
        {
            String json = gson.toJson(savedScanResults.get(i));
            editor.putString(SCAN_RESULT_DEVICE_PREF+i, json);
        }

        editor.commit();
    }
    */

    static void addLEScanResult(BluetoothDeviceData device) {
        if (tmpScanLEResults == null)
            tmpScanLEResults = new ArrayList<>();

        boolean found = false;
        for (BluetoothDeviceData _device : tmpScanLEResults) {

            if (_device.address.equals(device.address)) {
                found = true;
                break;
            }
        }
        if (!found) {
            if (tmpScanLEResults != null) // maybe set to null by startLEScan() or funuishLEScan()
                tmpScanLEResults.add(new BluetoothDeviceData(device.name, device.address, device.type, false, 0));
        }
    }

    static void finishScan(Context context) {
        PPApplication.logE("BluetoothScanJob.finishScan","BluetoothScanBroadcastReceiver: discoveryStarted="+Scanner.bluetoothDiscoveryStarted);

        if (Scanner.bluetoothDiscoveryStarted) {

            Scanner.bluetoothDiscoveryStarted = false;

            List<BluetoothDeviceData> scanResults = new ArrayList<>();

            if (Scanner.tmpBluetoothScanResults != null) {

                for (BluetoothDeviceData device : Scanner.tmpBluetoothScanResults) {
                    scanResults.add(new BluetoothDeviceData(device.getName(), device.address, device.type, false, 0));
                }
            }

            BluetoothScanJob.saveCLScanResults(context, scanResults);

            BluetoothScanJob.setWaitForResults(context, false);

            int forceOneScan = Scanner.getForceOneBluetoothScan(context);
            Scanner.setForceOneBluetoothScan(context, Scanner.FORCE_ONE_SCAN_DISABLED);

            if (forceOneScan != Scanner.FORCE_ONE_SCAN_FROM_PREF_DIALOG)// not start service for force scan
            {
                // start job
                new Handler(context.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        EventsHandlerJob.startForSensor(EventsHandler.SENSOR_TYPE_BLUETOOTH_SCANNER);
                    }
                }, 5000);
            }

            Scanner.tmpBluetoothScanResults = null;
        }
    }

}
