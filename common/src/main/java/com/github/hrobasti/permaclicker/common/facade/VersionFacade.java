package com.github.hrobasti.permaclicker.common.facade;

import com.github.hrobasti.turtlelib.VersionComparator.VersionComparator;

/**
 * Single facade for semantic version comparisons delegated to TurtleLib VersionComparator.
 */
public final class VersionFacade {
    private VersionFacade() {
    }

    public static int compare(String left, String right) {
        return VersionComparator.compare(left, right);
    }

    public static boolean isGreater(String left, String right) {
        return VersionComparator.isGreater(left, right);
    }
}
