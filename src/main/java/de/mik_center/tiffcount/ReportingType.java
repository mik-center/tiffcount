// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.tiffcount;

/**
 * Types of reporting.
 */
public enum ReportingType {
    /**
     * A comprehensive report is generated and written to the specified directory.
     */
    ENTIRE,

    /**
     * A report is created for each directory and written into that very directory.
     */
    BY_DIRECTORY;
}
