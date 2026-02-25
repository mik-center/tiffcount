// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.tiffcount;

import static de.mik_center.tiffcount.FileData.OtherField.*;
import static java.lang.System.*;

import java.util.*;
import java.util.Map.*;
import java.util.regex.*;
import java.util.stream.*;

import com.drew.metadata.*;
import com.drew.metadata.adobe.*;
import com.drew.metadata.exif.*;
import com.drew.metadata.gif.*;
import com.drew.metadata.icc.*;
import com.drew.metadata.iptc.*;
import com.drew.metadata.jfif.*;
import com.drew.metadata.jpeg.*;
import com.drew.metadata.photoshop.*;
import com.drew.metadata.png.*;
import com.drew.metadata.xmp.*;

/**
 * Data about an image file (raster graphic). The file format was known, so
 * details could be extracted from the image file.
 */
public class FullImageMetadata extends FileData {
    private static final Pattern SPACE = Pattern.compile(" ");

    /**
     * Reads all metadata using Drew Noakes' metadata extractor library and maps it
     * to the unified inner format.
     * 
     * @param aboutFile  basic information about the file
     * @param aboutImage metadata that was extracted
     * @see <A HREF="https://github.com/drewnoakes/metadata-extractor?tab=readme-ov-file#usage"
     *      TARGET="_blank">metadata-extractor</A> (github.com)
     */
    public FullImageMetadata(FileData aboutFile, Metadata aboutImage) {
        super(aboutFile);
        for (Directory directory : aboutImage.getDirectories()) {
            // _test_print_object_(aboutFile, directory);
            String directoryName = directory.getName();

            // Maker Notes are rare and diverse, that's why it's worth opening a separate can of worms here
            if(directory.getClass().getPackageName().equals("com.drew.metadata.exif.makernotes")) {
                mapMakernoteDirectories(directory);
                continue;
            }

            switch (directoryName) {
                case "File Type": break; // libraries' file type guessing

                // TIFF
                case "Exif IFD0": // is used for TIFF first image base *and* JPEG EXIF base
                    if(aboutFile.isTiff())
                        mapTiffDirectory((ExifIFD0Directory) directory);
                        else mapExifDirectory(((ExifIFD0Directory) directory));
                break;
                case "Exif Image": mapMultipageTiffDirectory((ExifImageDirectory) directory); break;

                // GIF
                case "GIF Header": mapGifHeaderDirectory((GifHeaderDirectory) directory); break;
                case "GIF Control": mapOtherDirectory((GifControlDirectory) directory, "gif_ctrl_", false); break;
                case "GIF Image": mapOtherDirectory((GifImageDirectory) directory, "gif_", false); break;
                case "GIF Comment": mapOtherDirectory((GifCommentDirectory) directory, "gif_", false); break;
                case "GIF Animation":
                    super.numberOfPages = "several";
                    mapOtherDirectory((GifAnimationDirectory) directory, "gif-ani_", true);
                break;

                // JPEG
                case "JPEG": mapJpegDirectory((JpegDirectory) directory); break;
                case "JFIF": mapJfifDirectory((JfifDirectory) directory); break;
                case "JpegComment": mapOtherDirectory((JpegCommentDirectory) directory, "", false); break;
                
                case "Adobe JPEG": mapOtherDirectory((AdobeJpegDirectory) directory, "adobe_", true); break;
                case "Ducky": mapOtherDirectory((DuckyDirectory) directory, "ducky_", false); break;
                case "Huffman": break; // JPEG compression internals

                // PNG
                case "PNG-IHDR": mapPngDirectoryIhdr((PngDirectory) directory); break;
                case "PNG-pHYs": mapPngDirectoryPhys((PngDirectory) directory); break;
                case "PNG-tIME": mapPngDirectoryTime((PngDirectory) directory); break;
                case "PNG-iCCP": mapPngDirectoryIccp((PngDirectory) directory); break;
                case "PNG Chromaticities": mapOtherDirectory((PngChromaticitiesDirectory) directory, "png_chroma_", false); break;
                case "PNG-tRNS": mapOtherDirectory((PngDirectory) directory, "png_transparency_", true); break;
                case "PNG-bKGD": mapOtherDirectory((PngDirectory) directory, "png_bkgd_", true); break;
                case "PNG-gAMA": mapOtherDirectory((PngDirectory) directory, "png_gama_", true); break;
                case "PNG-PLTE": mapPngDirectoryPalette((PngDirectory) directory); break;
                case "PNG-sBIT": mapOtherDirectory((PngDirectory) directory, "png_sbit_", true); break;
                case "PNG-sRGB": mapOtherDirectory((PngDirectory) directory, "png_", false); break;
                case "PNG-tEXt": mapOtherDirectory((PngDirectory) directory, "png_text_", true); break;
                case "PNG-iTXt": mapOtherDirectory((PngDirectory) directory, "png_", false); break;
                case "PNG-zTXt": break; // extra binary data

                // metadata
                case "Exif SubIFD": mapExifDirectory(((ExifSubIFDDirectory) directory)); break;
                case "Exif Thumbnail": mapOtherDirectory((ExifThumbnailDirectory) directory, "thumb_", false); break;
                case "Interoperability": mapOtherDirectory((ExifInteropDirectory) directory, "inter_", false); break;
                case "IPTC": mapIptcDirectory((IptcDirectory) directory); break;
                case "XMP": mapXMPDirectory((XmpDirectory) directory); break;
                case "ICC Profile": mapIccDirectory((IccDirectory) directory); break;
                case "GPS": mapOtherDirectory((GpsDirectory) directory, "", false); break;
                case "Photoshop": mapOtherDirectory((PhotoshopDirectory) directory, "pshop_", false); break;
                case "PrintIM": mapOtherDirectory((PrintIMDirectory) directory, "print_im_", true); break;

                // errors
                case "Error": break; // handled after switch

                default:
                    RuntimeException unmappedDirectoryException = new IllegalStateException(
                        '"' + directory.getName() + "\" (" + directory.getClass().getName() + ')'
                    );
                throw unmappedDirectoryException;
            }
            if(directory.hasErrors()) super.setException(String.join(", ", directory.getErrors()));
        }
    }

