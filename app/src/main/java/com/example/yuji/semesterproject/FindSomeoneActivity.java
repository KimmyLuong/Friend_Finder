package com.example.yuji.semesterproject;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;


public class FindSomeoneActivity extends ActionBarActivity implements SensorEventListener{

    /*
    * FindSomeoneActivity class
    *
    * This class prompts the user to type in a code that will be used to locate
    * a friend. The friend will enter the same code on the BeFoundActivity page.
    * The app will then use GPS, wifi, and cell tower communications to locate
    * the friend who entered in the code. The app will update the user the location
    * of the person in question via on screen compass.
    *
     */

    //------------------------------------------------------Booleans START
    boolean success = false;
    //--------------------------------------------------------Booleans END

    //----------------------------------------------System Variables START

    Intent toAppMainSettings;
    Intent toAboutActivity;
    Intent toWelcomeActivity;

    Socket senderSocket;
    HandlerThread distanceHandler;
    Handler mHandler;

    DataInputStream searcherIn;
    DataOutputStream searcherOut;
    //------------------------------------------------System variables END

    //------------------------------------------------------TextView START
    TextView latitudeText;
    TextView longitudeText;
    TextView progressIndicator;
    TextView receivedLatitudeText;
    TextView receivedLongitudeText;
    ImageView arrow;
    //--------------------------------------------------------TextView END

    //----------------------------------------Embedded Class Objects START
    CalculateDistance calculateDistance;
    InitialSearcherConnection initialSearcherConnection;
    //------------------------------------------Embedded Class Objects END

    //---------------------------------------------Storage Variables START
    int senderPortNumber = 10000;
    //-----------------------------------------------Storage Variables END

    //-------------------------------------------------GPS Variables START
    LocationManager locationManager;
    double longitude = 0;
    double latitude = 0;
    double receivedLongitude;
    double receivedLatitude;
    Location bearingTo;
    Location here;
    double bearing;
    double distanceAway = 0;        //Grabs the value of the distance from host to searcher from server

    //---------------------------------------------------GPS Variables END

