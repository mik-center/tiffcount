// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.tiffcount;

import java.io.*;
import java.nio.file.*;
import static java.nio.file.FileVisitResult.*;
import java.nio.file.attribute.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;
import java.util.Map.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.regex.*;

import com.drew.imaging.*;
import com.drew.metadata.*;

import com.github.jpeg2000.*;
import jj2000.j2k.io.*;

import org.apache.pdfbox.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.encryption.*;

/**
 * &#x1F511; The program that creates the Tiffcount. This runs in its own
 * thread, so it doesn't freeze the GUI window.
 */
public class Tiffcount implements Runnable {
    // Columns for FolderCount files
    private static final String COLUMN_FOLDER_PATH_SEGMENT_PREFIX = "folder";
    private static final String COLUMN_FOLDER_FULL_PATH           = "file_foldr";
    private static final String COLUMN_FOLDER_DATE                = "fldr_date";
    private static final DateTimeFormatter FORMAT_FOLDER_DATE     = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String COLUMN_FOLDER_TIME                = "fldr_time";
    private static final DateTimeFormatter FORMAT_FOLDER_TIME     = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String COLUMN_FOLDER_SIZE_IN_BYTES       = "fldr_size";
    private static final String COLUMN_FOLDER_NUMBER_OF_FILES     = "fldr_files";
    private static final String COLUMN_FOLDER_NUMBER_OF_VOLUMES   = "bd_anzahl";
    private static final String COLUMN_FOLDER_WEEK_OF_YEAR        = "kal_woche";

    // Columns for Tiffcount files
    private static final String COLUMN_FILE_PATH_SEGMENT_PREFIX   = "folder";
    private static final String COLUMN_FILE_DIRECTORY             = "file_foldr";
    private static final String COLUMN_FILE_FILENAME              = "file_name";
    private static final String COLUMN_FILE_FILENAME_LENGTH       = "file_char";
    private static final String COLUMN_FILE_DATE                  = "file_date";
    private static final DateTimeFormatter FORMAT_FILE_DATE       = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String COLUMN_FILE_TIME                  = "file_time";
    private static final DateTimeFormatter FORMAT_FILE_TIME       = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String COLUMN_FILE_SIZE_IN_BYTES         = "file_size";
    private static final String COLUMN_FILE_NUMBER_OF_FILES       = "file_count";
    private static final String COLUMN_FILE_NUMBER_OF_PAGES       = "page_count";
    private static final String COLUMN_FILE_ERROR                 = "error";
    private static final String COLUMN_FILE_WIDTH_IN_PIXELS       = "pixel_x";
    private static final String COLUMN_FILE_HEIGHT_IN_PIXELS      = "pixel_y";
    private static final String COLUMN_FILE_HORIZONTAL_RESOLUTION = "res_x";
    private static final String COLUMN_FILE_VERTICAL_RESOLUTION   = "res_y";
    private static final String COLUMN_FILE_COLOR_DEPTH           = "colordepth";

    private Path             startingPath;
    private Path             reportsPath;
    private Pattern          fileNamePattern;
    private boolean          analyzeImages;
    private Consumer<Double> progressMonitor;

    /**
     * &#x1F511; Creates a new program object to create Tiffcounts.
     * 
     * @param startingPath    directory from which the directories are searched
     * @param reportsPath     directory in which the report is written
     * @param fileNamePattern pattern specifying which files should be counted
     * @param analyzeImages   whether the overview of image metadata should be
     *                        created
     * @param progressMonitor monitor to which progress is communicated, may be
     *                        {@code null}
     */
    public Tiffcount(Path startingPath, Path reportsPath, Pattern fileNamePattern, boolean analyzeImages,
            Consumer<Double> progressMonitor) {
        assert startingPath != null : "'startingPath' must not be null";
        assert reportsPath != null : "'reportsPath' must not be null";
        assert fileNamePattern != null : "'fileNamePattern' must not be null";
        assert Files.isDirectory(startingPath) : "'startingPath' must point to an existing directory";
        assert Files.isDirectory(reportsPath) : "'reportsPath' must point to an existing directory";

        this.startingPath = startingPath;
        this.reportsPath = reportsPath;
        this.fileNamePattern = fileNamePattern;
        this.analyzeImages = analyzeImages;
        this.progressMonitor = progressMonitor;
    }

