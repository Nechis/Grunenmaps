package com.leinaro.grunenthal;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.Collator;
import java.text.ParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashScreenActivity extends AppCompatActivity implements GetTerms.AsyncResponse {

    private boolean mVisible;
    private SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        hide();
        setContentView(R.layout.activity_splash_screen);
        new GetTerms(this).execute();
        mVisible = true;
    }

    private void hide() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    @Override
    public void getTermsComplete(JSONObject output) {
        try {
            Log.d("iarl", "getTermsComplete" );
            boolean success = output.optBoolean("result", false);
            Log.d("iarl", "getTermsComplete  "+ success);

            if (success) {
                GrnenthalApplication.terms = output.optString("data");
                showTerms();
            } else {
                Toast.makeText(this, "No se pudo obtener términos y condiciones", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void aceptTerms() {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putBoolean("Acepto_Terminos_y_condiciones", true);
        editor.commit();
        requestAll();
    }

    private void showTerms() {
        Log.d("iarl", "showTerms");

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.layout_dialog_terms, null);

        sharedpreferences = getSharedPreferences("MyPREFERENCESGrunenthal", Context.MODE_PRIVATE);

        boolean terminosYCondiciones = sharedpreferences.getBoolean("Acepto_Terminos_y_condiciones", false);

        Log.d("iarl", "showTerms "+terminosYCondiciones);

        if (terminosYCondiciones) {
            requestAll();
        } else {
            TextView textview = (TextView) view.findViewById(R.id.textmsg);
            textview.setText(GrnenthalApplication.terms);
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle(R.string.terms_title);
            alertDialog.setCancelable(false);
            alertDialog.setView(view);
            alertDialog.setPositiveButton(getString(R.string.dialog_terms_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    aceptTerms();
                }
            });

            AlertDialog alert = alertDialog.create();
            alert.show();
        }
    }

    private void requestAll() {
        Log.d("iarl", "requestAll");

        Server server = new Server();
        server.getStablishment().enqueue(new Callback<ResponseGetAllPharmacies>() {
            @Override
            public void onResponse(Call<ResponseGetAllPharmacies> call, Response<ResponseGetAllPharmacies> response) {
                Log.d("iarl", "get stablshment success");
                if (response.isSuccessful()) {
                    getStablishmentsComplete(response.body());
                }
            }
            @Override
            public void onFailure(Call<ResponseGetAllPharmacies> call, Throwable t) {
                Log.e("iarl", "error to get stablishment");
                Toast.makeText(getApplicationContext(), "No se pudo obtener los establecimientos", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void getStablishmentsComplete(ResponseGetAllPharmacies output) {
        Log.d("iarl", "getStablishmentsComplete " );

        try {
            if (output.result) {
                setPalexis(output.data.get(1).pharmacies2);
                setTranstec(output.data.get(0).pharmacies1);
                sortFranquise();
                GrnenthalApplication.franquicias.add(0, "Todos");
            } else {
                Toast.makeText(this, "No se pudo obtener farmacias", Toast.LENGTH_LONG).show();
            }

            Intent mainIntent = new Intent().setClass(SplashScreenActivity.this, MainActivity.class);
            startActivity(mainIntent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setPalexis(List<Pharmacies> pharmacies1) throws JSONException {
        for (int i = 0; i < pharmacies1.size(); i++) {
            Log.d("iarl", "name " + pharmacies1.get(i).name);
            pharmacies1.get(i).color = "#92317c";
            addFranquicia(pharmacies1.get(i).franchise);
        }
        GrnenthalApplication.pharmacies1 = pharmacies1;
    }

    private void setTranstec(List<Pharmacies> pharmacies2) throws JSONException {
        for (int i = 0; i < pharmacies2.size(); i++) {
            Log.d("iarl", "name " + pharmacies2.get(i).name);
            pharmacies2.get(i).color = "#dc4338";
            addFranquicia(pharmacies2.get(i).franchise);
            GrnenthalApplication.pharmacies2 = pharmacies2;
        }
    }

    private void addFranquicia(String franquicia) {
        boolean exist = false;
        for (int i = 0; i < GrnenthalApplication.franquicias.size(); i++) {
            if (GrnenthalApplication.franquicias.get(i).compareToIgnoreCase(franquicia) == 0) {
                exist = true;
            }
        }
        if (!exist)
            GrnenthalApplication.franquicias.add(franquicia);
    }

    private void sortFranquise() throws ParseException {
        Collections.sort(GrnenthalApplication.franquicias, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                Collator usCollator = Collator.getInstance(Locale.US);
                return usCollator.compare(o1, o2);
//                return o1.compareToIgnoreCase(o2);
            }
        });

    }

}