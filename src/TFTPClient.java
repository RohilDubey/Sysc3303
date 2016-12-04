//TFTPClient.java
//This class is the client side for a very simple assignment based on TFTP on
//UDP/IP. The client uses one port and sends a read or write request and gets 
//the appropriate response from the server.  No actual file transfer takes place.
//based on SampleSolution for assignment1 given the Sept 19th,2016

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlException;

public class TFTPClient extends TFTPHost{

    private DatagramSocket sendReceiveSocket;
    public static final int READ= 1; 
    public static final int WRITE = 2;

    public static String DEFAULT_FILE_PATH = "src/sysc3303/files/";
    
    private int sendPort;
    private Mode run;
    byte requestFormatRead = 1;
    //we can run in normal (send directly to server) or test
    //(send to simulator) mode
    public static enum Mode { NORMAL, TEST};

    public TFTPClient()
    {
        super();
        super.clientPrompt=true;
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

    public void sendAndReceive(int type) throws AlreadyExistsException, UnknownHostException, WriteAccessException
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
            sendPacket = new DatagramPacket(msg, len, InetAddress.getLocalHost(), sendPort);            
        } 
        
        catch (UnknownHostException e) {
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
            	System.out.println("Where is the file location?");
                String saveLocation = sc.next();

                File fileLocation = new File(saveLocation+filename);
                //Path path = Paths.get(saveLocation + filename);
                try {
                	/*
                	if(!fileLocation.canWrite()){
            			System.out.println("Helloa");
            			throw new WriteAccessException("Cannot write: " + filename);      
            		}	*/
                	
                    byte[] resp = new byte[4];
                    receivePacket = new DatagramPacket(resp,4);
                    while (timeout) {//wait to receive the ACK00
                        timeout = false;
                        
                        try {
                        	sendReceiveSocket.setSoTimeout(25000);
                            sendReceiveSocket.receive(receivePacket);
                         
                        } catch (SocketTimeoutException e) {
    						if(rePrompt()==true){
                        		System.out.println("How long would you like to wait for?(Enter 0 for infinite)");
                        		int delayTime = sc.nextInt();    
                        		System.out.println();  
                        		System.out.println("waiting for: "+delayTime+"ms.");   
                        	}
    						else{
    							try{
    								sendReceiveSocket.send(sendPacket);	
    							}
    							catch (IOException a) {
    								a.printStackTrace();
    								System.exit(1);
    							}
    						}
                           		
    					}
    					catch (IOException e) {
    						e.printStackTrace();
    						System.exit(1);
    					}		
                    }
                    
                    printIncomingInfo(receivePacket,"Client",verbose);
                    
                    //Check if error Packet was received
                    if(parseErrorPacket(receivePacket) == true){
                    	System.exit(1);
                    }
                    
                    //Not an error Packet
                    else{
                        //check if packet received is ack00
                        if (resp[0]==(byte)0 && resp[1]==(byte)4 && resp[2]==(byte)0 && resp[3]==(byte)0){
                            //ACK 0 received 
                                BufferedInputStream in = new BufferedInputStream(new FileInputStream(fileLocation));
                                read(in,sendReceiveSocket,receivePacket.getPort());
                                timeout = false;
                                in.close();
                        }
                        else {//Server didn't answer correctly
                            System.out.println("First Ack invalid, shutdown");
                            System.exit(0);
                        }
                    }
                } /*
                catch(FileNotFoundException f){
                	error = createErrorByte((byte)1, filename + "not found. CODE 0501.");
                	//Send error packet
                    sendPacket = new DatagramPacket(error, error.length, InetAddress.getLocalHost(), sendPort);
                    printOutgoingInfo(sendPacket,"Client",verbose);
        		    try {
        			   sendReceiveSocket.send(sendPacket);
        			}
        		    catch (IOException d) {
        			       d.printStackTrace();
        			       System.exit(1);
        			}
        		    System.out.println("Client: packet sent using port " + sendReceiveSocket.getLocalPort());
        		    System.out.println();
        		    System.exit(1);
                }
                catch(WriteAccessException wA){
                	error = createErrorByte((byte)2, "Failed to write the " + filename + ". CODE 0502.");
                	//Send error packet
                    sendPacket = new DatagramPacket(error, error.length, InetAddress.getLocalHost(), sendPort);
                    printOutgoingInfo(sendPacket,"Client",verbose);
        		    try {
        			   sendReceiveSocket.send(sendPacket);
        			}
        		    catch (IOException d) {
        			       d.printStackTrace();
        			       System.exit(1);
        			}
        		    System.out.println("Client: packet sent using port " + sendReceiveSocket.getLocalPort());
        		    System.out.println();
        		    System.exit(1);
                }*/
                catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
            else if (type==READ) {//read request so the client must write on his side            	
            	//here the client doesn't have to wait for ack00 start directly to write
                try {
                	System.out.println("Where would you like to save the file?");

                	
                    String saveLocation = sc.next();/*
                    if(file.exists()){
                		throw new AlreadyExistsException(filename + "already exists in the directory: " + saveLocation + filename + ".");
        			}*/
                	File fileLocation = new File(saveLocation+filename);
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileLocation));
                    write(out,sendReceiveSocket, sendPort, sendPacket);
                    out.close();
                }/*
                catch(AlreadyExistsException a){
                	System.out.print("helafiodsf");
                	error = createErrorByte((byte)6, filename + " already exists. CODE 0506.");
                	//Send error packet
                    sendPacket = new DatagramPacket(error, error.length, InetAddress.getLocalHost(), sendPort);
                    printOutgoingInfo(sendPacket,"Client",verbose);
        		    try {
        			   sendReceiveSocket.send(sendPacket);
        			}
        		    catch (IOException d) {
        			       d.printStackTrace();
        			       System.exit(1);
        			}
        		    System.out.println("Server: packet sent using port " + sendReceiveSocket.getLocalPort());
        		    System.out.println();
                }*/
                catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
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
    public boolean rePrompt() throws UnknownHostException, AlreadyExistsException, WriteAccessException{//TODO A1
    	return super.rePrompt();
    }
  
