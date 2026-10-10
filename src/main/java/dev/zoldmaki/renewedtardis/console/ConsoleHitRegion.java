package dev.zoldmaki.renewedtardis.console;

/** One normalized rectangular interaction region on a placeholder panel. */
public final class ConsoleHitRegion {
    private final double minU;
    private final double minV;
    private final double maxU;
    private final double maxV;
    private final ConsoleControlId control;

    public ConsoleHitRegion(
        double minU,
        double minV,
        double maxU,
        double maxV,
        ConsoleControlId control
    ) {
        if (control == null) {
            throw new IllegalArgumentException("control cannot be null");
        }
        if (minU < 0.0D || minV < 0.0D || maxU > 1.0D || maxV > 1.0D || minU >= maxU || minV >= maxV) {
            throw new IllegalArgumentException("Invalid normalized console hit region");
        }
        this.minU = minU;
        this.minV = minV;
        this.maxU = maxU;
        this.maxV = maxV;
        this.control = control;
    }

    public ConsoleControlId getControl() {
        return control;
    }

    public boolean contains(double u, double v) {
        return u >= minU && u < maxU && v >= minV && v < maxV;
    }
}
