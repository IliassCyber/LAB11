package com.example.localisationsmartphone;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "GeoTrackerApp";
    private static final int PERMISSION_REQ_CODE = 1001;
    private static final String SERVER_API_URL = "http://10.0.2.2:8080/LAB11/createPosition.php";

    private TextView statusText;
    private RequestQueue networkQueue;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.displayInfo);
        networkQueue = Volley.newRequestQueue(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        setupLocationTracking();
    }

    private void setupLocationTracking() {
        if (checkAppPermissions()) {
            startUpdatingLocation();
        } else {
            requestPermissions();
        }
    }

    private boolean checkAppPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
               ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
        }, PERMISSION_REQ_CODE);
    }

    @SuppressLint("MissingPermission")
    private void startUpdatingLocation() {
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    60000, 
                    150,   
                    locationUpdateListener
            );
        } catch (Exception e) {
            Log.e(LOG_TAG, "Erreur GPS", e);
        }
    }

    private final LocationListener locationUpdateListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            updateUI(location);
            dispatchLocationUpdate(location);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            notifyMessage(getString(R.string.status_active, provider));
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            notifyMessage(getString(R.string.status_inactive, provider));
        }
    };

    private void updateUI(Location loc) {
        String displayText = getString(R.string.location_details,
                loc.getLatitude(), loc.getLongitude(), loc.getAltitude(), loc.getAccuracy());
        statusText.setText(displayText);
    }

    private void dispatchLocationUpdate(final Location loc) {
        StringRequest postRequest = new StringRequest(
                Request.Method.POST,
                SERVER_API_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        notifyMessage("Serveur : " + response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        notifyMessage(getString(R.string.upload_error));
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                
                params.put("latitude", String.valueOf(loc.getLatitude()));
                params.put("longitude", String.valueOf(loc.getLongitude()));
                params.put("date_position", dateFormat.format(new Date()));
                params.put("imei", getDeviceIdentifier());
                
                return params;
            }
        };
        networkQueue.add(postRequest);
    }

    @SuppressLint({"HardwareIds", "MissingPermission"})
    private String getDeviceIdentifier() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                try {
                    // Pour Android 10+ (API 29+), getDeviceId() est restreint. On utilise une alternative.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        return tm.getDeviceId();
                    } else {
                        return "ID_" + Build.ID;
                    }
                } catch (Exception e) {
                    return "err_id";
                }
            }
        }
        return "inconnu";
    }

    private void notifyMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startUpdatingLocation();
            } else {
                notifyMessage("Permissions refusées");
            }
        }
    }
}
