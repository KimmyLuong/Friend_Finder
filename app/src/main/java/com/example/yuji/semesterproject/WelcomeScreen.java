package com.example.yuji.semesterproject;


/*
* CS4V95 Semester Project
*
* Programmers:
*   Eric Komachi
*   Kimmy Luong
*
* Concept:
* This app serves to easily locate someone - or be located - in areas where
* GPS location services do not cut it. This could range anywhere from large
* indoor areas or even in underground areas. This app uses Wi-Fi to approximate
* distance between you and the person you are trying to find and will point you
* towards them via on screen compass.
*
* Current Progress: Initial Prototyping
* There is no logic yet. As per assignment instructions, we are laying out the
* graphical foundation for user navigation as well as commented methods
*
*
 */
import android.content.Intent;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

public class WelcomeScreen extends ActionBarActivity {

    //------------------------------List of intents START
    Intent toBeFound;
    Intent toFindSomeone;
    Intent toAppMainSettings;
    Intent toAboutActivity;
    //------------------------------List of intents END

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectNetwork() // or .detectAll() for all detectable problems
                .penaltyDialog()  //show a dialog
                        //.permitNetwork() //permit Network access
                .build());
        //Creating all the intents...
        toBeFound = new Intent(this, BeFoundActivity.class);
        toFindSomeone = new Intent(this, FindSomeoneActivity.class);
        toAboutActivity = new Intent(this, AboutActivity.class);

        toBeFound.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        toBeFound.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        toFindSomeone.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        toFindSomeone.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);




        //Sets content view to the welcome screen XML
        setContentView(R.layout.activity_welcome_screen);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_welcome_screen, menu);
        return true;
    }


    @Override
    public void onPause(){
        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.about_app) {
            startActivity(toAboutActivity);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Above are pre-defined methods
    //Below are methods we created


    public void onFindSomeone(View view){

    /*
    *onFindSomeone(View view)
    *
    * This method handles the onClick event for the button, Find Someone
    * As of 3/23/2015, this button will only serve as a way to start the
    * FindSomeoneActivity
    *
     */

        startActivity(toFindSomeone);
        finish();
    }


    public void onBeFound(View view){
    /*
    * onBeFound(View view)
    *
    * This method handles the onClick event for the button, Be Found
    * As of 3/23/2015, this button will only serve as a way to start the
    * BeFoundActivity
    *
     */
        startActivity(toBeFound);
        finish();
    }

}

