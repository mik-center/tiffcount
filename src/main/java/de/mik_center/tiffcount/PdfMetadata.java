// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.tiffcount;

import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.*;
import org.apache.pdfbox.pdmodel.graphics.*;
import org.apache.pdfbox.pdmodel.graphics.image.*;

/**
 * Data about a PDF file. The PDF files here are in the style of a multi-page
 * image, one image per page, spanning the whole page. Any image information
 * contained refers to the first image, which is on the first page of the PDF.
 */
public class PdfMetadata extends FileData {

    private static final double POINTS_PER_INCH = 72;

    /**
     * Reads some information about PDF files using Apache PDF Box, and maps it to
     * the unified inner format.
     * 
     * @param aboutFile  basic information about the file
     * @param aboutImage metadata that was extracted
     * @see <A HREF="https://pdfbox.apache.org/"
     *      TARGET="_blank">Apache PDFBox® - A Java PDF Library</A> (apache.org)
     */
    public PdfMetadata(FileData aboutFile, PDDocument aboutPdf) {
        super(aboutFile);
        super.numberOfPages = Integer.toString(aboutPdf.getNumberOfPages());

        
        PDPage page = aboutPdf.getPage(0);
        PDRectangle mediaBox = page.getMediaBox();

        try {
            double pageWidthInch = mediaBox.getWidth() / POINTS_PER_INCH;
            double pageHeightInch = mediaBox.getHeight() / POINTS_PER_INCH;
            
            PDResources resources = page.getResources();
    
            Integer imageWidth = null, imageHeight = null;
            for (COSName objectName : resources.getXObjectNames()) {
                PDXObject embeddedObject = resources.getXObject(objectName);
                if (embeddedObject instanceof PDImageXObject) {
                    PDImageXObject embeddedImage = (PDImageXObject) embeddedObject;
                    imageWidth = embeddedImage.getWidth();
                    imageHeight = embeddedImage.getHeight();
                    super.width = Integer.toString(imageWidth);
                    super.height = Integer.toString(imageHeight);
                    break;
                }
            }
            if (imageWidth != null) super.resolution.x
                = Resolution.formatDpi(imageWidth.doubleValue() / pageWidthInch);
            if (imageHeight != null) super.resolution.y
                = Resolution.formatDpi(imageHeight.doubleValue() / pageHeightInch);
        }catch(Exception ignoreErrors) { }
    }
}
