package com.interstellar.imaging.models;

import lombok.Data;

import java.time.LocalDate;

public @Data
class ImageCoordinates {
    private final int utmZone;
    private final char latitudeBand;
    private final String gridSquare;
    private final LocalDate date;
    private final ChannelMap channelMap;
}