    /**
     * &#x1F511; Starts the TIFF counting program.
     */
    @Override
    public void run() {
        try {
            if (progressMonitor != null) progressMonitor.accept(0.0);
            String started = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Map<Path, Collection<Path>> foldersAndGoodFiles = new LinkedHashMap<>();
            Map<Path, DirectoryData> allFolderData = new HashMap<>();

            AtomicInteger allRelevantCounter = new AtomicInteger(0);
            Files.walkFileTree(startingPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.getFileName() != null && dir.getFileName().toString().equalsIgnoreCase("$RECYCLE.BIN")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    allRelevantCounter.incrementAndGet();
                    foldersAndGoodFiles.put(dir, new ArrayList<Path>());
                    DirectoryData folderData = new DirectoryData();
                    folderData.setLastModified(attrs.lastModifiedTime().toInstant());
                    allFolderData.put(dir, folderData);
                    sendProgressToGui(foldersAndGoodFiles.size() - 1, 0, allRelevantCounter.get());
                    return CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!attrs.isRegularFile()) return CONTINUE;
                    String fileName = file.getFileName().toString();
                    boolean relevant = fileNamePattern.matcher(fileName).matches();
                    if (!relevant) return CONTINUE;
                    allRelevantCounter.incrementAndGet();
                    foldersAndGoodFiles.get(file.getParent()).add(file);
                    return CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return CONTINUE;
                }
            });
            sendProgressToGui(foldersAndGoodFiles.size(), 0, allRelevantCounter.get());

            List<FileData> imageInfos = new ArrayList<>(allRelevantCounter.get() - foldersAndGoodFiles.size());
            for (Entry<Path, Collection<Path>> folder : foldersAndGoodFiles.entrySet()) {
                for (Path imagePath : folder.getValue()) {
                    FileData imageData = readFileSystemFileProperties(imagePath);
                    if (analyzeImages) imageData = readImagePropertiesGeneralized(imageData);
                    imageInfos.add(imageData);
                    DirectoryData aboutContainingFolder = allFolderData.get(imagePath.getParent());
                    aboutContainingFolder.setSize(aboutContainingFolder.getSize() + imageData.getSize());
                    sendProgressToGui(foldersAndGoodFiles.size(), imageInfos.size(), allRelevantCounter.get());
                }
            }

            String tiffcountName = startingPath.getFileName() != null
                                   ? startingPath.getFileName().toString()
                                   : startingPath.getRoot().toString().substring(0, 1).toUpperCase();
            writeDirectoriesOverviewCsv(allFolderData, foldersAndGoodFiles,
                reportsPath.resolve(tiffcountName + "-" + started + "_FolderCountExtra.csv"));

            if (analyzeImages)
                writeImageDetailsCsv(imageInfos, reportsPath.resolve(tiffcountName + "-" + started + "_TiffCount.csv"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static FileData readFileSystemFileProperties(Path imagePath) throws IOException {
        FileData fileData = new FileData();
        fileData.setPath(imagePath);
        fileData.setLastModified(Files.getLastModifiedTime(imagePath).toInstant());
        fileData.setSize(Files.size(imagePath));
        return fileData;
    }

    /* Reads the image metadata. Returns a subclass of the FileData object in case
     * of success, else the 'in' object with the exception set. */
    private static FileData readImagePropertiesGeneralized(FileData in) {
        assert in != null : "'in' must not be null";
        assert in.getPath() != null : "'in.getPath()' must not return null";

        try {
            switch (in.getType()) {
            case "j2k": case "jp2": case "jpf": case "jpg2":
            case "jpm": case "jpx": case "mj2": case "mjp2":
                return useFaceless2sJpeg2000Reader(in);
            case "pdf":
                return useApachePdfBox(in);
            default:
                return useDrewNoakesMetadataExtractor(in);
            }
        } catch (InvalidPasswordException e) {
            in.setException(e.getMessage().replace(", the password is incorrect", ""));
        } catch (IOException e) {
            String message = e.getMessage();
            if (message != null && message.startsWith("Error: "))
                in.setException(message.replace("Error: ", ""));
            else if (message != null && !message.isBlank())
                in.setException(message);
            else
                in.setException(e);
        } catch (Exception errorBarrier) {
            in.setException(errorBarrier);
        }
        return in;
    }

    private static FileData useDrewNoakesMetadataExtractor(FileData in) throws IOException {
        assert in != null : "'in' must not be null";
        assert in.getPath() != null : "'in.getPath()' must not return null";

        try (InputStream inputStream = Files.newInputStream(in.getPath())) {
            Metadata metadata = ImageMetadataReader.readMetadata(inputStream);
            return new FullImageMetadata(in, metadata);
        } catch (ImageProcessingException e) {
            in.setException(e);
            return in;
        }
    }

    private static Jp2Metadata useFaceless2sJpeg2000Reader(FileData in) throws IOException {
        assert in != null : "'in' must not be null";
        assert in.getPath() != null : "'in.getPath()' must not return null";

        J2KFile j2kFile = new J2KFile();
        BEBufferedRandomAccessFile fileAccessor = null;
        try {
            fileAccessor = new BEBufferedRandomAccessFile(in.getPath().toFile(), "r", 8192);
            j2kFile.read(fileAccessor);
            try (J2KReader j2kMetadata = new J2KReader(j2kFile)) {
                return new Jp2Metadata(in, j2kMetadata);
            } // j2kMetadata.close();
        } finally {
            if (fileAccessor != null) fileAccessor.close();
        }
    }

    private static FileData useApachePdfBox(FileData in) throws IOException {
        assert in != null : "'in' must not be null";
        assert in.getPath() != null : "'in.getPath()' must not return null";

        PDDocument pdfFile = Loader.loadPDF(in.getPath().toFile());
        return new PdfMetadata(in, pdfFile);
    }

    private static void writeDirectoriesOverviewCsv(Map<Path, DirectoryData> allFolderData,
            Map<Path, Collection<Path>> foldersAndGoodFiles, Path fileToWrite) throws IOException {
        assert allFolderData != null : "'allFolderData' must not be null";
        assert foldersAndGoodFiles != null : "'foldersAndGoodFiles' must not be null";
        assert fileToWrite != null : "'fileToWrite' must not be null";

        // calculate number of 'folder' columns
        int numberOfFolderColumns = 0;
        for (Path folder : foldersAndGoodFiles.keySet()) {
            int count = folder.getNameCount();
            if (count > numberOfFolderColumns) numberOfFolderColumns = count;
        }

        // create table with columns
        Table folderCount = new Table();
        for (int i = 1; i <= numberOfFolderColumns; i++) folderCount.add(COLUMN_FOLDER_PATH_SEGMENT_PREFIX + i);
        folderCount.add
            (COLUMN_FOLDER_FULL_PATH,       COLUMN_FOLDER_DATE,             COLUMN_FOLDER_TIME,
             COLUMN_FOLDER_SIZE_IN_BYTES,   COLUMN_FOLDER_NUMBER_OF_FILES,  COLUMN_FOLDER_NUMBER_OF_VOLUMES,
             COLUMN_FOLDER_WEEK_OF_YEAR);

        // fill table
        int row = 0;
        for (Path folder : foldersAndGoodFiles.keySet()) {
            DirectoryData folderData = allFolderData.get(folder);

            for (int i = 0; i < folder.getNameCount(); i++) {
                folderCount.set(row, COLUMN_FOLDER_PATH_SEGMENT_PREFIX + (i + 1), folder.getName(i).toString());
            }
            String full_path = folder.toString();
            if (!full_path.endsWith(File.separator))
                full_path += File.separator;
            folderCount.set(row, COLUMN_FOLDER_FULL_PATH, full_path);
            ZonedDateTime folderTime = folderData.getLastModified().atZone(ZoneId.systemDefault());
            folderCount.set(row, COLUMN_FOLDER_DATE, folderTime.format(FORMAT_FOLDER_DATE));
            String time = folderTime.format(FORMAT_FOLDER_TIME);
            if (time.startsWith("0"))
                time = " " + time.substring(1);
            folderCount.set(row, COLUMN_FOLDER_TIME, time);
            folderCount.set(row, COLUMN_FOLDER_SIZE_IN_BYTES, folderData.getSize());
            folderCount.set(row, COLUMN_FOLDER_NUMBER_OF_FILES, foldersAndGoodFiles.get(folder).size());
            folderCount.set(row, COLUMN_FOLDER_NUMBER_OF_VOLUMES, 1);
            folderCount.set(row, COLUMN_FOLDER_WEEK_OF_YEAR, folderTime.get(WeekFields.ISO.weekOfWeekBasedYear()));
            row++;
        }

        // write file
        folderCount.toCSV(fileToWrite);
    }

    private static void writeImageDetailsCsv(List<FileData> imageInfos, Path fileToWrite) throws IOException {
        assert imageInfos != null : "'imageInfos' must not be null";
        assert fileToWrite != null : "'fileToWrite' must not be null";

        // calculate number of 'folder' columns
        int numberOfFolderColumns = 0;
        for (FileData imageData : imageInfos) {
            int count = imageData.getPath().getNameCount();
            if (count > numberOfFolderColumns) numberOfFolderColumns = count;
        }

        // create table with columns
        Table tiffCount = new Table();
        for (int i = 1; i <= numberOfFolderColumns; i++) tiffCount.add(COLUMN_FOLDER_PATH_SEGMENT_PREFIX + i);
        tiffCount.add(
            COLUMN_FILE_DIRECTORY,              COLUMN_FILE_FILENAME,           COLUMN_FILE_FILENAME_LENGTH,
            COLUMN_FILE_DATE,                   COLUMN_FILE_TIME,               COLUMN_FILE_SIZE_IN_BYTES,
            COLUMN_FILE_NUMBER_OF_FILES,        COLUMN_FILE_NUMBER_OF_PAGES,    COLUMN_FILE_ERROR,
            COLUMN_FILE_WIDTH_IN_PIXELS,        COLUMN_FILE_HEIGHT_IN_PIXELS,   COLUMN_FILE_HORIZONTAL_RESOLUTION,
            COLUMN_FILE_VERTICAL_RESOLUTION,    COLUMN_FILE_COLOR_DEPTH);
        for (FileData.OtherField dataGroup : FileData.OtherField.values()) {
            Set<String> allCaptions = new TreeSet<>();
            for (FileData fileData : imageInfos) allCaptions.addAll(fileData.getMetadata(dataGroup).keySet());
            for (String caption : allCaptions) tiffCount.add(dataGroup.getPrefix().concat(caption));
        }

        // fill table
        int numberOfLines = imageInfos.size();
        for (int row = 0; row < numberOfLines; row++) {
            FileData image = imageInfos.get(row);
            Path directory = image.getPath().getParent();
            for (int i = 0; i < directory.getNameCount(); i++) {
                tiffCount.set(row, COLUMN_FILE_PATH_SEGMENT_PREFIX + (i + 1), directory.getName(i).toString());
            }
            String trailingSepDir = directory.toString();
            if (!trailingSepDir.endsWith(File.separator)) trailingSepDir += File.separator;
            tiffCount.set(row, COLUMN_FILE_DIRECTORY, trailingSepDir);
            String fileName = image.getPath().getFileName().toString();
            tiffCount.set(row, COLUMN_FILE_FILENAME, fileName);
            tiffCount.set(row, COLUMN_FILE_FILENAME_LENGTH, fileName.length());
            ZonedDateTime imageTime = image.getLastModified().atZone(ZoneId.systemDefault());
            tiffCount.set(row, COLUMN_FILE_DATE, imageTime.format(FORMAT_FILE_DATE));
            String time = imageTime.format(FORMAT_FILE_TIME);
            if (time.startsWith("0")) time = " " + time.substring(1);
            tiffCount.set(row, COLUMN_FILE_TIME, time);
            tiffCount.set(row, COLUMN_FILE_SIZE_IN_BYTES, image.getSize());
            tiffCount.set(row, COLUMN_FILE_NUMBER_OF_FILES, 1);
            tiffCount.set(row, COLUMN_FILE_NUMBER_OF_PAGES, image.getNumberOfPages());
            tiffCount.set(row, COLUMN_FILE_ERROR, image.getException());

            tiffCount.set(row, COLUMN_FILE_WIDTH_IN_PIXELS, image.getWidth());
            tiffCount.set(row, COLUMN_FILE_HEIGHT_IN_PIXELS, image.getHeight());
            tiffCount.set(row, COLUMN_FILE_HORIZONTAL_RESOLUTION, image.getResolution().getX());
            tiffCount.set(row, COLUMN_FILE_VERTICAL_RESOLUTION, image.getResolution().getY());
            tiffCount.set(row, COLUMN_FILE_COLOR_DEPTH, image.getColorDepth());

            for (FileData.OtherField dataGroup : FileData.OtherField.values()) {
                for (Entry<String, String> otherData : image.getMetadata(dataGroup).entrySet()) {
                    String columnName = dataGroup.getPrefix().concat(otherData.getKey());
                    tiffCount.set(row, columnName, otherData.getValue());
        }   }   }

        // write file
        tiffCount.toCSV(fileToWrite);
    }

    private void sendProgressToGui(double processedDirs, double processedFiles, double total) {
        if (progressMonitor != null) progressMonitor.accept((processedDirs + processedFiles) / total);
    }
}
