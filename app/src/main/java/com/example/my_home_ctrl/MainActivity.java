package com.example.my_home_ctrl;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageButton;  // Change to ImageButton
import android.widget.TextView;

import androidx.leanback.app.BrowseFragment;

import com.google.gson.JsonObject;

import okhttp3.OkHttpClient;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends Activity {

    private TextView statusText;
    private Button powerButton;
    private ImageButton refreshButton;  // Change to ImageButton
    private boolean isOn = false;

    private final OkHttpClient unsafeClient = UnsafeOkHttpClient.getUnsafeOkHttpClient();
    private RedfishApi api;
    private String basicAuth;
    private final Handler handler = new Handler();
    private final String endpoint = "redfish/v1/Systems/System.Embedded.1";
    private final String baseUrl = "https://10.0.1.10/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        powerButton = findViewById(R.id.powerButton);
        refreshButton = findViewById(R.id.refreshButton);

        // Retrofit + Auth setup
        String username = "root";
        String password = "vel@dell";
        basicAuth = "Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(unsafeClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(RedfishApi.class);

        // Initial fetch
        fetchSystemStatus();

        // Auto Refresh every 30 seconds
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchSystemStatus();
                handler.postDelayed(this, 3000); // 3 sec
            }
        }, 30000);

        // Manual refresh
        refreshButton.setOnClickListener(v -> fetchSystemStatus());

        // Power button
        powerButton.setOnClickListener(v -> {
            String action = isOn ? "GracefulShutdown" : "On";

            JsonObject body = new JsonObject();
            body.addProperty("ResetType", action);

            Call<Void> call = api.sendPowerCommand(basicAuth, body);
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        statusText.setText("Power command sent: " + action);
                        fetchSystemStatus(); // Refresh after action
                    } else {
                        statusText.setText("Failed to send command: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    statusText.setText("Command failed: " + t.getMessage());
                }
            });
        });

        // Fragment setup
        BrowseFragment browseFragment = new BrowseFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.main_browse_fragment, browseFragment);
        transaction.commit();
    }

    private void fetchSystemStatus() {
        api.getSystemStatus(endpoint, basicAuth).enqueue(new Callback<RedfishSystem>() {
            @Override
            public void onResponse(Call<RedfishSystem> call, Response<RedfishSystem> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RedfishSystem system = response.body();
                    isOn = system.powerState.equalsIgnoreCase("On");
                    String status = "Power: " + system.powerState + "\nHealth: " + system.status.health;
                    statusText.setText(status);
                    powerButton.setText(isOn ? "Turn OFF" : "Turn ON");
                } else {
                    statusText.setText("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<RedfishSystem> call, Throwable t) {
                statusText.setText("Connection failed");
            }
        });
    }
}
