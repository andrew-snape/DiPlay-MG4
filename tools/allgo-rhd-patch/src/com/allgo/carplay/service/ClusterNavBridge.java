package com.allgo.carplay.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Parcel;
import android.util.Log;
import com.allgo.mirroring.control.NavRouteGuidanceUpdatesController.NavRouteGuidanceUpdateInfo;
import com.allgo.mirroring.control.NavRouteGuidanceUpdatesController.NavRouteManeuverUpdateInfo;
import java.util.TreeMap;

/**
 * Sends the stock CarPlay app's route guidance to the MG4 instrument cluster.
 * Our own code, injected by tools/allgo-rhd-patch. Mirrors Mg4ClusterNavigation in the
 * MG4CPlay app: the cluster takes guidance from com.saicmotor.adapterservice MapService.
 * Every entry point catches everything, so the stock app is never disturbed.
 */
public final class ClusterNavBridge {
    private static final String TAG = "RHD_CLUSTER";
    private static final String PACKAGE = "com.saicmotor.adapterservice";
    private static final String SERVICE = "com.saicmotor.adapterservice.services.MapService";
    private static final String DESCRIPTOR = "com.saicmotor.adapterservice.IMapService";
    private static final int TX_IS_MAP_NAVIGATING = 22;
    private static final int TX_GUIDE_STATUS = 29;
    private static final int TX_GUIDE_INFOS = 30;
    private static final int TX_ROAD_INFO = 31;
    private static final int TX_REMAINING_TIMES = 38;
    private static final int TX_REMAINING_DISTANCE = 39;

    private static final Object LOCK = new Object();
    private static IBinder binder;
    private static boolean bound;
    private static boolean guiding;

    // Latest state, from the stock callbacks.
    private static final TreeMap<Integer, int[]> MANEUVERS = new TreeMap<Integer, int[]>(); // index -> {type, side}
    private static final TreeMap<Integer, String> ROADS = new TreeMap<Integer, String>();
    private static int rawDistance = -1;
    private static int shownDistance = -1;
    private static int remainMetres = -1;
    private static int remainMinutes = -1;

    // What was last sent.
    private static int sentIcon = -1;
    private static int sentDistance = -1;
    private static String sentRoad;
    private static int sentRemainMetres = -1;
    private static int sentRemainMinutes = -1;

    private ClusterNavBridge() {
    }

    /** Called from onNavRouteGuidanceUpdate. */
    public static void onGuidance(NavRouteGuidanceUpdateInfo info) {
        try {
            synchronized (LOCK) {
                if (info == null) return;
                if ((info.mValidInfo & 2) != 0 && (info.mRouteState == 0 || info.mRouteState == 2)) {
                    Log.i(TAG, "route state " + info.mRouteState + ", guidance ends");
                    clearLocked();
                    publishLocked();
                    return;
                }
                if ((info.mValidInfo & 1024) != 0) {
                    int raw = Math.max(0, info.mDistToNextManeuver);
                    // A larger distance than before means the turn was taken: move to the next maneuver.
                    if (rawDistance >= 0 && raw > rawDistance + 50 && MANEUVERS.size() > 1) {
                        int first = MANEUVERS.firstKey();
                        MANEUVERS.remove(first);
                        ROADS.remove(first);
                    }
                    rawDistance = raw;
                    String text = (info.mValidInfo & 2048) != 0 ? info.mDistToNextManeuverDispStr : null;
                    int units = (info.mValidInfo & 4096) != 0 ? info.mDistToNextManeuverDispUnits : -1;
                    shownDistance = shown(raw, text, units);
                }
                if ((info.mValidInfo & 128) != 0) remainMetres = Math.max(0, info.mDistRemain);
                if ((info.mValidInfo & 64) != 0 && info.mTimeRemainToDest != null && info.mTimeRemainToDest.length >= 2) {
                    // Assumed [hours, minutes], like the stock app's REM_TIME_TO_DEST bundle.
                    remainMinutes = Math.max(0, info.mTimeRemainToDest[0] * 60 + info.mTimeRemainToDest[1]);
                }
                ensureBoundLocked();
                publishLocked();
            }
        } catch (Throwable t) {
            Log.w(TAG, "onGuidance failed: " + t);
        }
    }

