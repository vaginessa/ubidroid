package roman10.tutorial.tcpcommclient;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class TcpClient extends Activity {
    /** Called when the activity is firstx created. */
	Button btn; 
	Thread t = null ;
	ImageView iv;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        btn = (Button)findViewById(R.id.button1);
        iv = (ImageView)findViewById(R.id.imageView1);
        //finish();
        
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                runTcpClientThread();

            }
        });
    }
    
    
    
    private static final String TCP_SERVER_IP = "128.61.34.175";
    private static final int TCP_SERVER_PORT = 4444;
	
    void runTcpClientThread()
    {
    	if(t==null){
	    	t = new Thread(new Runnable() {
	            public void run() {
	            	runTcpClient();// do stuff that doesn't touch the UI here
	            }
    });
    	t.start();}
    }
	
    public void showMsgonui(final String str)
    {
    	runOnUiThread(
				new Runnable()
				{	
					public void run() 
					{
						Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show();
					}
				}
				);
    }
    
    public void showPic(final byte[] arr)
    {
    	runOnUiThread(
				new Runnable()
				{	
					public void run() 
					{				
						Bitmap bmp=BitmapFactory.decodeByteArray(arr,0,arr.length);
						iv.setImageBitmap(bmp);
						Toast.makeText(getApplicationContext(), "Shown Image", Toast.LENGTH_LONG).show();
					}
				}
				);
    }
    
    public byte[] getPicture(InputStream in) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int data;
            while ((data = in.read())>=0) {
                out.write(data);
            }
            return out.toByteArray();
        } catch(IOException ioe) {
            //handle it
        }
        return null;
    }
    
	private void runTcpClient() {
		 
    	try {
    		InetAddress serverAddr = InetAddress.getByName(TCP_SERVER_IP);
			Socket s = new Socket("localhost", TCP_SERVER_PORT);
			
			//BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
			
			//send output
			String outMsg = "click"; 
			out.write(outMsg);
			out.flush();
			Log.i("TcpClient", "sent: " + outMsg);
			showMsgonui("sent: " + outMsg);
			Log.i("TcpClient", "receiving ");
			//accept server response
			
			byte[] pic = getPicture(s.getInputStream());
			showMsgonui("received: pic");
			if(pic == null)
			{
				showMsgonui("received: NULL pic");
			}
			else
			{
				showPic(pic);
				showMsgonui("Shown: pic");
			}
			Log.i("TcpClient", "Shown pic ");
			
			//close connection
			s.close();
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
    }
}