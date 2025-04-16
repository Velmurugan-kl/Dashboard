package com.example.my_home_ctrl;

import com.google.gson.annotations.SerializedName;

public class PowerActionRequest {
    @SerializedName("ResetType")
    String resetType;

    public PowerActionRequest(String resetType) {
        this.resetType = resetType;
    }
}

