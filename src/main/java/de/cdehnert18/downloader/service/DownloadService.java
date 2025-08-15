package de.cdehnert18.downloader.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import de.cdehnert18.downloader.configuration.YtDlpConfiguration;

@Service
public class DownloadService {
    
    public void getFile(String url, String type, Consumer<Integer> onProgress)
            throws IOException, InterruptedException {
        String command = "~/Downloads/yt-dlp_linux " + url +
                " --progress" +
                " -o \"%(title)s.%(ext)s\"" +
                " --min-filesize " + YtDlpConfiguration.maxFilesize;

        ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
        pb.directory(new File(System.getProperty("user.home")));
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            int lastPercent = -1;
            while ((line = reader.readLine()) != null) {
                if (line.contains("[download]") && line.contains("%")) {
                    Matcher m = Pattern.compile("([0-9]+)(?:\\.[0-9]+)?%").matcher(line);
                    if (m.find()) {
                        int percent = Integer.parseInt(m.group(1));
                        if (percent != lastPercent) {
                            lastPercent = percent;
                            onProgress.accept(percent);
                        }
                    }
                }
            }
        }
        process.waitFor();
    }

    private void convertFile(String type) {

    }

    public String getYtDlpFileName(String url) throws IOException, InterruptedException {
        String command = "~/Downloads/yt-dlp_linux --get-filename -o \"%(title)s.%(ext)s\" " + url;
        ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line = reader.readLine();

        int exitVal = process.waitFor();
        if (exitVal == 0) {
            return line;
        } else {
            throw new InterruptedException("cant get filename");
        }
    }
}
