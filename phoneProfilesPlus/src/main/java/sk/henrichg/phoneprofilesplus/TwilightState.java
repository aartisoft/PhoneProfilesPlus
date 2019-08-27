package sk.henrichg.phoneprofilesplus;

import java.text.DateFormat;
import java.util.Date;

import androidx.annotation.NonNull;

@SuppressWarnings("unused")
class TwilightState {

    private final boolean mIsNight;
    private final long mYesterdaySunrise;
    private final long mYesterdaySunset;
    private final long mTodaySunrise;
    private final long mTodaySunset;
    private final long mTomorrowSunrise;
    private final long mTomorrowSunset;

    private long[] mDaysSunrise = new long[9];
    private long[] mDaysSunset = new long[9];

    TwilightState(boolean isNight,
                  long yesterdaySunrise, long yesterdaySunset,
                  long todaySunrise, long todaySunset,
                  long tomorrowSunrise, long tomorrowSunset,
                  long[] daysSunrise, long[] daysSunset) {
        mIsNight = isNight;
        mYesterdaySunrise = yesterdaySunrise;
        mYesterdaySunset = yesterdaySunset;
        mTodaySunrise = todaySunrise;
        mTodaySunset = todaySunset;
        mTomorrowSunrise = tomorrowSunrise;
        mTomorrowSunset = tomorrowSunset;

        for (int i = 0; i < 9; i++) {
            mDaysSunrise[i] = daysSunrise[i];
            mDaysSunset[i] = daysSunset[i];
        }
    }

    /**
     * Returns true if it is currently night time.
     */
    boolean isNight() {
        return mIsNight;
    }

    /**
     * Returns the time of yesterday's sunrise in the System.currentTimeMillis() timebase,
     * or -1 if the sun never sets.
     */
    long getYesterdaySunrise() {
        return mYesterdaySunrise;
    }

    /**
     * Returns the time of yesterday's sunset in the System.currentTimeMillis() timebase,
     * or -1 if the sun never sets.
     */
    long getYesterdaySunset() {
        return mYesterdaySunset;
    }

    /**
     * Returns the time of today's sunrise in the System.currentTimeMillis() timebase,
     * or -1 if the sun never rises.
     */
    long getTodaySunrise() {
        return mTodaySunrise;
    }

    /**
     * Returns the time of today's sunset in the System.currentTimeMillis() timebase,
     * or -1 if the sun never sets.
     */
    long getTodaySunset() {
        return mTodaySunset;
    }

    /**
     * Returns the time of tomorrow's sunrise in the System.currentTimeMillis() timebase,
     * or -1 if the sun never rises.
     */
    long getTomorrowSunrise() {
        return mTomorrowSunrise;
    }

    /**
     * Returns the time of tomorrow's sunset in the System.currentTimeMillis() timebase,
     * or -1 if the sun never rises.
     */
    long getTomorrowSunset() {
        return mTomorrowSunset;
    }

    long[] getDaysSunrise() { return mDaysSunrise; }
    long[] getDaysSunset() { return mDaysSunset; }

    @Override
    public boolean equals(Object o) {
        return o instanceof TwilightState && equals((TwilightState)o);
    }

    boolean equals(TwilightState other) {
        return other != null
                && mIsNight == other.mIsNight
                && mYesterdaySunrise == other.mYesterdaySunrise
                && mYesterdaySunset == other.mYesterdaySunset
                && mTodaySunrise == other.mTodaySunrise
                && mTodaySunset == other.mTodaySunset
                && mTomorrowSunrise == other.mTomorrowSunrise
                && mTomorrowSunset == other.mTomorrowSunset;
    }

    @Override
    public int hashCode() {
        return 0; // don't care
    }

    @NonNull
    @Override
    public String toString() {
        DateFormat f = DateFormat.getDateTimeInstance();
        return "{TwilightState: isNight=" + mIsNight
                + ", mYesterdaySunrise=" + f.format(new Date(mYesterdaySunrise))
                + ", mYesterdaySunset=" + f.format(new Date(mYesterdaySunset))
                + ", mTodaySunrise=" + f.format(new Date(mTodaySunrise))
                + ", mTodaySunset=" + f.format(new Date(mTodaySunset))
                + ", mTomorrowSunrise=" + f.format(new Date(mTomorrowSunrise))
                + ", mTomorrowSunset=" + f.format(new Date(mTomorrowSunset))
                + "}";
    }

}