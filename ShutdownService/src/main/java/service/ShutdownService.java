package service;

import java.io.IOException;

public class ShutdownService {

    public static void shutdownInSeconds(int seconds) {
        ProcessBuilder pb = new ProcessBuilder(
                "shutdown",
                "-s",
                "-t",
                seconds + ""
        );

        try {
            pb.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
