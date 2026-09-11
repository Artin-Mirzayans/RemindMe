package com.remindme.local;

// a resolved approximate location for a visitor - enough to find events near them
public record GeoLocation(String city, String region, String country, double lat, double lon, String timezone) {

    // e.g. "Calabasas, California" or "London, England"
    public String label() {
        if (city == null || city.isBlank()) {
            return region == null || region.isBlank() ? country : region;
        }
        return region == null || region.isBlank() ? city : city + ", " + region;
    }
}
