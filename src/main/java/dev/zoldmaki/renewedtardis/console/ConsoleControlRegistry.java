package dev.zoldmaki.renewedtardis.console;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Immutable registry views used by rendering, hit routing, and future QTEs. */
public final class ConsoleControlRegistry {
    private static final Map<ConsolePanel, List<ConsoleControlId>> BY_PANEL;
    private static final List<ConsoleControlId> QUICK_TIME_CONTROLS;

    static {
        EnumMap<ConsolePanel, List<ConsoleControlId>> byPanel = new EnumMap<>(ConsolePanel.class);
        for (ConsolePanel panel : ConsolePanel.values()) {
            byPanel.put(panel, new ArrayList<ConsoleControlId>());
        }

        List<ConsoleControlId> qte = new ArrayList<>();
        for (ConsoleControlId control : ConsoleControlId.values()) {
            byPanel.get(control.getPanel()).add(control);
            if (control.isQuickTimeEligible()) {
                qte.add(control);
            }
        }

        EnumMap<ConsolePanel, List<ConsoleControlId>> frozen = new EnumMap<>(ConsolePanel.class);
        for (Map.Entry<ConsolePanel, List<ConsoleControlId>> entry : byPanel.entrySet()) {
            frozen.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        BY_PANEL = Collections.unmodifiableMap(frozen);
        QUICK_TIME_CONTROLS = Collections.unmodifiableList(qte);
    }

    private ConsoleControlRegistry() {
    }

    public static List<ConsoleControlId> getControls(ConsolePanel panel) {
        List<ConsoleControlId> controls = BY_PANEL.get(panel);
        return controls == null ? Collections.<ConsoleControlId>emptyList() : controls;
    }

    public static List<ConsoleControlId> getQuickTimeControls() {
        return QUICK_TIME_CONTROLS;
    }
}
