package jace.website.personal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Hello world!
 *
 */
public class Main
{
    private static final Logger logger = LogManager.getLogger(Main.class);

    private static String readFile(String filepath) {
        String content = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filepath));
            String line = reader.readLine();
            while(line != null) {
                content = content + line.replace("\n", "");
                line = reader.readLine();
            }
            reader.close();
        } catch (FileNotFoundException e) {
            System.out.println("WARNING: Connot find config file, using default config");
        } catch (IOException e) {
            System.out.println("WARNING: I/O error, using default config");
        }
        return content;
    }

    private static Config parseArgs (String[] args) {
        Config config = new Config();
        int index = 0;
        while (index < args.length) {
            if (args[index].equals("-c")) {
                index += 1;
                String configPath = args[index];
                String content = readFile(configPath);
                System.out.println(content);
                index += 1;
            } else {
                logger.warn("WARNING: Unexpected parameters, using default config.");
                break;
            }
        }
        // TODO: Complete this
        return config;
    }

    public static void main( String[] args )
    {
        Config config = parseArgs(args);
        new Server(config).start();
    }
}
