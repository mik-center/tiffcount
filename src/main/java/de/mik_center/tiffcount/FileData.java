// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.tiffcount;

import static de.mik_center.tiffcount.FileData.OtherField.*;
import static java.util.regex.Pattern.*;

import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;

/**
 * Data about a file. This primarily refers to a file containing an image
 * (raster graphic), but that's not mandatory.
 */
public class FileData {
    /**
     * An image resolution statement.
     */
    public static class Resolution {
        /**
         * Conversion factor for converting pixels per meter to pixels per inch. While
         * SI units are generally preferred, resolutions in the reprographics are still
         * regularly specified in DPI.
         */
        public static final double INCH_PER_METER = 100 / 2.54;

        /**
         * Horizontal resolution specification, to be set by the subclass. Different
         * horizontal and vertical resolutions are technically possible, but nowadays
         * are more of a relic of the past and an indication of a potential error. The
         * human visual system is more sensitive to horizontal resolution.
         */
        protected String x;

        /**
         * Specifies the vertical resolution, to be set by the subclass. Different
         * horizontal and vertical resolutions are possible, but this is more likely an
         * indication of a potential error. The human visual system is less sensitive to
         * vertical resolution than to horizontal resolution.
         */
        protected String y;

        /*
         * An instance of this class is only created by the containing class.
         */
        private Resolution() { }

        /**
         * &#x1F511; Returns horizontal resolution information.
         * @return horizontal resolution information
         */
        public String getX() {
            return x;
        }

        /**
         * &#x1F511; Returns vertical resolution information.
         * @return vertical resolution information
         */
        public String getY() {
            return y;
        }

        /**
         * Formats a double-precision DPI value for display. If it rounds to the first
         * three decimal places, it is displayed as a whole number; otherwise, it
         * displays with the first three decimal places. The suffix is “dpi” in
         * lowercase.
         * 
         * @param decimal fractional DPI value
         * @return textual representation
         */
        public static String formatDpi(double decimal) {
            String representation = Long.toString(Math.round(1000 * decimal));
            while (representation.length() < 4) representation = "0".concat(representation);
            int pos = representation.length() - 3;
            String whole = representation.substring(0, pos);
            String fraction = representation.substring(pos, representation.length());
            return (fraction.equals("000") ? whole : whole + '.' + fraction) + " dpi";
        }
    }

    private static final TreeMap<String, String> EMPTY_TREE_MAP = new TreeMap<>();
    private static final Pattern PATTERN_TIFF_CI = Pattern.compile(".*\\.tiff?$", CASE_INSENSITIVE);

    private Path path;
    private Instant lastModified;
    private long size;
    private String readException;

    /**
     * Page count for multi-page images and PDF files; otherwise, it's "{@code 1}".
     * Initially, it's an empty string. This value must be assigned from the
     * subclass. If the number of images cannot be determined, but it's clear
     * <I>that</I> there is more than one image, then "{@code several}" is entered.
     */
    protected String numberOfPages = "";

    /**
     * Image width in pixels. Initially, it's an empty string. This value must be
     * assigned from the subclass.
     */
    protected String width = "";


    /**
     * Image height in pixels. Initially, it's an empty string. This value must be
     * assigned from the subclass.
     */
    protected String height = "";

    /**
     * Contains information about the image resolution. Must be assigned from the
     * subclass.
     */
    protected final Resolution resolution = new Resolution();

    /**
     * Color depth specification. Initial value is an empty string. This value must
     * be assigned from the subclass.
     */
    protected String colorDepth = "";

    /**
     * List of the different metadata groups that are written to the report table in
     * the order {@code BASIC}, {@code EXTENDED_INFO}, {@code INT_PRESS_TELCO},
     * {@code EXTENSIBLE}, {@code COLOR_PROFILE} and {@code OTHER}. The prefixes for
     * the table columns of the groups are also defined here.
     */
    protected static enum OtherField {
        /**
         * Basic metadata of the image format. What exactly constitutes “basic” depends on the format.
         */
        BASIC("tag_"),

        /**
         * Extended information embedded according to the EXIF ​​standard.
         */
        EXTENDED_INFO("exif_tag_"),

        /**
         * Metadata according to the standards of the International Press Telecommunications Council (IPTC).
         */
        INT_PRESS_TELCO("iptc_"),

        /**
         * Metadata stored in the Extensible Metadata Platform (XMP).
         */
        EXTENSIBLE("xmp_"),

        /**
         * Metadata extracted from an embedded color profile.
         */
        COLOR_PROFILE("color_pr_"),

        /**
         * Other metadata that could be determined.
         */
        OTHER("");

        private final String prefix; String getPrefix() { return prefix; }
        OtherField(String prefix) { this.prefix = prefix; }
    };
    private EnumMap<OtherField, TreeMap<String,String>> otherFields = new EnumMap<>(OtherField.class);

    /**
     * Creates a new record about a file.
     */
    public FileData() { }

    /**
     * Clone constructor for use by subclasses. The clone constructor only inherits
     * values ​​that should already be defined when the initial class is created.
     * 
     * @param other initialization instance
     */
    protected FileData(FileData other) {
        this.lastModified = other.lastModified;
        this.path = other.path;
        this.size = other.size;
        this.resolution.x = other.resolution.x;
        this.resolution.y = other.resolution.y;
    }

    /**
     * &#x1F511; Returns the full path to the file.
     * 
     * @return path to the file
     */
    public Path getPath() {
        return path;
    }

