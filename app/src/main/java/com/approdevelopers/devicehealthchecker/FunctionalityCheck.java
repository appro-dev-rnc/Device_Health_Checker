package com.approdevelopers.devicehealthchecker;

public class FunctionalityCheck {

    private String title;

    private String working_status;
    private String availability_status;

    public FunctionalityCheck() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getWorking_status() {
        return working_status;
    }

    public void setWorking_status(String working_status) {
        this.working_status = working_status;
    }

    public String getAvailability_status() {
        return availability_status;
    }

    public void setAvailability_status(String availability_status) {
        this.availability_status = availability_status;
    }

    @Override
    public String toString() {
        return "FunctionalityCheck{" +
                "title='" + title + '\'' +
                ", working_status='" + working_status + '\'' +
                ", availability_status='" + availability_status + '\'' +
                '}';
    }
}
