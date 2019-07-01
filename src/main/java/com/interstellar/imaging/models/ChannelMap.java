package com.interstellar.imaging.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ChannelMap {
    @JsonProperty("visible")
    VISIBLE("04", "03", "02"),

    @JsonProperty("vegetation")
    VEGETATION("05", "06", "07"),

    @JsonProperty("waterVapor")
    WATERVAPOR(null, null, "07");

    private final String redBand;
    private final String greenBand;
    private final String blueBand;

    ChannelMap(final String redBand, final String greenBand, final String blueBand) {
        this.redBand = redBand;
        this.greenBand = greenBand;
        this.blueBand = blueBand;
    }

    public String getRedBand() {
        return redBand;
    }

    public String getGreenBand() {
        return greenBand;
    }

    public String getBlueBand() {
        return blueBand;
    }


}