    /** Called from onNavRouteManeuverInfoUpdate. */
    public static void onManeuver(NavRouteManeuverUpdateInfo info) {
        try {
            synchronized (LOCK) {
                if (info == null || (info.mValidInfo & 8) == 0) return;
                int index = (info.mValidInfo & 2) != 0 ? info.mIndex : 0;
                int side = (info.mValidInfo & 256) != 0 ? info.mDriveSide : 1;
                MANEUVERS.put(index, new int[] {info.mManeuverType, side});
                String road = (info.mValidInfo & 16) != 0 && info.mAfterManeuverRoadName != null
                    ? info.mAfterManeuverRoadName.trim() : "";
                ROADS.put(index, road);
                // Drop maneuvers behind this one when the iPhone restarts the list at a lower index.
                while (MANEUVERS.size() > 24) {
                    int first = MANEUVERS.firstKey();
                    MANEUVERS.remove(first);
                    ROADS.remove(first);
                }
                ensureBoundLocked();
                publishLocked();
            }
        } catch (Throwable t) {
            Log.w(TAG, "onManeuver failed: " + t);
        }
    }

    /** Called from StopRouteGuidanceUpdate and when the stock controller unregisters. */
    public static void stop() {
        try {
            synchronized (LOCK) {
                clearLocked();
                publishLocked();
            }
        } catch (Throwable t) {
            Log.w(TAG, "stop failed: " + t);
        }
    }

    private static void clearLocked() {
        MANEUVERS.clear();
        ROADS.clear();
        rawDistance = -1;
        shownDistance = -1;
        remainMetres = -1;
        remainMinutes = -1;
    }

    private static void ensureBoundLocked() {
        if (bound) return;
        try {
            Context app = (Context) Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication").invoke(null);
            if (app == null) return;
            Intent intent = new Intent().setComponent(new ComponentName(PACKAGE, SERVICE));
            bound = app.bindService(intent, CONNECTION, Context.BIND_AUTO_CREATE);
            Log.i(TAG, bound ? "binding adapter MapService" : "adapter MapService not available");
        } catch (Throwable t) {
            Log.w(TAG, "bind failed: " + t);
        }
    }

