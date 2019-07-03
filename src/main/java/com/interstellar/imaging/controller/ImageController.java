package com.interstellar.imaging.controller;

import com.interstellar.imaging.models.ImageCoordinates;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.IMAGE_JPEG;

@RestController
public class ImageController {

    @Value("${granules.path}")
    private String granulesPath;

    @PostMapping(value = "/generate-image", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<byte[]> generateImage(@RequestBody ImageCoordinates imageCoordinates) {

        final String[] channelRegexes = getRegexesForChannelFiles(imageCoordinates);
        try {
            final BufferedImage[] images = getImagesGivenRegexes(channelRegexes);
            final BufferedImage completeImage = getCompleteImageFromChannels(images);

            if (completeImage != null) {
                final HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.setContentType(IMAGE_JPEG);
                return new ResponseEntity<>(getByteArrayFromBufferedImage(completeImage), httpHeaders, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        }
    }

    private byte[] getByteArrayFromBufferedImage(final BufferedImage bufferedImage){
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(bufferedImage, "jpg", byteArrayOutputStream);
            byteArrayOutputStream.flush();
            final byte[] imageInByte = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();
            return imageInByte;
        }
        catch (IOException exception){
            //TODO log exception
            exception.printStackTrace();
            return null;
        }
    }

    //TODO: improve performance
    private BufferedImage getCompleteImageFromChannels(final BufferedImage[] images) {
        final BufferedImage completeImage;

        if (images !=null && images[2] != null) { //using blue channel as the three channel maps have it.
            completeImage = new BufferedImage(images[2].getWidth(), images[2].getHeight(), 1);
            if (images[0] != null && images[1] != null) {
                for (int i = 0; i < completeImage.getWidth(); i++) {
                    for (int j = 0; j < completeImage.getHeight(); j++) {
                        final Color redColor = new Color(images[0].getRGB(i, j));
                        int redValue = redColor.getRed();

                        final Color greenColor = new Color(images[1].getRGB(i, j));
                        int greenValue = greenColor.getGreen();

                        final Color blueColor = new Color(images[2].getRGB(i, j));
                        int blueValue = blueColor.getBlue();

                        Color color = new Color(redValue, greenValue, blueValue);
                        completeImage.setRGB(i, j, color.getRGB());
                    }
                }
            } else { //favored adding this code instead of checking for null on each iteration of previous loop
                for (int i = 0; i < completeImage.getWidth(); i++) {
                    for (int j = 0; j < completeImage.getHeight(); j++) {
                        final Color blueColor = new Color(images[2].getRGB(i, j));
                        int blueValue = blueColor.getBlue();

                        Color color = new Color(0, 0, blueValue);
                        completeImage.setRGB(i, j, color.getRGB());
                    }
                }
            }
            return completeImage;
        }else {
            return null;
        }
    }

    private BufferedImage[] getImagesGivenRegexes(final String[] channelRegexes) {
        final BufferedImage[] images = new BufferedImage[channelRegexes.length];

        for (int i = 0; i < images.length; i++) {
            try {
                if (channelRegexes[i] != null) {
                    final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("regex:" + channelRegexes[i]);
                    final List<File> matchingFiles = Files.walk(Paths.get(granulesPath)).filter(pathMatcher::matches).map(Path::toFile).collect(Collectors.toList());
                    if (matchingFiles.size() > 0) {
                        images[i] = ImageIO.read(matchingFiles.get(0));
                    }
                } else {
                    images[i] = null;
                }
            } catch (IOException exception) {
                //TODO log Exception
                return null;
            }
        }
        return images;
    }

    private String[] getRegexesForChannelFiles(final ImageCoordinates imageCoordinates) {
        final String[] regexes = new String[3];
        regexes[0] = getRegexforRedChannelFile(imageCoordinates);
        regexes[1] = getRegexforGreenChannelFile(imageCoordinates);
        regexes[2] = getRegexforBlueChannelFile(imageCoordinates);

        return regexes;
    }


    private String getRegexforRedChannelFile(final ImageCoordinates imageCoordinates) {
        final String redBand = imageCoordinates.getChannelMap().getRedBand();
        if (redBand == null) {
            return null;
        }
        final StringBuilder stringBuilder = getCommonRegex(imageCoordinates);
        stringBuilder.append("(").append(redBand).append(")?");
        stringBuilder.append(".tif");

        return stringBuilder.toString();
    }

    private String getRegexforBlueChannelFile(final ImageCoordinates imageCoordinates) {
        final String blueBand = imageCoordinates.getChannelMap().getBlueBand();
        if (blueBand == null) {
            return null;
        }

        final StringBuilder stringBuilder = getCommonRegex(imageCoordinates);
        stringBuilder.append("(").append(blueBand).append(")?");
        stringBuilder.append(".tif");

        return stringBuilder.toString();
    }

    private String getRegexforGreenChannelFile(final ImageCoordinates imageCoordinates) {
        final String greenBand = imageCoordinates.getChannelMap().getGreenBand();
        if (greenBand == null) {
            return null;
        }

        final StringBuilder stringBuilder = getCommonRegex(imageCoordinates);
        stringBuilder.append("(").append(greenBand).append(")?");
        stringBuilder.append(".tif");

        return stringBuilder.toString();
    }

    private StringBuilder getCommonRegex(final ImageCoordinates imageCoordinates) {
        final StringBuilder stringBuilder = new StringBuilder("(.)*T");
        stringBuilder.append(imageCoordinates.getUtmZone());
        stringBuilder.append(imageCoordinates.getLatitudeBand());
        stringBuilder.append(imageCoordinates.getGridSquare());
        stringBuilder.append("_");

        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        final String formatteStringDate = imageCoordinates.getDate().format(dateTimeFormatter);
        stringBuilder.append(formatteStringDate);
        stringBuilder.append("(.)*_B");
        return stringBuilder;
    }
}
