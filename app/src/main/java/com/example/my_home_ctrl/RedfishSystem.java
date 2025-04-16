package com.example.my_home_ctrl;

import com.google.gson.annotations.SerializedName;

public class RedfishSystem {
    @SerializedName("PowerState")
    public String powerState;

    @SerializedName("Status")
    public Status status;

    public static class Status {
        @SerializedName("Health")
        public String health;
    }
}