    private static final ServiceConnection CONNECTION = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                synchronized (LOCK) {
                    binder = service;
                    Log.i(TAG, "adapter service connected");
                    publishLocked();
                }
            } catch (Throwable t) {
                Log.w(TAG, "connected failed: " + t);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (LOCK) {
                binder = null;
                guiding = false;
                resetSentLocked();
            }
        }
    };

    private static void resetSentLocked() {
        sentIcon = -1;
        sentDistance = -1;
        sentRoad = null;
        sentRemainMetres = -1;
        sentRemainMinutes = -1;
    }

    private static void publishLocked() {
        IBinder service = binder;
        if (service == null || !service.isBinderAlive()) return;
        if (MANEUVERS.isEmpty() || shownDistance < 0) {
            if (guiding) {
                call(service, TX_GUIDE_STATUS, 0, 0, null);
                call(service, TX_IS_MAP_NAVIGATING, 0, 0, null);
                guiding = false;
                resetSentLocked();
                Log.i(TAG, "guidance ended");
            }
            return;
        }
        if (!guiding) {
            call(service, TX_IS_MAP_NAVIGATING, 1, 0, null);
            boolean ok = call(service, TX_GUIDE_STATUS, 1, 0, null);
            guiding = true;
            Log.i(TAG, "guidance started sent=" + ok);
        }
        int index = MANEUVERS.firstKey();
        int[] maneuver = MANEUVERS.get(index);
        String road = ROADS.get(index);
        if (road == null) road = "";
        int icon = icon(maneuver[0], maneuver[1]);
        if (icon != sentIcon || shownDistance != sentDistance || !road.equals(sentRoad)) {
            boolean ok = call(service, TX_GUIDE_INFOS, icon, shownDistance, road);
            if (!road.equals(sentRoad)) call(service, TX_ROAD_INFO, 0, 0, road);
            if (icon != sentIcon || !road.equals(sentRoad)) {
                Log.i(TAG, "apple=" + maneuver[0] + " side=" + maneuver[1] + " icon=" + icon
                    + " distance=" + shownDistance + "m road=\"" + road + "\" sent=" + ok);
            }
            if (shownDistance != sentDistance) {
                Log.i(TAG, "distance raw=" + rawDistance + "m shown=" + shownDistance + "m");
            }
            sentIcon = icon;
            sentDistance = shownDistance;
            sentRoad = road;
        }
        if (remainMetres >= 0 && remainMetres != sentRemainMetres) {
            call(service, TX_REMAINING_DISTANCE, remainMetres, 0, null);
            sentRemainMetres = remainMetres;
        }
        if (remainMinutes >= 0 && remainMinutes != sentRemainMinutes) {
            call(service, TX_REMAINING_TIMES, remainMinutes, 0, null);
            sentRemainMinutes = remainMinutes;
        }
    }

    /** Writes one IMapService call: int a, then (for TX_GUIDE_INFOS) int b and the road string, or just the string. */
    private static boolean call(IBinder service, int code, int a, int b, String text) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            if (code == TX_GUIDE_INFOS) {
                data.writeInt(a);
                data.writeInt(b);
                data.writeString(text);
            } else if (code == TX_ROAD_INFO) {
                data.writeString(text);
            } else {
                data.writeInt(a);
            }
            service.transact(code, data, reply, 0);
            reply.readException();
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    /** CarPlay's display distance in metres: the iPhone's text if it fits the raw distance, else rounded. */
    static int shown(int raw, String text, int units) {
        if (text != null && units >= 0) {
            double number = firstNumber(text);
            if (number >= 0) {
                double metres;
                if (units == 0) metres = number * 1000.0; // kilometres
                else if (units == 1 || units == 2) metres = number; // metres (iAP2 1, AllGo 2)
                else metres = -1;
                if (metres >= 0) {
                    int m = (int) Math.round(metres);
                    int tolerance = Math.max(150, raw * 35 / 100);
                    if (Math.abs(m - raw) <= tolerance) return m;
                }
            }
        }
        int step = raw >= 500 ? 100 : raw >= 100 ? 50 : 10;
        return ((raw + step / 2) / step) * step;
    }

    private static double firstNumber(String text) {
        int i = 0;
        int n = text.length();
        while (i < n && !Character.isDigit(text.charAt(i))) i++;
        if (i >= n) return -1;
        int start = i;
        while (i < n && (Character.isDigit(text.charAt(i))
            || ((text.charAt(i) == '.' || text.charAt(i) == ',') && i + 1 < n && Character.isDigit(text.charAt(i + 1))))) i++;
        try {
            return Double.parseDouble(text.substring(start, i).replace(',', '.'));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Apple maneuver type to the cluster's ic_tbt_N icon. Same table as Mg4ClusterNavigation.icon. */
    static int icon(int appleType, int drivingSide) {
        boolean leftHand = drivingSide == 1;
        if (appleType >= 28 && appleType <= 46) {
            switch (appleType - 27) {
                case 1: return leftHand ? 10 : 11;
                case 2: return 9;
                case 3: return leftHand ? 11 : 10;
                default: return 12;
            }
        }
        switch (appleType) {
            case 1: case 20: return 1;
            case 2: case 21: return 2;
            case 49: return 3;
            case 50: return 4;
            case 47: return 8;
            case 48: return 7;
            case 4: case 18: case 26: return leftHand ? 6 : 5;
            case 19: return 12;
            case 6: case 7: return 9;
            case 13: case 52: return 17;
            case 14: case 53: return 18;
            case 22: return 36;
            case 23: return 37;
            case 8: return leftHand ? 36 : 37;
            case 9: return leftHand ? 32 : 33;
            case 15: case 16: case 17: return 19;
            case 10: case 12: case 27: return 20;
            case 24: return 21;
            case 25: return 22;
            default: return 0;
        }
    }
}
