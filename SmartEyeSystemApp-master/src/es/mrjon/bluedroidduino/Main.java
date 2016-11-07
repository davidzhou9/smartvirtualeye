package es.mrjon.bluedroidduino;

import java.io.IOException;
import java.util.Locale;
import java.lang.*;
import java.util.*;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Address;
import android.location.Geocoder;
import android.speech.*;
import android.net.*;

@SuppressLint("HandlerLeak")
@TargetApi(Build.VERSION_CODES.ECLAIR)
public class Main extends Activity implements OnInitListener{

  private BluetoothAdapter bluetoothAdapter = null;
  //  private BluetoothConnection connection = null;
  private BluetoothConnection.ConnectionFuture connectionFuture = null;

  private Button gpsButton;
  private Button transmitD;
  private Button transmitC;
  private Button transmitQ;
  private Button speechButton;

  private static final int MY_DATA_CHECK_CODE = 0;
  private static final int REQUEST_CONNECT_DEVICE = 1;
  private static final int REQUEST_ENABLE_BT = 2;
  private static final int REQUEST_SST = 3;
  private TextView displayedTextBox;

  private SoundManager soundmanager;
  private TextToSpeech tts;
  private GPSTracker gps;

  private void debug(String text) {
//    Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    Log.i("Main", text);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);    
    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    // If the adapter is null, then Bluetooth is not supported
    if (bluetoothAdapter == null) {
      debug("Bluetooth is not available");
      finish();
      return;
    }

    //sets up all the widgets

    gpsButton = (Button)findViewById(R.id.gpsButton);
    transmitQ = (Button)findViewById(R.id.button1);
    transmitC = (Button) findViewById(R.id.button_transmit);
    transmitD = (Button) findViewById(R.id.button2);
    speechButton = (Button)findViewById(R.id.speechButton);
    displayedTextBox = (TextView) findViewById(R.id.recieved_text);

    soundmanager = new SoundManager(Main.this);
    gps = new GPSTracker(Main.this);

    transmitD.setOnClickListener(new OnClickListener() { //sends out D for one time data
        public void onClick(View v) {
            try {
                connectionFuture.get().write("D".getBytes());
            } catch (IOException e) {
            }
        }
    });
    transmitC.setOnClickListener(new OnClickListener() { //sends out for continuous data
        public void onClick(View v) {
          //synchronized (transmitTextBox) {
           // Editable transmitText = transmitTextBox.getText();
           // String text = transmitText.toString();
           // transmitText.clear();
            try {
              // Disable and block until this is ready
              connectionFuture.get().write("C".getBytes());
              debug("Wrote message: C");
            } catch (IOException e) {
              debug("Write failed.");
            }
         // }
        }
      });
    
    transmitQ.setOnClickListener(new OnClickListener() { //disconnects from bluetooth

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
			/*try{
				connectionFuture.get().write("Q".getBytes());
				debug("Wrote Message: Q");
			}
			catch (IOException e){
			debug("Write Failed");
			}*/
            try {
                if (connectionFuture.getAdapter().isConnected()) {
                    connectionFuture.get().write("Q".getBytes());
                    connectionFuture.getAdapter().close();
                } else
                    onStart();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    });

    gpsButton.setOnClickListener(new OnClickListener() { //sends out for continuous data
        public void onClick(View v) {
            getMyLocationAddress();
          }
      });

      speechButton.setOnClickListener(new OnClickListener() { //sends out for continuous data
          public void onClick(View v) {
              Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
              intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
              intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Awaiting voice command...");
              startActivityForResult(intent, REQUEST_SST);
          }
      });
  }
  

  public void onStart() {
    super.onStart();
    Intent checkIntent = new Intent(); //sets up speech engine
 	checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
 	startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);
 	
    if (!bluetoothAdapter.isEnabled()) { //starts intent for bluetooth connection
      Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }
    Intent serverIntent = new Intent(this, DeviceListActivity.class);
    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
  }
  

  public void onActivityResult(int requestCode, int resultCode, Intent data) { //checks for multiple results from activities
    debug("result!");
    switch (requestCode) {
    case REQUEST_CONNECT_DEVICE:
      onSelectDeviceActivityResult(resultCode, data);
      break;
    case REQUEST_ENABLE_BT:
      onEnableBluetoothActivityResult(resultCode, data);
      break;
    case MY_DATA_CHECK_CODE:
    	if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) 
		{
			tts = new TextToSpeech(this, this);
			Log.d("onActivityResult", "onInit");
		}
		else		
		{
			Intent installIntent = new Intent();
			installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
			startActivity(installIntent);
		}
    	break;
    case REQUEST_SST:
        if (resultCode == RESULT_OK){
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String command = matches.get(0);
            displayedTextBox.setText(command);
            command = command.trim().toLowerCase();
            if (command.equals("where am i")){
                getMyLocationAddress();
            }
            else if (command.equals("start detecting")){
                try {
                    // Disable and block until this is ready
                    connectionFuture.get().write("C".getBytes());
                    debug("Wrote message: C");
                } catch (IOException e) {
                    debug("Write failed.");
                }
            }
            else if (command.equals("stop")){
                soundmanager.setMuted(true);
            }
            else if (command.equals("restart")){
                soundmanager.setMuted(false);
            }
            else if (command.equals("close connection")){
                try {
                    if  (connectionFuture.getAdapter().isConnected())
                        connectionFuture.get().write("Q".getBytes());
                    connectionFuture.getAdapter().close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            else if (command.equals("restart connection")){
                if (!connectionFuture.getAdapter().isConnected())
                    onStart();
                }
            else if (command.startsWith("navigate to")){
                String address = command.substring(12);
                List<Address> addresses;
                Geocoder geo = new Geocoder(this, Locale.US);
                try {
                    if (address!=null || !address.equals(""))
                        addresses = geo.getFromLocationName(address, 1, -90, -180, 90, 180);
                    else
                        addresses = null;
                }
                catch (IOException e){
                    addresses = null;
                }
                if (addresses!=null){
                    Address temp = addresses.get(0);
                    String latitude = temp.getLatitude()+"";
                    String longitude = temp.getLongitude()+"";
                    Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                            Uri.parse("google.navigation:q="+latitude+","+longitude+"&mode=w"));
                    soundmanager.setMuted(true);
                    startActivity(intent);
                }
            }
            else{
                speakOut("Command not recognized");
            }
        }
    }
  }
  

