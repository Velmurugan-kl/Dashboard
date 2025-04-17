package com.example.my_home_ctrl;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import okhttp3.OkHttpClient;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends Activity {

    private TextView statusText;
    private Button powerButton;
    private ImageButton refreshButton;
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
        powerButton.requestFocus();
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

        TextView pingStatusText = findViewById(R.id.pingStatusText);
        checkSiteStatus("google.com", 443, pingStatusText); // Redfish default port
        // Or replace with your desired IP/domain

        // Initial fetch
        fetchSystemStatus();

        // Auto Refresh every 30 seconds
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchSystemStatus();
                handler.postDelayed(this, 30000); // 30 sec
            }
        }, 30000);

        // Manual refresh
        refreshButton.setOnClickListener(v -> {
            fetchSystemStatus();
            checkSiteStatus("10.0.1.1", 443, pingStatusText); // Redfish default port
            // Or use any domain/IP you want to monitor
        });

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
                        statusText.setText("Power : " + action);
                        fetchSystemStatus(); // Refresh after action
                    } else {
                        statusText.setText("Command Failed : " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    statusText.setText("Command failed: " + t.getMessage());
                }
            });
        });
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

    private void checkSiteStatus(String host, int port, TextView pingStatusText) {
        new Thread(() -> {
            try {
                Log.d("PingCheck", "Trying to connect to: " + host + ":" + port);
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 1500);
                socket.close();
                runOnUiThread(() -> {
                    pingStatusText.setText("ONLINE");
                    pingStatusText.setTextColor(0xFF00FF00);
                });
            } catch (IOException e) {
                Log.e("PingCheck", "Failed to connect to " + host + ":" + port, e);
                runOnUiThread(() -> {
                    pingStatusText.setText("OFFLINE");
                    pingStatusText.setTextColor(0xFFFF4444);
                });
            }
        }).start();
    }



}