    private void mapTiffDirectory(ExifDirectoryBase directory) {
        if(super.numberOfPages.isBlank()) super.numberOfPages = "1";
        for(Tag tag:directory.getTags()) {
            String value = tag.getDescription();
            switch(tag.getTagName()) {
                case "Image Width": super.width = value.replaceFirst(" pixels$", ""); break;
                case "Image Height": super.height = value.replaceFirst(" pixels$", ""); break;
                case "X Resolution": super.resolution.x = value.replaceFirst(" dots per inch$", " dpi"); break;
                case "Y Resolution": super.resolution.y = value.replaceFirst(" dots per inch$", " dpi"); break;
                case "Bits Per Sample": try {
                        Stream<String> values = SPACE.splitAsStream(value.replaceFirst(" bits/component/pixel$", ""));
                        super.colorDepth = Integer.toString(values .mapToInt(Integer::valueOf).sum());
                    } catch (RuntimeException e) {
                        super.colorDepth = value;
                    }
                break;
                case "Strip Byte Counts": break; // pixel data
                case "Strip Offsets": break; // pixel data
                default:
                    super.setMetadata(BASIC, tag.getTagType(), value);
                break;
            }
        }
    }

    private void mapMultipageTiffDirectory(ExifImageDirectory directory) {
        for(Tag tag:directory.getTags()) {
            String value = tag.getDescription();
            switch(tag.getTagName()) {
                case "New Subfile Type":
                    if (value.equals("Single page of multi-page image")) {
                        super.numberOfPages = "several";
                    }
                continue;
                case "Strip Byte Counts": continue; // pixel data
                case "Strip Offsets": continue; // pixel data
            }
            super.setMetadata(EXTENDED_INFO, tag.getTagType(), value);
        }
    }

    private void mapGifHeaderDirectory(GifHeaderDirectory directory) {
        assert super.numberOfPages == "" : "GIF already defined";
        super.numberOfPages = "1";
    
        for(Tag tag:directory.getTags()) {
            String value = tag.getDescription();
            switch(tag.getTagName()) {
                case "Image Width": super.width = value; break;
                case "Image Height": super.height = value; break;
                case "Bits per Pixel": super.colorDepth = value; break;
                default:
                    super.setMetadata("gif_", tag.getTagName(), value);
                break;
            }
        }
    }

    private void mapJpegDirectory(JpegDirectory directory) {
        assert super.numberOfPages == "" : "JPEG already defined";
        super.numberOfPages = "1";
    
        for(Tag tag:directory.getTags()) {
            String value = tag.getDescription();
            switch(tag.getTagName()) {
                case "Image Width": super.width = value.replaceFirst(" pixels$", ""); break;
                case "Image Height": super.height = value.replaceFirst(" pixels$", ""); break;
                default: super.setMetadata("jpeg_", tag.getTagName(), value); break;
            }
        }
    }

