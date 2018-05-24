package reposense;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import reposense.dataobject.RepoConfiguration;
import reposense.frontend.CliArguments;
import reposense.parser.CsvParser;
import reposense.report.RepoInfoFileGenerator;
import reposense.util.FileUtil;


public class Entry {
    private static final String FT_TEMP_DIR = "ft_temp";
    private static final String EXPECTED_FOLDER = "expected";

    @Test
    public void test() {
        try {
            generateReport();
            String actualRelativeDir = getRelativeDir();
            File actualFiles = new File(getClass().getClassLoader().getResource("expected").getFile());
            verifyAllJson(actualFiles, actualRelativeDir);
            FileUtil.deleteDirectory(FT_TEMP_DIR);
        } catch (IOException iex) {
            iex.printStackTrace();
        }
    }

    private void generateReport() throws IOException {
        File configFile = new File(getClass().getClassLoader().getResource("sample_full.csv").getFile());
        String[] args = new String[]{"-config", configFile.getAbsolutePath(),
            "-since", "01/07/2017", "-until", "30/11/2017"};

        CliArguments arguments = new CliArguments(args);
        CsvParser csvParser = new CsvParser();

        List<RepoConfiguration> configs = csvParser.parse(arguments);
        RepoInfoFileGenerator.generateReposReport(configs, FT_TEMP_DIR);
    }

    private void verifyAllJson(File expectedDirectory, String actualRelative) {
        for (File file : expectedDirectory.listFiles()) {
            if (file.isDirectory()) {
                verifyAllJson(file, actualRelative);
            } else {
                if (!file.getName().endsWith(".js")) {
                    continue;
                }
                String relativeDirectory = file.getAbsolutePath().split(EXPECTED_FOLDER)[1];
                assertJson(file, relativeDirectory, actualRelative);
            }
        }
    }

    private void assertJson(File expectedJson, String expectedPosition, String actualRelative) {
        File actual = new File(actualRelative + expectedPosition);
        Assert.assertTrue(actual.exists());
        verifyContent(expectedJson, actual);
    }

    private void verifyContent(File expected, File actual) {
        String expectedContent = "";
        String actualContent = "";
        try {
            expectedContent = new String(Files.readAllBytes(expected.toPath()));
            actualContent = new String(Files.readAllBytes(actual.toPath()));
        } catch (IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(expectedContent, actualContent);
    }

    private String getRelativeDir() {
        for (File file : (new File(FT_TEMP_DIR)).listFiles()) {
            if (file.getName().contains("DS_Store")) {
                continue;
            }
            return FT_TEMP_DIR + File.separator + file.getName();
        }
        Assert.fail();
        return "";
    }
}
