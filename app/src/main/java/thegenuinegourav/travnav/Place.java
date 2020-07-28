package thegenuinegourav.travnav;

import android.location.Location;

public class Place {
    private String adminArea, place, addressLine, locality, subAdminArea;
    private Location location;

    public Place() {
    }

    public Place(String adminArea, String place, String addressLine, String locality, String subAdminArea, Location location) {
        this.adminArea = adminArea;
        this.place = place;
        this.addressLine = addressLine;
        this.locality = locality;
        this.subAdminArea = subAdminArea;
        this.location = location;
    }

    public String getAdminArea() {
        return adminArea;
    }

    public void setAdminArea(String adminArea) {
        this.adminArea = adminArea;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getSubAdminArea() {
        return subAdminArea;
    }

    public void setSubAdminArea(String subAdminArea) {
        this.subAdminArea = subAdminArea;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
