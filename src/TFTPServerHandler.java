//TFTPServerHandler.java
//This class represents the thread that handle file transfer for the server based on
//UDP/IP. This thread receive a read or write packet from a client and
//sends back the appropriate response without any actual file transfer.
// 


//Necessary imports
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.util.List;



	//Classes Constructor
	public class TFTPServerHandler extends TFTPHost implements Runnable{

	// Types of receivable requests 
	public static enum Request { READ, WRITE, ERROR};

	// responses for valid requests
	public static final byte[] readResp = {0, 3, 0, 1};
	public static final byte[] writeResp = {0, 4, 0, 0};

	//Variable for error flagging
	public boolean boolError = false;
	
    //Error Packet
    public DatagramPacket sendErrorPacket;
  

    // UDP datagram packets and sockets used to send / receive
    String filename;
    private DatagramSocket sendReceiveSocket;
    int writePort;
    boolean readTransfer;
    byte requestFormatRead = 3;
    public TFTPServerHandler(DatagramPacket rp){
        super();
        try {
            // Construct a datagram socket and bind it to any port
            // on the local host machine. This socket will be used to
            //  send and receive UDP Datagram packets.
            sendReceiveSocket = new DatagramSocket();
        } 
        catch (SocketException se){
           se.printStackTrace();
           System.exit(1);
        }
        this.receivePacket=rp;
        checkFirstMessage();
        //filename=parseFilename(new String(receivePacket.getData(),0,receivePacket.getLength()));
    }

	
    	//Checks for any errors and proceeds if there are none 	
    	public void checkFirstMessage(){
        byte[] data=receivePacket.getData(),
        response = new byte[4];
        boolean flag = false;

        Request req; // READ, WRITE or ERROR

        String mode;
        int len, j=0, k=0;

        if(parseErrorPacket(receivePacket)){
        	System.exit(1);
        }
        
        //can be replace by validate message                    
        len = receivePacket.getLength();

        // If it's a read, send back DATA (03) block 1
        // If it's a write, send back ACK (04) block 0
        // Otherwise, ignore it
        if (data[0]!=0) req = Request.ERROR; // bad
        else if (data[1]==1) {
            req = Request.READ; // could be read
            readTransfer=true;
        }
        else if (data[1]==2){
            req = Request.WRITE; // could be write
            readTransfer=false;
        }
        else {
        	req = Request.ERROR; // bad
        	//flag = true;
        }

        if (req!=Request.ERROR) { // check for filename
            // search for next all 0 byte
            for(j=2;j<len;j++) {
                if (data[j] == 0) break;
            }
            if (j==len) req=Request.ERROR; // didn't find a 0 byte
            if (j==2) req=Request.ERROR; // filename is 0 bytes long
            // otherwise, extract filename
            filename = new String(data,2,j-2);
        }
        
        if(req!=Request.ERROR) { // check for mode
		    // search for next all 0 byte
		    for(k=j+1;k<len;k++) { 
			if (data[k] == 0) break;
		    }
		    if (k==len) req=Request.ERROR; // didn't find a 0 byte
		    if (k==j+1) req=Request.ERROR; // mode is 0 bytes long
		    mode = new String(data,j,k-j-1);
		}

		if(k!=len-1) req=Request.ERROR; // other stuff at end of packet        
		// Create a response.
		if (req==Request.READ) { // for Read it's 0301
		    response = readResp;
		} else if (req==Request.WRITE) { // for Write it's 0400
		    response = writeResp;//ACK00
		} else { // it was invalid, just quit
		    //sendError(flag);
		}        	
		
        System.out.println("Valid request as no error has been encountered.");	
		printIncomingInfo(receivePacket,"Server",true);
			
		if (req==Request.WRITE) { //if a write request is received the server must send ACK00
			sendPacket = new DatagramPacket(response, response.length,
			receivePacket.getAddress(), receivePacket.getPort());	
			printOutgoingInfo(sendPacket,"Server",true);
			File file = new File(DESKTOP+"\\"+ parseFilename(new String(receivePacket.getData(), 0, receivePacket.getLength())));
			try {
				sendReceiveSocket.send(sendPacket);
			} 
			catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			System.out.println("Server: packet sent using port " + sendReceiveSocket.getLocalPort());
			System.out.println();
		}
   }
	    
    
    public void run() {
        if (!readTransfer) {
            try {//server starts to write 
				write();
			} 
            catch (WriteAccessException e) {
				e.printStackTrace();
			}
        }
        else {
            try {//server starts to read and send data
				read();
			} 
            catch (AlreadyExistsException e) {
				e.printStackTrace();
			}
            catch (WriteAccessException e) {
				e.printStackTrace();
			}
        }
        System.out.println("File transfer finished");
        System.out.println("Waiting on next transfer...");
        
        
        sendReceiveSocket.close();
        Thread.currentThread().interrupt();
        return;
    } 
    
    //read method
    public void read() throws AlreadyExistsException, WriteAccessException { 
        BufferedInputStream in;      
        File file = new File(DESKTOP + "\\" + filename);
        Path path = file.toPath();
        System.out.println(DESKTOP + "\\" + filename);   
        try {  
            in = new BufferedInputStream(new FileInputStream (DESKTOP + "\\" + filename));
            super.read(in, sendReceiveSocket, receivePacket.getAddress(), receivePacket.getPort(), true);  
        } 
        catch (FileNotFoundException e) {//File Not Found
            
            if(!Files.isReadable(path)){        	
            	error = createErrorByte((byte)2, "Failed to read the " + filename + ". CODE 0502.");
            }
            
            if(!file.exists()){
            	error = createErrorByte((byte)1, filename + " not found. CODE 0501.");
            }
            //Send error packet
            sendPacket = new DatagramPacket(error, error.length, receivePacket.getAddress(), receivePacket.getPort());
            printOutgoingInfo(sendPacket,"Error",verbose);
		    try {
			   sendReceiveSocket.send(sendPacket);
			}
		    catch (IOException d) {
			       d.printStackTrace();
			       System.exit(1);
			}
		    System.out.println("Server: packet sent using port " + sendReceiveSocket.getLocalPort());
		    System.out.println();
		    System.exit(1);
        }
        catch (IOException e) {
        	e.printStackTrace();
        	System.exit(1);
        }
    }	

    
    //write method
    public void write() throws WriteAccessException{//the ACK00 has already been sent , so next packet send must be ack01 when receiving data01
        BufferedOutputStream out;
        //Change for regular operation and other tftp error handling
        File file = new File(DESKTOP+ "\\"+ parseFilename(new String(receivePacket.getData(), 0, receivePacket.getLength())));
        System.out.println(DESKTOP + "\\"+ filename);        
        try {
        	if(file.exists()){				
				throw new AlreadyExistsException(filename + "already exists in the directory: " + DESKTOP + "\\" + parseFilename(new String(receivePacket.getData(), 0, receivePacket.getLength())) +".");				
        	}
        	out = new BufferedOutputStream(new FileOutputStream(DESKTOP + "\\" + filename));
            super.write(out, sendReceiveSocket, writePort, sendPacket, true, DESKTOP + "\\" + filename);
			
         }
        catch(AlreadyExistsException a){
        	error = createErrorByte((byte)6, filename + " already exists. CODE 0506.");
        	//Send error packet
            sendPacket = new DatagramPacket(error, error.length,  receivePacket.getAddress(), receivePacket.getPort());
            printOutgoingInfo(sendPacket,"ERROR",verbose);
            System.out.println(filename + " already exists. CODE 0506.");
		    try {
			   sendReceiveSocket.send(sendPacket);
			}
		    catch (IOException d) {
			       d.printStackTrace();
			       System.exit(1);
			}
		    System.out.println("Server: packet sent using port " + sendReceiveSocket.getLocalPort());
		    System.out.println();
		    System.exit(1);
        }       
        catch (IOException e) {
        	e.printStackTrace();
        }
    }
    
}