    //---------------------------------------------------Sensors START
    Sensor accelerometer;
    Sensor magnetometer;
    SensorManager sManager;
    float[] mGravity;
    float[] mGeomagnetic;
    float orientation[] = new float[3];
    //---------------------------------------------------Sensors END

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectNetwork() // or .detectAll() for all detectable problems
                .penaltyDialog()  //show a dialog
                        //.permitNetwork() //permit Network access
                .build());
        bearingTo = new Location("Host");
        here = new Location("Seeker");


        //Instantiates all intents needed in this activity
        toAboutActivity = new Intent(this, AboutActivity.class);
        toWelcomeActivity = new Intent(this, WelcomeScreen.class);

        setContentView(R.layout.activity_find_someone);


        //Instantiates all TextViews
        latitudeText = (TextView) findViewById(R.id.searcherLatitude);
        longitudeText = (TextView) findViewById(R.id.searcherLongitude);
        progressIndicator = (TextView) findViewById(R.id.progressIndicatorTextView);
        receivedLatitudeText = (TextView) findViewById(R.id.receivedLatitudeValueTextView);
        receivedLongitudeText = (TextView) findViewById(R.id.receivedLongitudeValueTextView);
        arrow = (ImageView) findViewById(R.id.datArrow);

        //Instantiates and executes the AsyncTask responsible for network connectivity
        initialSearcherConnection = new InitialSearcherConnection();
        initialSearcherConnection.execute();

        //Instantiates the distanceHandler to accept mHandler as the handler being looped
        //mHandler then posts calculateDistance immediately once the AsyncTask completes
        distanceHandler = new HandlerThread("HandleTheDistance");
        distanceHandler.start();
        Looper looper = distanceHandler.getLooper();
        mHandler = new Handler(looper);

        //Instantiates the inner Runnable class CalcuateDistance
        calculateDistance = new CalculateDistance();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_find_someone, menu);
        return true;
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

    @Override
    protected void onResume() {
        super.onResume();
        //Instantiates the sensor events for orientation to allow our app to work no matter
        //what direction the user is facing
        sManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause(){
        super.onPause();
        System.out.println("Pausing app.. Pausing GPS..");
//        locationManager.removeUpdates((LocationListener) locationManager);
    }



    //This method starts the GPS on the phone.
    public void startGPS(){
        //Instantiate GPS Unit
        System.out.println("GPS instantiated!");

        //grabs location service from system context
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //In order to save some battery, we attempt to get the last known location first before actively
        //searching for a new one
        locationManager.getLastKnownLocation(Context.LOCATION_SERVICE);

        //This creates the rules that tells our app to have very precise GPS measurements
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        locationManager.getBestProvider(criteria, true);

        //This creates a listener object that only responds when the GPS picks up new coordinates
        final LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(android.location.Location location) {
                System.out.println("There was a change in coordinates!!");

                //Grabs longitude and latitude coordinates
                latitude = location.getLatitude();
                longitude = location.getLongitude();

//                String latitudeString = String.valueOf(latitude);
                //Sets the TextViews to update with GPS coordinates for the user to see
                latitudeText.setText(String.valueOf(latitude));
                longitudeText.setText(String.valueOf(longitude));

                try {
                    //Send longitude and latitude code to the server
                    searcherOut.writeDouble(latitude);
                    searcherOut.flush();
                    searcherOut.writeDouble(longitude);
                    searcherOut.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                bearingTo.setLatitude(receivedLatitude);
                bearingTo.setLongitude(receivedLongitude);
                here.setLatitude(latitude);
                here.setLongitude(longitude);
                bearing = here.bearingTo(bearingTo);
//                arrow.setRotation((float) bearing);
                System.out.println("Bearing to: " + here.bearingTo(bearingTo));

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            //            public void onPause() {
//                locationManager.removeUpdates((LocationListener) locationManager);
//                super.onPause();
//            }
            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 300, 1, locationListener);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {

                SensorManager.getOrientation(R, orientation);
//                azimut = orientation[0];
                arrow.setRotation((float) ((float) (-orientation[0]*(180/Math.PI))+bearing));
                System.out.println("Azimuth (around Z AXIS):  " + (orientation[0] * (180 / Math.PI)));
//                System.out.println("Pitch (around X AXIS): " + (orientation[1]*(180/Math.PI)));
//                System.out.println("Roll (around Y AXIS): " + (orientation[2]*(180/Math.PI)));// orientation contains: azimut, pitch and roll
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //We don't need this
    }



    private class InitialSearcherConnection extends AsyncTask<Void, Void, Boolean> {

        int progressSpot = 0;               //This variable allows the TextView to circulate through all the progresses
        String progressTextDisplay;         //This is what gets shown to the user
        int testBit = 0;                    //This should change to 100 to ensure a proper connection

        /*
        * InitialSenderConnection<Void, Void, Void>
        *
        * This AsyncTask focuses on creating the server connection between the host
        * of this app and the server that we use to test it on.
        *
         */

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            switch (progressSpot) //progressSpot is the location at which the program is currently running
            {
                case 0: progressTextDisplay = "Establishing connection to server...";
                    break;
                case 1: progressTextDisplay = "Connection Established! Establishing input and output streams...";
                    break;
                case 2: progressTextDisplay = "Testing communication ";
                    break;
                case 3: progressTextDisplay = "Done! You are ready to progress";
                    break;
                case 4: progressTextDisplay = "Error! Cannot read from server";
                    break;
            }
            progressIndicator.setText(progressTextDisplay);
        }


        //This is where the logic behind the AsyncTask is located at. It will quickly connect
        //to the server and perform a simple integer transfer and integer receive
        protected Boolean doInBackground(Void... params) {
            try{
                InetAddress serverAddr;
//                serverAddr = InetAddress.getByName("72.64.113.158");  //CHANGE THIS LATER
                serverAddr = InetAddress.getByName("71.170.79.201");
//                serverAddr = InetAddress.getByName("192.168.3.127");  //Public library using localhost
//                serverAddr = InetAddress.getByName("192.168.1.6");  //Eric's house
                //here you must put your computer's IP address.
                Log.e("TCP Client", "C: Connecting...");

                //Initializes the senderSocket to connect to the server at the desired port (10000 in this case)
                senderSocket = new Socket(serverAddr, senderPortNumber);

                progressSpot++;  //Increments progress to next portion
                publishProgress();

                //Sets up the input and output streams
                searcherIn = new DataInputStream(senderSocket.getInputStream());
                searcherOut = new DataOutputStream(senderSocket.getOutputStream());

                progressSpot++;  //Increments progress to next portion
                publishProgress();

                //Tests to see if the host can send a 100 to the server
                searcherOut.writeInt(99);
                searcherOut.flush();

                //Attempts to read from the server
                testBit = searcherIn.readInt();
                System.out.println("Test bit: " + testBit);


                if(testBit == 99){
                    progressSpot++;
                    publishProgress();
                    System.out.println("Acknowledgment bit received, sending data to server");
                }
                else{
                    //If the host cannot receive a 100, the app will alert the user
                    progressSpot = 4;
                    publishProgress();
                }
            }
            catch(UnknownHostException e){
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            //Alerts the AsyncTask to continue with onPostExecute()
            success = true;
            return success;
        }

        @Override
        protected void onPostExecute(Boolean success) {

            //Only run if doInBackground() completes all the way
            if(success){
                System.out.println("Inside onPostExecute!");
                //Starts the GPS location service
                startGPS();

                //Starts calculating distance
                mHandler.postDelayed(calculateDistance, 0);
            } else {
                System.out.println("Did not successfully finish configuring server connection");
            }

        }
        //Before the AsyncTask actually runs it will post an update to let users know that the
        //selection has been made and the program is now running.
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            publishProgress();

        }

    }


    private class CalculateDistance implements Runnable{

        /*
        * CalculateDistance
        *
        * This class receives data from the server to check if the two devices are close
        * enough to each other to turn on tethering. Because turning on tethering drops
        * server connection, it is very important for the two devices to be within the
        * proper range before activating tethering.
        *
         */

        @Override
        public void run() {


            int receivedData = 0;        //Grabs the data bit that determines what this method will do

            while(true) {

                System.out.println("LOOOP");
                try {
                    System.out.println("Attempting to read coordinates from server..");

                    //Attempts to read coordinates from the host through the server
                    receivedLongitude = searcherIn.readDouble();
                    receivedLatitude = searcherIn.readDouble();
                    System.out.println("Received Longitude: " + receivedLongitude);
                    System.out.println("Received Latitude: " + receivedLatitude);
//                    Location.distanceBetween(latitude, longitude, receivedLatitude,receivedLongitude, bearing);
                    //Displays received coordinates to the user
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            receivedLongitudeText.setText(String.valueOf(receivedLongitude));
                            receivedLatitudeText.setText(String.valueOf(receivedLatitude));
                        }
                    });



                    //300 is the bit that this app needs to read in order to turn on tethering
                    receivedData = searcherIn.readInt();
                    System.out.println("received data: " + receivedData);


                        //Update the user with the approximate distance from each other
                        distanceAway = searcherIn.readDouble();
                        //Implement TextViews to show distance away!
                        System.out.println("distance away: " + distanceAway);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressIndicator.setText("Approximate distance away: \n" + String.valueOf(distanceAway*1000) + "m");
                        }
                    });
                    System.out.println("Finished reading from server");

                } catch (Throwable e){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //This updates the text to display this message. However, it closes too fast to really see it...
                            progressIndicator.setText("Connection ended abruptly. Closing window..");
                        }
                    });
                    e.printStackTrace();
                    try {
                        //Closes all connections to ensure no memory leaks

                        senderSocket.close();
                        searcherIn.close();
                        searcherOut.close();
                        distanceHandler.quit();
                        System.exit(0);
                        finish();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                }
            }
        }
    }

    public class scanWifi implements Runnable{

        @Override
        public void run() {

            WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

            String ssid = wifi.getConnectionInfo().getSSID(); // for geting SSID
            String bssid = wifi.getConnectionInfo().getBSSID(); // for geting BSSID
            int ipAddress = wifi.getConnectionInfo().getIpAddress(); // for geting IP Address
            int rssi = wifi.getConnectionInfo().getRssi(); // for geting RSSI
        }
    }
    public void exit(View view) throws IOException {
        //This onClick listener safely closes all connections to the server when the user presses Exit
        senderSocket.close();
        searcherOut.close();
        searcherIn.close();
        finish();
    }

}
