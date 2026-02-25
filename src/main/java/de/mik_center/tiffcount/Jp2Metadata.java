// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.tiffcount;

import com.github.jpeg2000.*;

import static de.mik_center.tiffcount.FileData.OtherField.*;

import java.awt.color.*;
import java.awt.geom.Point2D;

/**
 * Data about a JPEG 2000 image file.
 */
public class Jp2Metadata extends FileData {

    /**
     * Reads metadata from JPEG 2000 files using Faceless2's fork of the "JJ2000"
     * package from the JAI code, and maps it to the unified inner format.
     * 
     * @param aboutFile  basic information about the file
     * @param aboutImage metadata that was extracted
     * @see <A HREF="https://github.com/faceless2/jpeg2000#_R_r9ab_"
     *      TARGET="_blank">JPEG2000</A> (github.com)
     */
    public Jp2Metadata(FileData aboutFile, J2KReader aboutImage) {
        super(aboutFile);
        super.numberOfPages = "1";
        super.width = Integer.toString(aboutImage.getWidth());
        super.height = Integer.toString(aboutImage.getWidth());
        Point2D resc = aboutImage.getCaptureResolution();
        if(resc != null) {
            super.resolution.x = Resolution.formatDpi(resc.getX() / Resolution.INCH_PER_METER);
            super.resolution.y = Resolution.formatDpi(resc.getY() / Resolution.INCH_PER_METER);
        }
        super.colorDepth = Integer.toString(aboutImage.getNumComponents() * aboutImage.getBitsPerComponent());

        ColorSpace colorSpace = aboutImage.getColorSpace();
        if(colorSpace != null) {
            StringBuilder colr = new StringBuilder();
            boolean space = false;
            for(int i = 0; i < colorSpace.getNumComponents(); i++) {
                if(space) colr.append(' '); else space = true;
                colr.append(colorSpace.getName(i));
            }
            super.setMetadata(BASIC, "colr", colr.toString());
        }
        Point2D resd = aboutImage.getDisplayResolution();
        if(resd != null) {
            super.setMetadata(BASIC, "resd_x", Resolution.formatDpi(resd.getX() / Resolution.INCH_PER_METER));
            super.setMetadata(BASIC, "resd_y", Resolution.formatDpi(resd.getY() / Resolution.INCH_PER_METER));
        }
        super.setMetadata(OTHER, "jp2_bytes_per_scanline", Integer.toString(aboutImage.getRowSpan()));
    }
}
