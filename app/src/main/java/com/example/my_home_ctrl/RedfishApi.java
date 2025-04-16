package com.example.my_home_ctrl;
import com.example.my_home_ctrl.RedfishSystem;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface RedfishApi {

    @GET
    Call<RedfishSystem> getSystemStatus(@Url String url, @Header("Authorization") String auth);

    @Headers("Content-Type: application/json")
    @POST("redfish/v1/Systems/System.Embedded.1/Actions/ComputerSystem.Reset")
    Call<Void> sendPowerCommand(
            @Header("Authorization") String auth,
            @Body JsonObject body
    );
}
