//TFTPClient.java
//This class is the client side for a very simple assignment based on TFTP on
//UDP/IP. The client uses one port and sends a read or write request and gets 
//the appropriate response from the server.  No actual file transfer takes place.
//based on SampleSolution for assignment1 given the Sept 19th,2016

import java.io.*;
import java.net.*;

public class TFTPClient extends TFTPHost{

    private DatagramSocket sendReceiveSocket;
    public static final int READ= 1; 
    public static final int WRITE = 2;

    public static String DEFAULT_FILE_PATH = "src/sysc3303/files/";
    private String filePath;
    
    private int sendPort;
    private Mode run;
    byte requestFormatRead = 1;
    //we can run in normal (send directly to server) or test
    //(send to simulator) mode
    public static enum Mode { NORMAL, TEST};

    public TFTPClient()
    {
        super();
        try {

            // Construct a datagram socket and bind it to any available
            // port on the local host machine. This socket will be used to
            // send and receive UDP Datagram packets.
            sendReceiveSocket = new DatagramSocket();
        } catch (SocketException se) {   // Can't create the socket.
            se.printStackTrace();
            System.exit(1);
        }
        run=Mode.NORMAL;
    }

    public void sendAndReceive(int type)
    {
        byte[] msg = new byte[100], // message we send
        fn, // filename as an array of bytes
        md, // mode as an array of bytes
        data; // reply as array of bytes
        String filename, mode="Octet"; // filename and mode as Strings
        int j, len;

        if (run==Mode.NORMAL) 
            sendPort = 69;
        else
            sendPort = 23;

        sc.reset();
        System.out.println("Please enter a filename");
        filename=sc.next();

         
        fn=filename.getBytes();
        md=mode.getBytes();

        //form a request with fileame, format, and message type

        msg=formatRequest(fn,md,type);

        len = fn.length+md.length+4; // length of the message
        // length of filename + length of mode + opcode (2) + two 0s (2)

        // Construct a datagram packet that is to be sent to a specified port
        // on a specified host.
        // The arguments are:
        //  msg - the message contained in the packet (the byte array)
        //  the length we care about - k+1
        //  InetAddress.getLocalHost() - the Internet address of the
        //     destination host.
        //     In this example, we want the destination to be the same as
        //     the source (i.e., we want to run the client and server on the
        //     same computer). InetAddress.getLocalHost() returns the Internet
        //     address of the local host.
        //  69 - the destination port number on the destination host.
        try {
            sendPacket = new DatagramPacket(msg, len,
                InetAddress.getLocalHost(), sendPort);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }

        printOutgoingInfo(sendPacket, "Client",verbose);

        // Send the datagram packet to the server via the send/receive socket.
        try {
            sendReceiveSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("Client: Packet sent.");

       //reset timetout
        timeout=true;
        
        // Process the received datagram.
        while(!shutdown){
            if (type==WRITE) {//write request so the client must read on its side
            	//TODO write has to be fixed so file path is specified to fetch file from
            	// also file has to be saved in desktop/server folder 
                try {
                    byte[] resp = new byte[4];
                    receivePacket = new DatagramPacket(resp,4);
                    while (timeout) {//wait to receive the ACK00
                        timeout = false;
                        try {
                        	//sendReceiveSocket.setSoTimeout(10000);
                            sendReceiveSocket.receive(receivePacket);
                        } catch (SocketTimeoutException e) {
                            timeout = true;
                            if (shutdown) {
                                System.exit(0);
                            }
                        }
                    }
                    printIncomingInfo(receivePacket,"Client",verbose);
                    
                    //Check if error Packet was received
                    if(parseErrorPacket(receivePacket) == true){
                    	System.exit(1);
                    //chheck if packet received is ack00
                    if (resp[0]==(byte)0 && resp[1]==(byte)4 && resp[2]==(byte)0 && resp[3]==(byte)0){
                        //ACK 0 received 
                        	sendPort = receivePacket.getPort();
                        	System.out.println(System.getProperty("user.dir"));
                            BufferedInputStream in = new BufferedInputStream(new FileInputStream(filename));
                            read(in,sendReceiveSocket,sendPort);
                            timeout = false;
                            in.close();
                    }
                    
                    //Not an error Packet
                    else{
	                    //check if packet received is ack00
	                    if (resp[0]==(byte)0 && resp[1]==(byte)4 && resp[2]==(byte)0 && resp[3]==(byte)0){
	                        //ACK 0 received 
	                        	sendPort = receivePacket.getPort();
	                        	System.out.println(System.getProperty("user.dir"));
	                            BufferedInputStream in = new BufferedInputStream(new FileInputStream(filename));
	                            read(in,sendReceiveSocket,sendPort);
	                            timeout = false;
	                            in.close();
	                    }
	                    
	                    else {//Server didn't answer correctly
	                    	System.out.println("First Ack invalid, shutdown");
	                    	System.exit(0);
	                    }
                    else {//Server didn't answer correctly
                    	System.out.println("First Ack invalid, shutdown");
                    	System.exit(0);
                    }
                    
                } catch (FileNotFoundException e) {//File Not Found
                	System.out.println("File not found as error packet has been recieved. CODE: 0501");
                	System.exit(1);
                } catch (IOException e) {

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if (type==READ) {//read request so the client must write on his side
            	
            	//here the client doesn't have to wait for ack00 start directly to write
                try {
                	System.out.println("Where would you like to save the file?");
                    String saveLocation = sc.next();
                	File fileLocation = new File(saveLocation+filename); //Check for proper file path !!!!!
                	// use trycatch for above to check for proper filepath 
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileLocation));
                    write(out,sendReceiveSocket);
                    out.close();

                }
                catch (IOException e) {
                    e.printStackTrace();                }
            }
            

            promptUser();

        } // end of loop

        // We're finished, so close the socket.
        sendReceiveSocket.close();
    }

    /*
     * formatRequest takes a filename and a format and an opcode (which corresponds to read or write)
     * and formats them into a correctly formatted request
     */
    public byte[] formatRequest(byte[] filename, byte[] format, int opcode) {
        int lf = filename.length,lm=format.length;

        byte [] result=new byte[lf+4+lm];

        result[0] =(byte) 0;
        result[1] = (byte) opcode;
        System.out.println(opcode);
        System.arraycopy(filename,0,result,2,lf);

        result[lf+2] = 0;

        System.arraycopy(format,0,result,3+lf,lm);

        result[lf+3+lm] = 0;

        return result;
    }

    public void promptUser(){

        String x;

        do{
            System.out.println("(R)ead, (w)rite, (o)ptions, or (q)uit?");
            x = sc.next();
            if (x.contains("R")||x.contains("r")) {
                sc.reset();
                this.sendAndReceive(READ);
                //System.exit(0);
            }
            else if (x.contains("w")||x.contains("W")) {
                sc.reset();
                this.sendAndReceive(WRITE);
                //System.exit(0);
            }
            else if (x.contains("q")||x.contains("Q")) {
                this.sendReceiveSocket.close();
                System.exit(0);
            }
            else if (x.contains("o")||x.contains("O")) {
                System.out.println("Would you like to turn off verbose mode? Y/N");
                x = sc.next();
                sc.reset();
                if (x.contains("y")||x.contains("Y")) {
                    this.verbose = false;
                }
                System.out.println("Would you like to turn on test mode?");
                x = sc.next();
                sc.reset();
                if (x.contains("y")||x.contains("Y")) {
                    this.run=Mode.TEST;
                }
            }
            else {
                sc.reset(); //clear scanner
            }
        }while(sc.hasNext()) ;
        sc.close();

    }

    public static void main(String args[])
    {
//    	Scanner s = new Scanner(System.in);
        TFTPClient c = new TFTPClient();
//        while(true) {
//        	System.out.println("------------------------------------------------------");
//       	System.out.println("File path selection: \n");
//			System.out.println("Enter the name of the file path for the Client to use (if * is typed then src/client/files/ will be used):");
			
//			c.filePath = s.next();
			
//			if (c.filePath.trim().equals("*")) {
//				c.filePath = DEFAULT_FILE_PATH;
//			}
//			s.reset();
//			//Check that file path exists/is a valid directory
//			if (new File(c.filePath).isDirectory()) {
//				break;
//			} else {
//				System.out.println("File path does not exist or is not a directory, try again.");
//			}
//        }
        c.promptUser();
//       s.close();
    }
}