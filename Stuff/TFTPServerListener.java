//TFTPServerListener.java
//This class is the listener process of the server. The Listener use only one socket (69) and wait for any request.
//Once a request is received the listener create a new thread to handle the request.
//Before waiting again a new request the listener check the shutdown command(status)
//If no request is received during tOut, the listener check the shutdown command.

/*error code : 05 opcode : 01=filenotfound/02=access violation /03=disk full  or allocation exceed/04=file already exists
 * + force to happen no error sim , opcode +errorcode + usefull message +0 byte and stop*/

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class TFTPServerListener  extends Thread {

    // UDP datagram packets and sockets used to send / receive
    private DatagramPacket receivePacket;
    private DatagramSocket receiveSocket;

    //timeout
    private final int tOut=100000;//10 sec

    //status (shutdown or not)
    private boolean status;
    private boolean timeout;
   

    public TFTPServerListener()
    {
        super();
        try {
            // Construct a datagram socket and bind it to port 69
            // on the local host machine. This socket will be used to
            // receive UDP Datagram packets.
            receiveSocket = new DatagramSocket(69);
        } catch(SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }
        try {
            receiveSocket.setSoTimeout(tOut);
        }catch (SocketException set){ //TODO Error handling Timeout Exceptions
            set.printStackTrace();
            System.exit(1);
        }

        status=false;
        timeout=true;

    }

    public void setStatus() {
        status = true;
    }
    
    public void run(){
        byte[] data;
        // Current version will prompt during every loop iteration however in future version with GUI
        // A dedicated text box will exist so the prompt is only displayed once
       
        for (;;){

            data = new byte[516];
            receivePacket = new DatagramPacket(data, data.length);

            System.out.println("ServerListener: Waiting for packet.");
            // Block until a datagram packet is received from receiveSocket or the timeout expires
            while(timeout){
	            try {
	                receiveSocket.receive(receivePacket);
	                timeout=false;
	            } catch (InterruptedIOException t){
	                //no request is received during tOut, the listener check the shutdown command.
	                checkStat();
	                timeout=true;
	            }catch (IOException e) {
	                e.printStackTrace();
	                System.exit(1);
	            } 
        	}
            // Before creating a new thread, check if shutdown has been intitiated or not. 
            checkStat();
            // create a new thread to handle the file transfer request
            Thread handler=new Thread(new TFTPServerHandler(receivePacket));
            handler.start();
            timeout=true;
        }

    }

    public void checkStat(){
        if (status==true){
        	System.out.println("Server is now shutting down.");
        	System.exit(1);
        }
    }

}