    private void mapJfifDirectory(JfifDirectory directory) {
        if (super.numberOfPages == "") throw new IllegalStateException("No image");
        if (!super.numberOfPages.equals("1")) return; // do not extract JFIF data for subsequent images
    
        String xResolution = null, yResolution = null, unit = null;
        for (Tag tag : directory.getTags()) {
            switch (tag.getTagName()) {
                case "Resolution Units": unit = tag.getDescription(); break;
                case "X Resolution": xResolution = tag.getDescription(); break;
                case "Y Resolution": yResolution = tag.getDescription(); break;
                default: super.setMetadata("jfif_", tag.getTagName(), tag.getDescription()); break;
            }
        }
        if (unit != null && !unit.equals("none")) {
            if (xResolution != null) xResolution += " per " + unit;
            if (yResolution != null) yResolution += " per " + unit;
        }
        if (xResolution != null) super.resolution.x = xResolution.replaceFirst(" dots per inch$", " dpi");
        if (yResolution != null) super.resolution.y = yResolution.replaceFirst(" dots per inch$", " dpi");
    }

    private void mapPngDirectoryIhdr(PngDirectory directory) {
        if (super.numberOfPages == "") { // first image
            super.numberOfPages = "1";
            for(Tag tag:directory.getTags()) {
                String value = tag.getDescription();
                switch(tag.getTagName()) {
                    case "Image Width": super.width = value; break;
                    case "Image Height": super.height = value; break;
                    case "Color Type": super.colorDepth = value.equals("True Color") ? "24" : value; break;
                    default: super.setMetadata(BASIC, tag.getTagType(), value); break;
                }
            }
        } else { // multiple images (mng / apng)
            super.numberOfPages = Integer.toString(Integer.valueOf(super.numberOfPages) + 1);
        }
    }

    private void mapPngDirectoryPhys(PngDirectory directory) {
        if (super.numberOfPages == "") throw new IllegalStateException("No image");
        if (!super.numberOfPages.equals("1")) return; // do not extract JFIF data for subsequent images
    
        String xResolution = null, yResolution = null, unit = null;
        for (Tag tag : directory.getTags()) {
            switch (tag.getTagName()) {
                case "Unit Specifier": unit = tag.getDescription(); break;
                case "Pixels Per Unit X": xResolution = tag.getDescription(); break;
                case "Pixels Per Unit Y": yResolution = tag.getDescription(); break;
                default:
                    super.setMetadata("png_", tag.getTagName(), tag.getDescription());
                break;
            }
        }
        if ("Metres".equals(unit)) {
            if (xResolution != null) super.resolution.x
                = Resolution.formatDpi(Integer.valueOf(xResolution).doubleValue() / Resolution.INCH_PER_METER);
            if (yResolution != null) super.resolution.y
                = Resolution.formatDpi(Integer.valueOf(yResolution).doubleValue() / Resolution.INCH_PER_METER);
            return;
        }
        if (unit != null && !unit.equals("none")) {
            if (xResolution != null) xResolution += " dots per " + unit;
            if (yResolution != null) yResolution += " dots per " + unit;
        }
        if (xResolution != null) super.resolution.x = xResolution.replaceFirst(" dots per [Ii]nch(?:es)?$", " dpi");
        if (yResolution != null) super.resolution.y = yResolution.replaceFirst(" dots per [Ii]nch(?:es)?$", " dpi");
    }

    private void mapPngDirectoryTime(PngDirectory directory) {
        if (super.numberOfPages == "") throw new IllegalStateException("No image");
        if (!super.numberOfPages.equals("1")) return; // do not extract data for subsequent images
    
        for (Tag tag : directory.getTags()) {
            if(tag.getTagName().equals("Last Modification Time")) {
                super.setMetadata(BASIC, 306, tag.getDescription()); // TIFF field 306
                continue;
            }
            super.setMetadata("png_time_", tag.getTagName(), tag.getDescription());
        }
    }

