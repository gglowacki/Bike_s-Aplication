package bike_s.arduino.bike_s;

/**
 * Created by Grzegorz on 18.03.2018.
 */

public class Station {
    private String name;
    private Double latitude;
    private Double longitude;
    private Integer emptySlots;
    private Integer freeBikes;

    public Station(String name, Double latitude, Double longitude, Integer emptySlots, Integer freeBikes) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.emptySlots = emptySlots;
        this.freeBikes = freeBikes;
    }

    @Override
    public String toString() {
        return  name +
                "\nWolne miejsca: " + emptySlots +
                ", Wolne rowery: " + freeBikes;
    }

    public String getName() {
        return name;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Integer getEmptySlots() {
        return emptySlots;
    }

    public Integer getFreeBikes() {
        return freeBikes;
    }
}
