package reposense.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import reposense.authorship.model.AuthorshipSummary;
import reposense.commits.model.CommitContributionSummary;
import reposense.model.Author;
import reposense.model.RepoConfiguration;
import reposense.parser.AuthorAdapter;
import reposense.system.LogsManager;

public class FileUtil {

    // zip file which contains all the specified file types
    public static final String ZIP_FILE = "archive.zip";

    private static final Logger logger = LogsManager.getLogger(FileUtil.class);
    private static final String GITHUB_API_DATE_FORMAT = "yyyy-MM-dd";
    private static final ByteBuffer buffer = ByteBuffer.allocate(1 << 11); // 2KB

    public static void writeJsonFile(Object object, String path) {
        Gson gson = new GsonBuilder()
                .setDateFormat(GITHUB_API_DATE_FORMAT)
                .setPrettyPrinting()
                .create();
        String result = gson.toJson(object);

        try (PrintWriter out = new PrintWriter(path)) {
            out.print(result);
            out.print("\n");
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public static <T> T fromJson(Path path, Type type) throws FileNotFoundException {
        return fromJson(new Gson(), path, type);
    }

    public static <T> T fromJson(Gson gson, Path path, Type type) throws FileNotFoundException {
        JsonReader jsonReader = new JsonReader(new FileReader(path.toString()));
        return gson.fromJson(jsonReader, type);
    }

    public static <T> T fromJson(Gson gson, JsonElement jsonElement, Type type) throws FileNotFoundException {
        return gson.fromJson(jsonElement, type);
    }

    public static JsonObject fromJson(Path path) throws FileNotFoundException {
        JsonReader jsonReader = new JsonReader(new FileReader(path.toString()));
        JsonParser parser = new JsonParser();
        return parser.parse(jsonReader).getAsJsonObject();
    }

    public static <T> T fromJson(JsonElement jsonElement, Type type) throws FileNotFoundException {
        return fromJson(new Gson(), jsonElement, type);
    }

    public static String getRepoDirectory(String org, String repoName) {
        return Constants.REPOS_ADDRESS + File.separator + org + File.separator + repoName + File.separator;
    }

    public static void deleteDirectory(String root) throws IOException {
        Path rootPath = Paths.get(root);
        if (Files.exists(rootPath)) {
            Files.walk(rootPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(filePath -> filePath.toFile().delete());
        }
    }

    /**
     * Zips all the files of {@code fileTypes} contained in {@code sourceAndOutputPath} directory into the same folder.
     */
    public static void zip(Path sourceAndOutputPath, String... fileTypes) {
        FileUtil.zip(sourceAndOutputPath, sourceAndOutputPath, fileTypes);
    }

    /**
     * Zips all the {@code fileTypes} files contained in the {@code sourcePath} and its subdirectories.
     * Creates the zipped {@code ZIP_FILE} file in the {@code outputPath}.
     */
    public static void zip(Path sourcePath, Path outputPath, String... fileTypes) {
        try (
                FileOutputStream fos = new FileOutputStream(outputPath + File.separator + ZIP_FILE);
                ZipOutputStream zos = new ZipOutputStream(fos)
        ) {
            Set<Path> allFiles = getFilePaths(sourcePath, fileTypes);
            for (Path path : allFiles) {
                String filePath = sourcePath.relativize(path.toAbsolutePath()).toString();
                String zipEntry = Files.isDirectory(path) ? filePath + File.separator : filePath;
                zos.putNextEntry(new ZipEntry(zipEntry.replace("\\", "/")));
                if (Files.isRegularFile(path)) {
                    try (InputStream is = Files.newInputStream(path)) {
                        int length;
                        while ((length = is.read(buffer.array())) > 0) {
                            zos.write(buffer.array(), 0, length);
                        }
                    }
                }
                zos.closeEntry();
            }
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, ioe.getMessage(), ioe);
        }
    }

    /**
     * Unzips the contents of the {@code zipSourcePath} into {@code outputPath}.
     * @throws IOException if {@code zipSourcePath} is an invalid path.
     */
    public static void unzip(Path zipSourcePath, Path outputPath) throws IOException {
        try (InputStream is = Files.newInputStream(zipSourcePath)) {
            unzip(is, outputPath);
        }
    }

    /**
     * Unzips the contents of the {@code is} into {@code outputPath}.
     * @throws IOException if {@code is} refers to an invalid path.
     */
    public static void unzip(InputStream is, Path outputPath) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            Files.createDirectories(outputPath);
            while ((entry = zis.getNextEntry()) != null) {
                Path path = Paths.get(outputPath.toString(), entry.getName());
                // create the directories of the zip directory
                if (entry.isDirectory()) {
                    Files.createDirectories(path.toAbsolutePath());
                    zis.closeEntry();
                    continue;
                }
                if (!Files.exists(path.getParent())) {
                    Files.createDirectories(path.getParent());
                }
                try (OutputStream output = Files.newOutputStream(path)) {
                    int length;
                    while ((length = zis.read(buffer.array())) > 0) {
                        output.write(buffer.array(), 0, length);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * Copies the template files from {@code sourcePath} to the {@code outputPath}.
     * @throws IOException if {@code is} refers to an invalid path.
     */
    public static void copyTemplate(InputStream is, String outputPath) throws IOException {
        FileUtil.unzip(is, Paths.get(outputPath));
    }

    /**
     * Creates the {@code dest} directory if it does not exist.
     */
    public static void createDirectory(Path dest) throws IOException {
        Files.createDirectories(dest);
    }

    /**
     * Returns a list of {@code Path} of {@code fileTypes} contained in the given {@code directoryPath} directory.
     */
    private static Set<Path> getFilePaths(Path directoryPath, String... fileTypes) throws IOException {
        return Files.walk(directoryPath)
                .filter(p -> FileUtil.isFileTypeInPath(p, fileTypes) || Files.isDirectory(p))
                .collect(Collectors.toSet());
    }

    /**
     * Returns true if the {@code path} contains one of the {@code fileTypes} extension.
     */
    private static boolean isFileTypeInPath(Path path, String... fileTypes) {
        return Arrays.stream(fileTypes).anyMatch(path.toString()::endsWith);
    }

    private static String attachJsPrefix(String original, String prefix) {
        return "var " + prefix + " = " + original;
    }

    public static List<RepoConfiguration> getRepoConfigsFromJson(Path reposenseReportFolderPath)
            throws FileNotFoundException {
        Path summaryJsonPath = Paths.get(reposenseReportFolderPath.toString(), "summary.json");
        Type type = new TypeToken<List<RepoConfiguration>>(){}.getType();
        return FileUtil.fromJson(summaryJsonPath, type);
    }

    public static List<AuthorshipSummary> getAuthorshipFromJson(Path reposenseReportFolderPath, String folder)
            throws FileNotFoundException {
        Path authorshipJsonPath = Paths.get(reposenseReportFolderPath.toString(), folder, "authorship.json");
        Type type = new TypeToken<List<AuthorshipSummary>>(){}.getType();
        return FileUtil.fromJson(authorshipJsonPath, type);
    }

    public static CommitContributionSummary getCommitContributionSummaryFromJson(
            Path reposenseReportFolderPath, String folder) throws FileNotFoundException {
        Path commitsJsonPath = Paths.get(reposenseReportFolderPath.toString(), folder, "commits.json");
        Type type = new TypeToken<CommitContributionSummary>(){}.getType();

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Author.class, new AuthorAdapter());
        Gson gson = builder.create();

        return FileUtil.fromJson(gson, commitsJsonPath, type);
    }

    public static Map<Author, Float> getAuthorContributionVarianceFromJson(
            Path reposenseReportFolderPath, String folder) throws FileNotFoundException {
        Path commitsJsonPath = Paths.get(reposenseReportFolderPath.toString(), folder, "commits.json");
        JsonObject jsonObject = FileUtil.fromJson(commitsJsonPath);
        JsonElement jsonElement = jsonObject.get("authorFinalContributionMap");

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Author.class, new AuthorAdapter());
        Gson gson = builder.create();

        Type type = new TypeToken<Map<Author, Float>>(){}.getType();
        return FileUtil.fromJson(gson, jsonElement, type);
    }

    public static boolean isFileExists(PathMatcher matcher, Path path) {
        return Files.exists(path) && Files.isReadable(path) && Files.isRegularFile(path)
                && matcher.matches(path.getFileName());
    }

    public static boolean isFolderAndContainsExpectedFiles(
            PathMatcher matcher, Path path, int expectedFileCount, boolean exactExpectedFileCount) {
        if (!Files.exists(path) || !Files.isReadable(path) || !Files.isDirectory(path)) {
            return false;
        }

        try (DirectoryStream<Path> files = Files.newDirectoryStream(path)) {
            int matchCount = 0;

            for (Path p : files) {
                if (matcher.matches(p.getFileName())) {
                    matchCount++;
                }
            }

            if (exactExpectedFileCount) {
                return matchCount == expectedFileCount;
            }

            return matchCount <= expectedFileCount;
        } catch (IOException ioe) {
            return false;
        }
    }
}
