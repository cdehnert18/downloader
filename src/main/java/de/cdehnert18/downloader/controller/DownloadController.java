package de.cdehnert18.downloader.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import de.cdehnert18.downloader.service.DownloadService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/download")
@RequiredArgsConstructor
public class DownloadController {
    
    private final DownloadService downloadService;

    @GetMapping(value = "/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamProgress(@RequestParam("url") String url,
                                    @RequestParam("format") String format) {
        SseEmitter emitter = new SseEmitter();

        new Thread(() -> {
            try {
                String fileName = downloadService.getYtDlpFileName(url); // get filename first

                downloadService.getFile(url, format, percent -> {
                    try {
                        emitter.send(percent); // just send progress
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                });

                // after download is fully done, send "done" event separately
                String downloadUrl = "/download?name=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8);
                emitter.send(SseEmitter.event()
                    .name("done")
                    .data(downloadUrl));

                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }


    @GetMapping()
    public ResponseEntity<Resource> downloadFile(@RequestParam("name") String name) throws IOException {
        String home = System.getProperty("user.home");
        Path filePath = Paths.get(home, name);

        Resource resource = new FileSystemResource(filePath);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filePath.getFileName() + "\"")
                .contentLength(Files.size(filePath))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

}
