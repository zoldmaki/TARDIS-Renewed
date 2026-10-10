package dev.zoldmaki.renewedtardis.flight;

/** Pure travel-progress helpers kept independent from Minecraft classes for testing. */
public final class FlightTravelMath {
    private FlightTravelMath() {
    }

    public static long clampElapsed(long elapsedMillis, long durationMillis) {
        long duration = Math.max(0L, durationMillis);
        return Math.max(0L, Math.min(duration, elapsedMillis));
    }

    public static int progressPercent(long elapsedMillis, long durationMillis) {
        long duration = Math.max(0L, durationMillis);
        if (duration == 0L) {
            return 100;
        }
        long elapsed = clampElapsed(elapsedMillis, duration);
        return (int) Math.min(100L, (elapsed * 100L) / duration);
    }

    public static long remainingMillis(long elapsedMillis, long durationMillis) {
        long duration = Math.max(0L, durationMillis);
        return Math.max(0L, duration - clampElapsed(elapsedMillis, duration));
    }

    public static int interpolateCoordinate(int from, int to, long elapsedMillis, long durationMillis) {
        long duration = Math.max(0L, durationMillis);
        if (duration == 0L) {
            return to;
        }
        long elapsed = clampElapsed(elapsedMillis, duration);
        double progress = (double) elapsed / (double) duration;
        double value = (double) from + ((double) to - (double) from) * progress;
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) Math.round(value);
    }
}
