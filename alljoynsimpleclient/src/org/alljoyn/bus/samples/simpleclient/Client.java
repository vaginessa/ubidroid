/*
 * Copyright 2010-2011, Qualcomm Innovation Center, Inc.
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.alljoyn.bus.samples.simpleclient;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.ErrorReplyBusException;
import org.alljoyn.bus.MarshalBusException;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.samples.simpleclient.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class Client extends Activity implements SensorEventListener {
    /* Load the native alljoyn_java library. */
    static {
        System.loadLibrary("alljoyn_java");
    }
    
    private static final int MESSAGE_PING = 1;
    private static final int MESSAGE_PING_REPLY = 2;
    private static final int MESSAGE_POST_TOAST = 3;
    private static final int MESSAGE_START_PROGRESS_DIALOG = 4;
    private static final int MESSAGE_STOP_PROGRESS_DIALOG = 5;
    private static final int MESSAGE_GETPICTURE = 6;
    private static final int MESSAGE_GETPICTURE_REPLY = 7;
    private static final int MESSAGE_GETSENSORDATA = 8;
    private static final int MESSAGE_GETSENSORDATA_REPLY = 9;
    private static final int MESSAGE_GETFEATURE = 10;
    private static final int MESSAGE_GETFEATURE_REPLY = 11;
    private static final String TAG = "UbiDroidClient";

    private EditText mEditText;
    private ArrayAdapter<String> mListViewArrayAdapter;
    private ListView mListView;
    private Menu menu;
    private ImageView iv; 
    private Button mButton;
    //private TextView tv1;
    private Button buttonSensorList;
    private Button buttonAccessVolume;
    private Button changeSlide;
	private String feature = "";
	private boolean usingCamera = false;
	private boolean usingFeatures = false;
	private boolean usingSensorData = false;
    
    //Sensor related variables
    private long lastUpdate;
    private SensorManager sensorManager;
    private boolean color = false;
    /* Handler used to make calls to AllJoyn methods. See onCreate(). */
    private BusHandler mBusHandler;
    
    private ProgressDialog mDialog;
    
    private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case MESSAGE_PING:
                    String ping = (String) msg.obj;
                    mListViewArrayAdapter.add("Ping:  " + ping);
                    break;
                case MESSAGE_PING_REPLY:
                    String ret = (String) msg.obj;
                    mListViewArrayAdapter.add("Reply:  " + ret);
                    mEditText.setText("");
                    break;
                case MESSAGE_POST_TOAST:
                	Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_LONG).show();
                	break;
                case MESSAGE_START_PROGRESS_DIALOG:
                    mDialog = ProgressDialog.show(Client.this, 
                                                  "", 
                                                  "Finding UbiDroid Service.\nPlease wait...", 
                                                  true,
                                                  true);
                    break;
                case MESSAGE_STOP_PROGRESS_DIALOG:
                    mDialog.dismiss();
                    break;
                case MESSAGE_GETPICTURE:
            		String getPicture= (String) msg.obj;
                    mListViewArrayAdapter.add("Picture request sent:  " + getPicture);
                    break;
                case MESSAGE_GETPICTURE_REPLY:
            	   	String retPicture = (String) msg.obj;
                    mListViewArrayAdapter.add("Picture response:  " + retPicture);
                    mEditText.setText("");
                    break;
                case MESSAGE_GETSENSORDATA:
                	String getSensorData = (String) msg.obj;
                	mListViewArrayAdapter.add("Send: " + getSensorData);
                	break;
                case MESSAGE_GETSENSORDATA_REPLY:
                   	mEditText.setText("");
                   	break;
                case MESSAGE_GETFEATURE:
            	    String getFeature= (String) msg.obj;
                    mListViewArrayAdapter.add("Feature request sent:  " + getFeature);
                    break;
               case MESSAGE_GETFEATURE_REPLY:
	        	    String retFeature = (String) msg.obj;
	                mListViewArrayAdapter.add("Feature response:  " + feature);
	                //tv1.setText(feature);
	                mEditText.setText("");
	                break;
                default:
                    break;
                }
            }
        };

    private void resetAccessFlags()
    {
    	usingCamera = false;
    	usingFeatures = false;
    	usingSensorData = false;
    }
        
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mListViewArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mListView = (ListView) findViewById(R.id.ListView);
        mListView.setAdapter(mListViewArrayAdapter);
        
        mButton = (Button)findViewById(R.id.button1);//NEXT PICTURE
        mButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				 Message msg = mBusHandler.obtainMessage(BusHandler.PING, 
                         "n");
				 mBusHandler.sendMessage(msg);
				
			}
		});
        changeSlide = (Button)findViewById(R.id.button2); //PREV SLIDE
        changeSlide.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				 Message msg = mBusHandler.obtainMessage(BusHandler.PING, 
                         "p");
				 mBusHandler.sendMessage(msg);
				
			}
		});
        
        buttonSensorList = (Button) findViewById(R.id.button3); //GET FEATURE
        buttonSensorList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            	resetAccessFlags();
            	/* Call the remote object's Ping method. */
                //Message msg = mBusHandler.obtainMessage(BusHandler.PING, 
                //                                           view.getText().toString());
            	String str = "click";
            	//Message msg = mBusHandler.obtainMessage(BusHandler.GETPICTURE,str);
            	Message msg = mBusHandler.obtainMessage(BusHandler.GETFEATURE,str);
                mBusHandler.sendMessage(msg);
            }
        });
        
        buttonAccessVolume = (Button) findViewById(R.id.GetSensors);//GET SENSOR DATA
        buttonAccessVolume.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				resetAccessFlags();
				usingSensorData = true;
			}
		});
        
        iv = (ImageView)findViewById(R.id.imageView1); //PICTURE THAT GETS RETURNED
        //tv1 = (TextView) findViewById(R.id.TV1); //main.xml has been modified

        
		mEditText = (EditText) findViewById(R.id.EditText); //PING
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_NULL
                        && event.getAction() == KeyEvent.ACTION_UP) {
                        /* Call the remote object's Ping method. */
                        Message msg = mBusHandler.obtainMessage(BusHandler.PING, 
                                                                view.getText().toString());
                        mBusHandler.sendMessage(msg);
                    }
                    return true;
                }
            });
        
        /* Make all AllJoyn calls through a separate handler thread to prevent blocking the UI. */
        HandlerThread busThread = new HandlerThread("BusHandler");
        busThread.start();
        mBusHandler = new BusHandler(busThread.getLooper());

        /* Connect to an AllJoyn object. */
        mBusHandler.sendEmptyMessage(BusHandler.CONNECT);
        mHandler.sendEmptyMessage(MESSAGE_START_PROGRESS_DIALOG);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lastUpdate = System.currentTimeMillis();
    }
    
    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
    	// TODO Auto-generated method stub
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
    //if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
    	getAccelerometer(event);
    }
    @Override
    protected void onResume(){
    	super.onResume();
	    sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
	    SensorManager.SENSOR_DELAY_NORMAL);
    }
    
    @Override
    protected void onPause()
    {
	    super.onPause();
	    sensorManager.unregisterListener(this);
    }
    
    private void getAccelerometer(SensorEvent event){
    	float[] values = event.values;
    
    	float x = values[0];
    	float y = values[1];
    	float z = values[2];
    	
    	float accelerationSquareRoot = (x*x + y*y + z*z) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
    	
  		long actualTime = System.currentTimeMillis();
    	
		if(accelerationSquareRoot >= 2)
		{
			if(actualTime - lastUpdate < 200)
			{
				return;
			}

			lastUpdate = actualTime;
			Toast.makeText(this, "Device was Shuffeled", Toast.LENGTH_SHORT).show();

			if(color)
			{
				//view.setBackgroundColor(Color.GREEN);
			}
			else
			{
				//view.setBackgroundColor(Color.RED);
			}
			color = !color;
			//view.setText("" + x);
			//String str = "X value: " + x;
			//int newX = (int)x;
			if(usingSensorData)
			{
				String str = Float.toString(x); 
				Message msg = mBusHandler.obtainMessage(BusHandler.GETSENSORDATA,str);
				mBusHandler.sendMessage(msg);
			}
		}
   	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        this.menu = menu;
        return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.quit:
	    	finish();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        /* Disconnect to prevent resource leaks. */
        mBusHandler.sendEmptyMessage(BusHandler.DISCONNECT);
    }
    
    /* This class will handle all AllJoyn calls. See onCreate(). */
    class BusHandler extends Handler {    	
        /*
         * Name used as the well-known name and the advertised name of the service this client is
         * interested in.  This name must be a unique name both to the bus and to the network as a
         * whole.
         *
         * The name uses reverse URL style of naming, and matches the name used by the service.
         */
        private static final String SERVICE_NAME = "org.alljoyn.bus.samples.simple";
        private static final short CONTACT_PORT=42;

        private BusAttachment mBus;
        private ProxyBusObject mProxyObj;
        private SimpleInterface mSimpleInterface;
        
        private int 	mSessionId;
        private boolean mIsInASession;
        private boolean mIsConnected;
        private boolean mIsStoppingDiscovery;
        
        /* These are the messages sent to the BusHandler from the UI. */
        public static final int CONNECT = 1;
        public static final int JOIN_SESSION = 2;
        public static final int DISCONNECT = 3;
        public static final int PING = 4;
        public static final int GETPICTURE = 5;
        public static final int GETSENSORDATA = 6;
        public static final int GETFEATURE = 7;

        public BusHandler(Looper looper) {
            super(looper);
            
            mIsInASession = false;
            mIsConnected = false;
            mIsStoppingDiscovery = false;
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            /* Connect to a remote instance of an object implementing the SimpleInterface. */
            case CONNECT: {
            	org.alljoyn.bus.alljoyn.DaemonInit.PrepareDaemon(getApplicationContext());
                /*
                 * All communication through AllJoyn begins with a BusAttachment.
                 *
                 * A BusAttachment needs a name. The actual name is unimportant except for internal
                 * security. As a default we use the class name as the name.
                 *
                 * By default AllJoyn does not allow communication between devices (i.e. bus to bus
                 * communication). The second argument must be set to Receive to allow communication
                 * between devices.
                 */
                mBus = new BusAttachment(getPackageName(), BusAttachment.RemoteMessage.Receive);
                
                /*
                 * Create a bus listener class
                 */
                mBus.registerBusListener(new BusListener() {
                    @Override
                    public void foundAdvertisedName(String name, short transport, String namePrefix) {
                    	logInfo(String.format("MyBusListener.foundAdvertisedName(%s, 0x%04x, %s)", name, transport, namePrefix));
                    	/*
                    	 * This client will only join the first service that it sees advertising
                    	 * the indicated well-known name.  If the program is already a member of 
                    	 * a session (i.e. connected to a service) we will not attempt to join 
                    	 * another session.
                    	 * It is possible to join multiple session however joining multiple 
                    	 * sessions is not shown in this sample. 
                    	 */
                    	if(!mIsConnected) {
                    	    Message msg = obtainMessage(JOIN_SESSION);
                    	    msg.arg1 = transport;
                    	    msg.obj = name;
                    	    sendMessage(msg);
                    	}
                    }
                });

                /* To communicate with AllJoyn objects, we must connect the BusAttachment to the bus. */
                Status status = mBus.connect();
                logStatus("BusAttachment.connect()", status);
                if (Status.OK != status) {
                    finish();
                    return;
                }

                /*
                 * Now find an instance of the AllJoyn object we want to call.  We start by looking for
                 * a name, then connecting to the device that is advertising that name.
                 *
                 * In this case, we are looking for the well-known SERVICE_NAME.
                 */
                status = mBus.findAdvertisedName(SERVICE_NAME);
                logStatus(String.format("BusAttachement.findAdvertisedName(%s)", SERVICE_NAME), status);
                if (Status.OK != status) {
                	finish();
                	return;
                }

                break;
            }
            case (JOIN_SESSION): {
            	/*
                 * If discovery is currently being stopped don't join to any other sessions.
                 */
                if (mIsStoppingDiscovery) {
                    break;
                }
                
                /*
                 * In order to join the session, we need to provide the well-known
                 * contact port.  This is pre-arranged between both sides as part
                 * of the definition of the chat service.  As a result of joining
                 * the session, we get a session identifier which we must use to 
                 * identify the created session communication channel whenever we
                 * talk to the remote side.
                 */
                short contactPort = CONTACT_PORT;
                SessionOpts sessionOpts = new SessionOpts();
                sessionOpts.transports = (short)msg.arg1;
                Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
                
                Status status = mBus.joinSession((String) msg.obj, contactPort, sessionId, sessionOpts, new SessionListener() {
                    @Override
                    public void sessionLost(int sessionId) {
                        mIsConnected = false;
                        logInfo(String.format("MyBusListener.sessionLost(%d)", sessionId));
                        mHandler.sendEmptyMessage(MESSAGE_START_PROGRESS_DIALOG);
                    }
                });
                logStatus("BusAttachment.joinSession() - sessionId: " + sessionId.value, status);
                    
                if (status == Status.OK) {
                	/*
                     * To communicate with an AllJoyn object, we create a ProxyBusObject.  
                     * A ProxyBusObject is composed of a name, path, sessionID and interfaces.
                     * 
                     * This ProxyBusObject is located at the well-known SERVICE_NAME, under path
                     * "/SimpleService", uses sessionID of CONTACT_PORT, and implements the SimpleInterface.
                     */
                	mProxyObj =  mBus.getProxyBusObject(SERVICE_NAME, 
                										"/SimpleService",
                										sessionId.value,
                										new Class<?>[] { SimpleInterface.class });

                	/* We make calls to the methods of the AllJoyn object through one of its interfaces. */
                	mSimpleInterface =  mProxyObj.getInterface(SimpleInterface.class);
                	
                	mSessionId = sessionId.value;
                	mIsConnected = true;
                	mHandler.sendEmptyMessage(MESSAGE_STOP_PROGRESS_DIALOG);
                	
                	try {
						mSimpleInterface.RegisterClient(mBus.getUniqueName());
					} catch (BusException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                }
                break;
            }
            
            /* Release all resources acquired in the connect. */
            case DISCONNECT: {
				try {
					mSimpleInterface.UnRegisterClient(mBus.getUniqueName());
				} catch (BusException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
            	mIsStoppingDiscovery = true;
            	if (mIsConnected) {
                	Status status = mBus.leaveSession(mSessionId);
                    logStatus("BusAttachment.leaveSession()", status);
            	}
            	
                mBus.disconnect();
                getLooper().quit();
                break;
            }
            
            /*
             * Call the service's Ping method through the ProxyBusObject.
             *
             * This will also print the String that was sent to the service and the String that was
             * received from the service to the user interface.
             */
            case PING: {
                try {
                	if (mSimpleInterface != null) {
                		sendUiMessage(MESSAGE_PING, msg.obj);
                		String reply = mSimpleInterface.Ping((String) msg.obj);
                		sendUiMessage(MESSAGE_PING_REPLY, reply);
                	}
                } catch (BusException ex) {
                    logException("SimpleInterface.Ping()", ex);
                }
                break;
            }
            case GETPICTURE: {
                try {
                	if (mSimpleInterface != null) {
                		sendUiMessage(MESSAGE_GETPICTURE, msg.obj); //request sent <<<<<<< .mine
                		//byte[] arr= mSimpleInterface.GetPicture((String) msg.obj); //response from service
                		String pictureResponse = mSimpleInterface.GetPicture((String) msg.obj); //response from service
                		//byte[] arr = pictureResponse.getBytes();
						//Bitmap bmp=BitmapFactory.decodeByteArray(arr,0,arr.length);
						//Bitmap resizedBitmap = Bitmap.createScaledBitmap(bmp, 1024, 1024, false);

			            //iv.setImageBitmap(resizedBitmap);
						//iv.requestFocus();
						//Log.i("TcpClient", "Shown image ");
						//Toast.makeText(getApplicationContext(), "Shown Image", Toast.LENGTH_LONG).show();

                        //String reply = "received picture";
                		//sendUiMessage(MESSAGE_GETPICTURE_REPLY, reply); //response receive=======
                		final String arrs = mSimpleInterface.GetPicture((String) msg.obj); //response from service						
						mHandler.post(new Runnable()
						{	
							public void run() 
							{
								               		
		                		final byte[] arr = arrs.getBytes();
		                		Bitmap bmp=BitmapFactory.decodeByteArray(arr,0,arr.length);
								final Bitmap resizedBitmap = Bitmap.createScaledBitmap(bmp, 1024, 1024, false);
								
								iv.setImageBitmap(resizedBitmap);
								iv.requestFocus();
								Log.i("TcpClient", "Shown image ");
								Toast.makeText(getApplicationContext(), "Shown Image", Toast.LENGTH_LONG).show();                    	   
								
							}
						});
						
						String reply = "received picture";
                		sendUiMessage(MESSAGE_GETPICTURE_REPLY, reply); //response receive>>>>>>> .r69
		
                	}
                } catch (BusException ex) {
                    logException("SimpleInterface.GetPicture()", ex);
                }
                break;
            }
            case GETSENSORDATA: {
                try 
                {
                        if(mSimpleInterface != null) 
                        {
                                sendUiMessage(MESSAGE_GETSENSORDATA, msg.obj);

                                mSimpleInterface.RegisterService(mBus.getUniqueName(), "SensorData");
                                
                                String reply = mSimpleInterface.Ping((String) msg.obj);
                                //sendUiMessage(MESSAGE_GETSENSORDATA_REPLY, reply);
                        }
                }
                catch (BusException ex) 
                {
                        logException("SimpleInterface.SendSensorData()", ex);
                }
            }
            break;
            
            case GETFEATURE:{
            	try {
                	if (mSimpleInterface != null) {
                		sendUiMessage(MESSAGE_GETFEATURE, msg.obj); //request sent

                        mSimpleInterface.RegisterService(mBus.getUniqueName(), "Feature List");
                		String[] featureInfoList = mSimpleInterface.GetFeature(); //response from service
						
						if(featureInfoList == null){
							  Log.e("ERROR","No feature is available");
						}
						else
					    {
							//send feature list to List view and Log
							for (int i=0; i<featureInfoList.length; i++)
							{
								feature += "\n" + featureInfoList[i];
								Log.d("DEBUG", "Feature available: "+ feature); //Consider changing this to actual feature name
								//mListViewArrayAdapter.add(featureName);
							}
						}												
												
						String reply = "received Feature";
	                	sendUiMessage(MESSAGE_GETFEATURE_REPLY, reply); //response receive
			
	                }
                } catch (ErrorReplyBusException ex) {
                    logException("SimpleInterface.GetFeature() errorreply", ex);
                } catch (BusException ex) {
                    logException("SimpleInterface.GetFeature()", ex);
                }
            }
            default:
                break;
            }
        }
        
        /* Helper function to send a message to the UI thread. */
        private void sendUiMessage(int what, Object obj) {
            mHandler.sendMessage(mHandler.obtainMessage(what, obj));
        }

    }

    private void logStatus(String msg, Status status) {
        String log = String.format("%s: %s", msg, status);
        if (status == Status.OK) {
            Log.i(TAG, log);
        } else {
        	Message toastMsg = mHandler.obtainMessage(MESSAGE_POST_TOAST, log);
            mHandler.sendMessage(toastMsg);
            Log.e(TAG, log);
        }
    }

    private void logException(String msg, BusException ex) {
        String log = String.format("%s: %s", msg, ex);
        Message toastMsg = mHandler.obtainMessage(MESSAGE_POST_TOAST, log);
        mHandler.sendMessage(toastMsg);
        Log.e(TAG, log, ex);
    }
    
    /*
     * print the status or result to the Android log. If the result is the expected
     * result only print it to the log.  Otherwise print it to the error log and
     * Sent a Toast to the users screen. 
     */
    private void logInfo(String msg) {
            Log.i(TAG, msg);
    }
}
