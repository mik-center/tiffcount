// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.tiffcount;

import java.time.*;

/**
 * Data about a file system directory. This bean is used in a map containing
 * directory paths, therefore the bean does not contain the path itself.
 */
public class DirectoryData {

    private long bytes = 0;
    private Instant lastModified;

    /**
     * Creates a new bean for the directory information.
     */
    public DirectoryData() { }

    /**
     * Sets the time of the last modification. Set the time as reported by the file
     * system.
     * 
     * @param lastModified time of last modification
     */
    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Returns the time of the last modification of the directory.
     * 
     * @return time of last modification
     */
    public Instant getLastModified() {
        return lastModified;
    }

    /**
     * Returns the stored size in bytes. The size is the sum of the bytes of all
     * files in the directory that matched the file filter when examined. Files in
     * subdirectories are not included.
     * 
     * @return size in bytes
     */
    public long getSize() {
        return bytes;
    }

    /**
     * Specifies the size in bytes. Sum the size in bytes of all files in the
     * directory that match the file filter for the desired files, excluding files
     * in subdirectories.
     * 
     * @param bytes size in bytes
     */
    public void setSize(long bytes) {
        this.bytes = bytes;
    }
}