    /**
     * Returns the file extension in lower case. Returns an empty string if the
     * filename does not contain a dot.
     * 
     * @return the file extension in lower case
     */
    public String getType() {
        String fileName = path.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) return "";
        return fileName.substring(lastDot + 1).toLowerCase();
    }

    /**
     * Returns whether the file extension is that of a TIFF file.
     * 
     * @return whether the file extension of a TIFF is
     */
    public boolean isTiff() {
        return PATTERN_TIFF_CI.matcher(path.getFileName().toString()).matches();
    }

    /**
     * Sets the path to the image file. Set the absolute path.
     * 
     * @param path path to image file
     */
    public void setPath(Path path) {
        this.path = path;
    }

    /**
     * &#x1F511; Returns the time of the last modification of the file.
     * 
     * @return time of last modification
     */
    public Instant getLastModified() {
        return lastModified;
    }

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
     * &#x1F511; Returns the size of the file in bytes.
     * @return file size in bytes
     */
    public long getSize() {
        return size;
    }

    /**
     * Sets the file size in bytes.
     * @param size file size in bytes
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * &#x1F511; Returns the number of pages. PDFs and many image formats can
     * contain multiple pages or frames. If the number of pages is unknown but known
     * to be greater than one, "{@code several}" is returned.
     * 
     * @return number of pages
     */
    public String getNumberOfPages() {
        return numberOfPages;
    }

    /**
     * &#x1F511; Returns the error that occurred while reading the image in human-readable
     * form; otherwise, returns an empty string.
     * 
     * @return reading error, otherwise empty
     */
    public String getException() {
        return readException == null ? "" : readException;
    }

    /**
     * Saves that an error occurred while reading the file.
     * 
     * @param readException error message
     */
    public void setException(String readException) {
        this.readException = readException;
    }

    /**
     * Converts an exception into a human-readable error description and saves that.
     * 
     * @param exception exception
     */
    public void setException(Exception exception) {
        String readableName = Pattern.compile("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])")
            .splitAsStream(exception.getClass().getSimpleName().replaceFirst("Exception$", ""))
            .map(word ->
                word.length() > 1 && word.chars().allMatch(Character::isUpperCase)
                ? word : word.toLowerCase()
            )
            .reduce((first, second) -> first + " " + second)
            .orElse("");
        StringBuilder message = new StringBuilder();
        message.append(readableName.substring(0, 1).toUpperCase());
        message.append(readableName.substring(1));
        if(exception.getMessage() != null) {
            message.append(": ");
            message.append(exception.getMessage());
        }
        readException = exception.toString();
    }

    /**
     * &#x1F511; Returns the image width in pixels.
     * 
     * @return image width in pixels
     */
    public String getWidth() {
        return width;
    }

    /**
     * &#x1F511; Returns the image height in pixels.
     * 
     * @return image height in pixels
     */
    public String getHeight() {
        return height;
    }

    /**
     * &#x1F511; Returns information about the image resolution.
     * 
     * @return the image resolution
     */
    public Resolution getResolution() {
        return resolution;
    }

    /**
     * &#x1F511; Returns the color depth of the image. This is usually an integer in
     * bits per pixel, where 24 describes a typical color image. Smaller numbers
     * indicate a reduced color gamut, larger numbers indicate additional channels,
     * which could originate from a color model for print preparation, or a
     * transparency channel existing. It can also return descriptive text if the
     * image format puts it that way, for example, "color palette".
     * 
     * @return the color depth
     */
    public String getColorDepth() {
        return colorDepth;
    }

    /**
     * &#x1F511; Returns the collection of metadata for a specific group. The
     * collection is arranged in ascending order by key.
     * 
     * @param dataGroup group of metadata
     * @return collection of metadata
     */
    public TreeMap<String, String> getMetadata(OtherField dataGroup) {
        TreeMap<String, String> dataMap = otherFields.get(dataGroup);
        return dataMap != null ? dataMap : EMPTY_TREE_MAP;
    }

    /**
     * Adds metadata to the specified group.
     * 
     * @param dataGroup group of metadata
     * @param key       column heading core, will be prefixed by the group
     * @param value     cell value
     */
    protected void setMetadata(OtherField dataGroup, String key, String value) {
        TreeMap<String, String> groupData = otherFields.get(dataGroup);
        if (groupData == null) otherFields.put(dataGroup, groupData = new TreeMap<>());
        groupData.put(key, value);
    }

    /**
     * Adds metadata to the specified group. This procedure is for the convenience
     * of adding integers, as these occur frequently.
     * 
     * @param dataGroup group of metadata
     * @param key       column heading core, will be prefixed by the group
     * @param value     cell value
     */
    protected void setMetadata(OtherField dataGroup, int key, String value) {
        setMetadata(dataGroup, Integer.toString(key), value);
    }

    /**
     * Adds metadata to the group {@code OTHER}. A prefix should also be specified.
     * 
     * @param prefix prefix indicating the origin of the metadata
     * @param key    key number
     * @param value  cell value
     */
    protected void setMetadata(String prefix, int key, String value) {
        setMetadata(OTHER, prefix.concat(Integer.toString(key)), value);
    }

    /**
     * Adds metadata to the group {@code OTHER}. A prefix should also be specified.
     * 
     * @param prefix prefix indicating the origin of the metadata
     * @param key    descriptive heading, it is converted to lowercase letters and
     *               spaces are replaced with underscores
     * @param value  cell value
     */
    protected void setMetadata(String prefix, String key, String value) {
        setMetadata(OTHER, prefix.concat(key.replace(' ', '_').toLowerCase()), value);
    }
}