  private void onEnableBluetoothActivityResult(int resultCode, Intent data) {
    if (resultCode == Activity.RESULT_OK) {
      // do something interesting?
    } else {
      debug("Setting up bluetooth failed.");
      finish();
    }
  }

  private void onSelectDeviceActivityResult(int resultCode, Intent data) {
    if (resultCode == Activity.RESULT_OK) {
      String address = data.getExtras()
        .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
//      debug("GRD");
      debug("extras");

//      debug("Connecting to: " + address);

      Log.i("Main", "Creating connection");
      final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
      connectionFuture = new BluetoothConnection.ConnectionFuture(device, readHandler);
      if (connectionFuture.failed()) {
        debug("Connection failed");
      } else {
        final BluetoothConnection.ConnectionFuture localConnection = connectionFuture;
        Log.i("Main", "Starting AsyncTask");
        new AsyncTask<Integer, Integer, Boolean> () {
        	
			public Boolean doInBackground(Integer... params) {
              localConnection.block();
              Log.i("Main", "done blocking for connection");
              return localConnection.failed();
            }

            public void onPostExecute(Boolean failed) {
              if (!failed) {
                try {
					connectionFuture.get().write("Q".getBytes());
					//startCycle();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();}              
              }
            }
        }.execute();
      }
    }
  }

  private final Handler readHandler = new Handler() { //handles new messages
      @Override
      public void handleMessage(Message msg){
        switch (msg.what) {
            case BluetoothConnection.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                int index1 = readMessage.indexOf("D");
                int index2 = readMessage.indexOf("E");
                int num = 0;
//                mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
//                debug(readMessage);
                if (index1>-1 && index2>-1){ //applies the correction factor to the data
                	double temp = Double.parseDouble(readMessage.substring(index1+1,index2));
                	readMessage = Math.round(temp-(temp*.1521-.4114))+""; 
                	num = (int) Math.round(temp-(temp*.1521-.4114));
                }
                else{
                	displayedTextBox.setText("");
                }
                displayedTextBox.setText("Distance " + num);
                soundmanager.loadSounds(Main.this, num);
                soundmanager.playSound(0);
        }
      }
    };

    @Override
    public void onInit(int status) { //initiates TTS engines
    	// TODO Auto-generated method stub
    	if (status == TextToSpeech.SUCCESS)
    	{
    		tts.setPitch(1.0f); 
    		tts.setSpeechRate((float) 2); 
    		int result = tts.setLanguage(Locale.US);
		
    		if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
    		{
    			Log.e("TTS", "This Language is not supported");
    		}
    	}
    	else
    	{
    		Log.e("TTS", "Initilization Failed!");
    	}
    }

    private void speakOut(String sentence){ //Text to speech. speaks out whatever string is passed
        tts.speak(sentence, TextToSpeech.QUEUE_FLUSH, null);
    }

    public void getMyLocationAddress() {

        Geocoder geocoder= new Geocoder(this, Locale.ENGLISH);
        try{
            //Place your latitude and longitude
            List<Address> addresses;
            if (gps.canGetLocation()) {
                double latitude = gps.getLatitude();
                double longitude = gps.getLongitude();
                addresses = geocoder.getFromLocation(latitude,longitude, 1);
            }
            else{
                addresses = null;
            }
            if(addresses != null) {

                Address fetchedAddress = addresses.get(0);
                StringBuilder strAddress = new StringBuilder();

                for(int i=0; i<fetchedAddress.getMaxAddressLineIndex(); i++) {
                    strAddress.append(fetchedAddress.getAddressLine(i)).append("\n");
                }

                speakOut("You are at "+strAddress.toString());
            }

            else{
                speakOut("Location cannot be determined");
            }
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"Could not get address..!", Toast.LENGTH_LONG).show();
        }
    }
}
 // @Override
  // public boolean onOptionsItemSelected(MenuItem item) {
  //       switch (item.getItemId()) {
  //       case R.id.scan:
  //           // Launch the DeviceListActivity to see devices and do scan
  //           Intent serverIntent = new Intent(this, DeviceListActivity.class);
  //           startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
  //           return true;
  //       case R.id.discoverable:
  //           // Ensure this device is discoverable by others
  //           ensureDiscoverable();
  //           return true;
  //       }
  //       return false;
  //   }

