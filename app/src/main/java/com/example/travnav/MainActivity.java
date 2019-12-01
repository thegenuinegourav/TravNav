package com.example.travnav;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int ERROR_DIALOG_REQUEST = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Thread timerThread= new Thread()
        {
            public void run()
            {
                    if(isServicesOK()){
                        init();
                    }
            }
        };
        timerThread.start();//to start the thread


    }

    private void init(){
        try {
            sleep(2000);
            // to make the thread sleep for some time (in milliseconds).
            // Here we used this delay time as the time to display the splash screen activity.
            // After this delay time, MapActivity is started by the code written in finally{} block.

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            startActivity(intent);

            // finifh this activity
            finish();
        }

    }

    public boolean isServicesOK(){
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if(available == ConnectionResult.SUCCESS){
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

}
