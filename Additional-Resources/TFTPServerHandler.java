//TFTPServerHandler.java
//This class represents the thread that handle file transfer for the server based on
//UDP/IP. This thread receive a read or write packet from a client and
//sends back the appropriate response without any actual file transfer.
// 
//based on SampleSolution for assignment1 given the Sept 19th,2016

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class TFTPServerHandler extends TFTPHost implements Runnable {
    // types of requests we can receive
    public static enum Request { READ, WRITE, ERROR};

    // responses for valid requests
    public static final byte[] readResp = {0, 3, 0, 1};
    public static final byte[] writeResp = {0, 4, 0, 0};

    // UDP datagram packets and sockets used to send / receive
    String filename;
    private DatagramSocket sendReceiveSocket;
    boolean readTransfer;
    byte requestFormatRead = 3;
    public TFTPServerHandler(DatagramPacket rp){
        super();
        try {
            // Construct a datagram socket and bind it to any port
            // on the local host machine. This socket will be used to
            //  send and receive UDP Datagram packets.
            sendReceiveSocket = new DatagramSocket();
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }

        this.receivePacket=rp;
        checkFirstMessage();
        //filename=parseFilename(new String(receivePacket.getData(),0,receivePacket.getLength()));
    }

    public void checkFirstMessage(){
        byte[] data=receivePacket.getData(),
        response = new byte[4];

        Request req; // READ, WRITE or ERROR

        String mode;
        int len, j=0, k=0;

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
        else req = Request.ERROR; // bad

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
            //throw new Exception("Not yet implemented");
        }

        printIncomingInfo(receivePacket,"Server",verbose);

        // Construct a datagram packet that is to be sent to a specified port
        // on a specified host.
        // The arguments are:
        //  data - the packet data (a byte array). This is the response.
        //  receivePacket.getLength() - the length of the packet data.
        //     This is the length of the msg we just created.
        //  receivePacket.getAddress() - the Internet address of the
        //     destination host. Since we want to send a packet back to the
        //     client, we extract the address of the machine where the
        //     client is running from the datagram that was sent to us by
        //     the client.
        //  receivePacket.getPort() - the destination port number on the
        //     destination host where the client is running. The client
        //     sends and receives datagrams through the same socket/port,
        //     so we extract the port that the client used to send us the
        //     datagram, and use that as the destination port for the TFTP
        //     packet.

        if (req==Request.WRITE) { //if a write request is received the server must send ACK00
        	sendPacket = new DatagramPacket(response, response.length,
	        receivePacket.getAddress(), receivePacket.getPort());
	
	        printOutgoingInfo(sendPacket,"Server",verbose);
	
	        try {
	            sendReceiveSocket.send(sendPacket);
	        } catch (IOException e) {
	            e.printStackTrace();
	            System.exit(1);
	        }
	
	        System.out.println("Server: packet sent using port " + sendReceiveSocket.getLocalPort());
	        System.out.println();
        }
            

    }

    public void run() {
        if (!readTransfer) {
            write();//server starts to write , the client must read on its side
        }
        else {
            read();//server starts to read and send data, client must write on its side
        }
        System.out.println("File transfer finished => server handler close");
        sendReceiveSocket.close();
    } 

    public void read() { //first packet sent should be data01
        BufferedInputStream in;
        String Home = System.getProperty("user.home");
        String desktop = Home+"/Desktop/";
        		
        try {
            in = new BufferedInputStream(new FileInputStream (desktop+filename));
            super.read(in, sendReceiveSocket, receivePacket.getPort());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write() {//the ACK00 has already been sent , so next packet send must be ack01 when receiving data01
        String newfile = "copy"+(filename); //appends copy because everything's in the same folder on the same computer right now
        BufferedOutputStream out;
        try {
            out = new BufferedOutputStream(new FileOutputStream(newfile));
            super.write(out, sendReceiveSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}