    public void promptUser() throws AlreadyExistsException, UnknownHostException, WriteAccessException{

        String x;
        System.out.println("(R)ead, (w)rite, (o)ptions, or (q)uit?");
        do{
            x = sc.next();
            if (x.contains("R")||x.contains("r")) {
                sc.reset();
                this.sendAndReceive(READ);
            }
            else if (x.contains("w")||x.contains("W")) {
                sc.reset();
                this.sendAndReceive(WRITE);
            }
            else if (x.contains("q")||x.contains("Q")) {
                this.sendReceiveSocket.close();
                System.out.println("Client is Quitting");
                System.exit(0);
            }
            else if (x.contains("o")||x.contains("O")) {
                System.out.println("Would you like to turn off verbose mode? Y/N");
                x = sc.next();
                sc.reset();
                if (x.contains("y")||x.contains("Y")) {
                    this.verbose = false;
                }
                else if (x.contains("n")||x.contains("N")) {
                    this.verbose = true;
                }
                System.out.println("Would you like to turn on test mode? Y/N");
                x = sc.next();
                sc.reset();
                if (x.contains("y")||x.contains("Y")) {
                    this.run=Mode.TEST;
                }
                else if (x.contains("n")||x.contains("N")) {
                    this.run=Mode.NORMAL;
                }
            }
            System.out.println("(R)ead, (w)rite, (o)ptions, or (q)uit?");
        }while(sc.hasNext()) ;
        sc.close();

    }

    public static void main(String args[]) throws AlreadyExistsException, UnknownHostException, WriteAccessException
    {
        TFTPClient c = new TFTPClient();
        c.promptUser();
    }
}
