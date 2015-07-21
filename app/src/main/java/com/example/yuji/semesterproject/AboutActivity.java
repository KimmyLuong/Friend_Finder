package com.example.yuji.semesterproject;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class AboutActivity extends ActionBarActivity {


    /*
    * AboutActivity class
    *
    * This activity simply show miscellaneous information about the app
    * such as version number and the developer name as well as a link to
    * the website. As of 3/24/2015, nothing is coded in. Only the layout
    * is here
    *
     */

    Intent backToHome;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        backToHome = new Intent(this, WelcomeScreen.class);
        setContentView(R.layout.activity_about);
    }


    public void returnHome(View view){
        startActivity(backToHome);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_about, menu);
        return true;
    }
}
