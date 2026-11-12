package com.allgo.mirroring.control;

/** Compile-time stub of the stock class (fields only). Never shipped. */
public class NavRouteGuidanceUpdatesController {
    public static final class NavRouteGuidanceUpdateInfo {
        public int mValidInfo;
        public int mRouteState;
        public int[] mTimeRemainToDest;
        public int mDistRemain;
        public int mDistToNextManeuver;
        public String mDistToNextManeuverDispStr;
        public int mDistToNextManeuverDispUnits;
    }

    public static final class NavRouteManeuverUpdateInfo {
        public int mValidInfo;
        public int mIndex;
        public int mManeuverType;
        public String mAfterManeuverRoadName;
        public int mDriveSide;
    }
}
