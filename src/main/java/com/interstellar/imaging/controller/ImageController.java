package com.interstellar.imaging.controller;

import com.google.common.primitives.Bytes;
import com.interstellar.imaging.models.ImageCoordinates;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.IMAGE_JPEG;

@RestController
public class ImageController {

    @Value("${granules.path}")
    private String granulesPath;

    @PostMapping(value = "/generate-image", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Byte>> generateImage(@RequestBody ImageCoordinates imageCoordinates) {
        final String regex = getRegexFromCoordinates(imageCoordinates);
        try {
            PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("regex:" + regex);
            List<File> matchingFiles = Files.walk(Paths.get(granulesPath)).filter(pathMatcher::matches).map(Path::toFile).collect(Collectors.toList());

            List<Byte> allMedia = new ArrayList<>();
            for (File file : matchingFiles) {
                List<Byte> newBytes = getBytesFromFile(file);
                if (newBytes != null) {
                    allMedia.addAll(newBytes);
                }
            }

            //Continues to get error due to bits amounts. Tried different libraries but TIFF files seem to have
            // weird bits amount per sample
            // BufferedImage imag = ImageIO.read(new ByteArrayInputStream(Bytes.toArray(allMedia)));

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(IMAGE_JPEG);

            return new ResponseEntity<>(allMedia, httpHeaders, HttpStatus.OK);

        } catch (Exception e) {
            return null;
        }
    }

    private String getRegexFromCoordinates(ImageCoordinates imageCoordinates) {
        StringBuilder stringBuilder = new StringBuilder("(.)*T");
        stringBuilder.append(imageCoordinates.getUtmZone());
        stringBuilder.append(imageCoordinates.getLatitudeBand());
        stringBuilder.append(imageCoordinates.getGridSquare());
        stringBuilder.append("_");

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String formatteStringDate = imageCoordinates.getDate().format(dateTimeFormatter);
        stringBuilder.append(formatteStringDate);
        stringBuilder.append("(.)*_B");

        final String redBand = imageCoordinates.getChannelMap().getRedBand();
        final String blueBand = imageCoordinates.getChannelMap().getBlueBand();
        final String greenBand = imageCoordinates.getChannelMap().getGreenBand();

        stringBuilder.append("(").append(redBand).append(")?");
        stringBuilder.append("(").append(blueBand).append(")?");
        stringBuilder.append("(").append(greenBand).append(")?");

        stringBuilder.append(".tif");

        return stringBuilder.toString();
    }

    private List<Byte> getBytesFromFile(File file) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            return Bytes.asList(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }


    }
}
