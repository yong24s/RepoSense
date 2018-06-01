package reposense.frontend;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import reposense.dataobject.RepoConfiguration;
import reposense.parser.CsvParser;
import reposense.report.RepoInfoFileGenerator;

public class RepoSense {

    public static void main(String[] args) {
        if (args.length == 0) {
            showHelpMessage();
        }

        try {
            CliArguments cliArguments = new CliArguments(args);
            Path targetFilePath = cliArguments.getTargetFilePath();
            CsvParser csvParser = new CsvParser();
            List<RepoConfiguration> configs = csvParser.parse(cliArguments);
            RepoInfoFileGenerator.generateReposReport(configs, targetFilePath.toAbsolutePath().toString());
        } catch (IllegalArgumentException iae) {
            System.out.print(iae.getMessage());
            showHelpMessage();
        } catch (IOException ioe) {
            System.out.print(ioe.getMessage());
        }
    }

    private static void showHelpMessage() {
        System.out.println("usage: java -jar RepoSense.jar -config CSV_CONFIG_FILE_PATH\n"
                + "   [-output OUTPUT_DIRECTORY]\n"
                + "   [-since DD/MM/YYYY]\n"
                + "   [-until DD/MM/YYYY]\n");
        System.out.println("-config: Mandatory. The path to the CSV config file.");
        System.out.println("-output: Optional. The path to the dashboard generated.\n"
                + "   If not provided, it will be generated in the current directory.");
        System.out.println("-since : Optional. start date of analysis. Format: dd/MM/yyyy");
        System.out.println("-until : Optional. end date of analysis. Format: dd/MM/yyyy");
    }
}
