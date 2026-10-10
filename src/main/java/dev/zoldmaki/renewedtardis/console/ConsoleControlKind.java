package dev.zoldmaki.renewedtardis.console;

/** Physical interaction family, independent of the eventual model part. */
public enum ConsoleControlKind {
    BUTTON,
    PULL_SWITCH,
    LEVER,
    DIAL,
    SLIDER,
    GAUGE,
    SCREEN,
    SLOT,
    INDICATOR;

    public boolean isDirectlyAdjustable() {
        return this == PULL_SWITCH || this == LEVER || this == DIAL || this == SLIDER;
    }

    public boolean isReadOnly() {
        return this == GAUGE || this == INDICATOR;
    }
}