    private void mapPngDirectoryPalette(PngDirectory directory) {
        if (super.numberOfPages == "") throw new IllegalStateException("No image");
        if (!super.numberOfPages.equals("1")) return; // do not extract data for subsequent images
    
        for (Tag tag : directory.getTags()) {
            if(tag.getTagName().equals("Palette Size")) try {
                super.colorDepth = Integer.toString(
                    31 - Integer.numberOfLeadingZeros(Integer.valueOf(tag.getDescription()))
                );
            } catch (RuntimeException e) { }
            super.setMetadata("png_plte_", tag.getTagName(), tag.getDescription());
        }
    }

    private void mapExifDirectory(ExifDirectoryBase directory) {
        for (Tag tag : directory.getTags()) {
            if(tag.getDescription() != null) super.setMetadata(EXTENDED_INFO,
                tag.getTagType(), tag.getDescription().replaceFirst(" dots per inch$", " dpi"));
        }
    }

    private void mapIptcDirectory(IptcDirectory directory) {
        if (super.numberOfPages == "") throw new IllegalStateException("No image");
        if (!super.numberOfPages.equals("1")) return; // do not extract IPTC data for subsequent images

        for(Tag tag:directory.getTags()) {
            super.setMetadata(INT_PRESS_TELCO, tag.getTagType(), tag.getDescription());
        }
    }

    private void mapXMPDirectory(XmpDirectory directory) {
        if (super.numberOfPages == "") throw new IllegalStateException("No image");
        if (!super.numberOfPages.equals("1")) return; // do not extract data for subsequent images

        for (Entry<String, String> entry : ((XmpDirectory) directory).getXmpProperties().entrySet()) {
            StringBuilder captionBuilder = new StringBuilder();
            boolean separator = false;
            for (String part : entry.getKey().split("/")) {
                if (separator) captionBuilder.append('_'); else separator = true;
                captionBuilder.append(part.replaceAll("^.*:", "").replace("[", "").replace("]", "").toLowerCase());
            }
            super.setMetadata(EXTENSIBLE, captionBuilder.toString(), entry.getValue());
        }
    }

    private void mapIccDirectory(IccDirectory directory) {
        if (super.numberOfPages == "") throw new IllegalStateException("No image");
        if (!super.numberOfPages.equals("1")) return; // do not extract data for subsequent images

        for(Tag tag:directory.getTags()) {
            String tagName = tag.getTagName();
            if(tagName.equals("XYZ values")|| tagName.equals("Media White Point")
                    || tagName.endsWith(" TRC") || tagName.endsWith(" Colorant")
                    || tagName.startsWith("Unknown tag "))
                { continue; } // data
            super.setMetadata(COLOR_PROFILE, tagName.replace("/", ""),
                tag.getDescription().replaceFirst("^1 enUS\\((.*)\\)$", "$1"));
        }
    }

    private void mapPngDirectoryIccp(PngDirectory directory) {
        if (super.numberOfPages == "") throw new IllegalStateException("No image");
        if (!super.numberOfPages.equals("1")) return; // do not extract data for subsequent images

        for(Tag tag:directory.getTags()) {
            super.setMetadata(COLOR_PROFILE, tag.getTagName().replaceFirst("^ICC ", ""), tag.getDescription());
        }
    }

    private void mapMakernoteDirectories(Directory directory) {
        String prefix = directory.getName().replaceFirst("^([A-Za-z]+).*$", "$1").toLowerCase().concat("_");
        for (Tag tag : directory.getTags()) super.setMetadata(prefix, tag.getTagType(), tag.getDescription());
    }

    private void mapOtherDirectory(Directory directory, String prefix, boolean numeric) {
        for (Tag tag : directory.getTags()) {
            if (numeric) super.setMetadata(prefix, tag.getTagType(), tag.getDescription());
                else super.setMetadata(prefix, tag.getTagName(), tag.getDescription());
        }
    }

    @SuppressWarnings("unused")
    private static void _test_print_object_ (FileData fileData, Directory directory) {
        out.println(fileData.getPath());
        out.println('"' + directory.getName() + "\" " + directory.getClass());
        out.println();

        if (!(directory instanceof XmpDirectory)) {
            for (Tag tag : directory.getTags()) {
                out.println("  \"" + tag.getTagName() + "\" (" + tag.getTagType() + ')');
                out.println("  ›" + Table.quoteNonDigitContent(Objects.toString(tag.getDescription())) + '‹');
                out.println();
            }
        } else {
            for (Entry<String, String> xx : ((XmpDirectory) directory).getXmpProperties().entrySet()) {
                out.println("> \"" + xx.getKey() + '"');
                out.println("  \"" + xx.getValue() + '"');
                out.println();
            }
        }
    }
}
