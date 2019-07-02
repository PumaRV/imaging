package com.interstellar.imaging.controller;

import com.aspose.imaging.Image;
import com.aspose.imaging.fileformats.tiff.TiffImage;
import com.aspose.imaging.imageoptions.JpegOptions;
import com.google.common.io.ByteStreams;
import com.interstellar.imaging.models.ImageCoordinates;


import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.util.factory.Hints;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.github.jaiimageio.plugins.tiff.GeoTIFFTagSet;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
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
    public ResponseEntity<byte[]> generateImage(@RequestBody ImageCoordinates imageCoordinates) {
        final String regex = getRegexFromCoordinates(imageCoordinates);
        try {
            PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("regex:" + regex);
            List<File> matchingFiles = Files.walk(Paths.get(granulesPath)).filter(pathMatcher::matches).map(Path::toFile).collect(Collectors.toList());

            List<TiffImage> allTiffs = new ArrayList<>();

//            Unsuccesful attempt to use JAI ImageIO to read TIFS, wrong value for bits per sample (15)
//            File file0 = matchingFiles.get(0);
//            ImageReader imageReader = ImageIO.getImageReadersByFormatName("tif").next();
//            ImageInputStream imageInputStream = ImageIO.createImageInputStream(file0);
//            imageReader.setInput(imageInputStream);
//            BufferedImage bufferedImage = imageReader.read(0);

//            Unsuccesful attempt to use GeoTools to read TIFS, wrong value for bits per sample (15)
//            File file0 = matchingFiles.get(0);
//            Hints hints = new Hints();
//            GeoTiffReader geoTiffReader = new GeoTiffReader(file0);

            for (File file : matchingFiles) {
                allTiffs.add((TiffImage) Image.load(file.getPath()));
            }

            int frameCounter = 0;
            for (TiffImage tiffImage : allTiffs) {
                if (frameCounter > 0) {
                    allTiffs.get(0).add(tiffImage);
                }
                frameCounter++;
            }
            TiffImage completeImage = allTiffs.get(0);

            //TODO: improve response time
            completeImage.save(granulesPath + "/output.jpg", new JpegOptions());

            InputStream inputStream = new FileInputStream(granulesPath + "/output.jpg");

            byte[] bytes = ByteStreams.toByteArray(inputStream);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(IMAGE_JPEG);

            return new ResponseEntity<>(bytes, httpHeaders, HttpStatus.OK);

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
}
