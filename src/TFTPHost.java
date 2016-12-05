//Host.java
//This class is the parent class of TFTPClient, TFTPSim, TFTPServer containing 
//the function to print information 
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class TFTPHost {

	protected boolean shutdown;
	protected boolean timeout,clientPrompt, reTransmitFlag;
	protected byte[] error;
	protected String message;
	protected DatagramPacket sendPacket, receivePacket, errorPacket;

	protected static final String[] mtype = {"nothing", "RRQ", "WRQ", "DATA", "ACK", "ERROR" };

	protected static Scanner sc;
	protected boolean verbose;

	// Server folder location
    protected static final String DESKTOP = "C:\\Users\\user\\Desktop\\Server";
    protected static final String USB = "F:\\";
    protected static final String DELETE = "C:\\temp\\";

	protected int delayTime;

	public TFTPHost() {
		delayTime = 25000;
		sc = new Scanner(System.in);
		shutdown = false;
		verbose = true;
		timeout = true;
	}
	
	protected void setShutdown() {
		shutdown = true;
	}
	protected void setClientPrompt(boolean t){
		clientPrompt=t;
	}

	// returns the packet number
	protected int parseBlock(byte[] data) {
		int x = (int) data[2];
		int y = (int) data[3];
		if (x < 0) {
			x = 256 + x;
		}
		if (y < 0) {
			y = 256 + y;
		}
		/*
		 * System.out.println((int) x); System.out.println("-");
		 * System.out.println((int)y);
		 */
		return 256 * x + y;
	}

	// returns the filename
	protected String parseFilename(String data) {
		return data.split("\0")[1].substring(1);
	}
	protected int checkOpcode(DatagramPacket p){
		int opcode = p.getData()[1];
		if(opcode>5||opcode<0){
			System.out.println("");
			System.out.println("ERROR: Invalid opcode value of:"+opcode);
			System.out.println("");
			return opcode=0;
		}
		return opcode;
		
	}

	// prints relevent information about an incoming packet
	protected void printIncomingInfo(DatagramPacket p, String name, boolean verbose) {
		int opcode = checkOpcode(p);
		if (verbose) {
			System.out.println(name + ": packet received.");
			System.out.println("From host: " + p.getAddress());
			System.out.println("Host port: " + p.getPort());
			int len = p.getLength();
			System.out.println("Length: " + len);
			System.out.println("Packet type: " + mtype[opcode]);
			if (opcode == 1 || opcode == 2) {
				System.out.println("Filename: " + parseFilename(new String(p.getData(), 0, len)));
			}
			else if (opcode == 3) {
				System.out.println("Number of bytes: " + (len - 4));
				System.out.println("Block number " + parseBlock(p.getData()));
			}
			else if (opcode == 4){
				System.out.println("Block number " + parseBlock(p.getData()));
			}
			else if (opcode == 5){
				System.out.println("Recieving an Error Message!");	
				if(!parseErrorPacket(p)){
					System.exit(1);
				}
			}
			else if (opcode == 0){
				System.out.println("Sending an unknown TFTP Message!");
			}
			System.out.println();
		}
	}
    public byte[] formatRequest(byte[] filename, byte[] format, int opcode) {
        int lf = filename.length,lm=format.length;
	// Format
        byte [] result=new byte[lf+4+lm];

        result[0] =(byte) 0;
        result[1] = (byte) opcode;
        //System.out.println(opcode);
        System.arraycopy(filename,0,result,2,lf);

        result[lf+2] = 0;

        System.arraycopy(format,0,result,3+lf,lm);

        result[lf+3+lm] = 0;

        return result;
    }
    	
	// prints information about an outgoing packet
	protected void printOutgoingInfo(DatagramPacket p, String name, boolean verbose) {
		int opcode=checkOpcode(p);
		if (verbose) {
			System.out.println(name + ": packet sent.");
			System.out.println("To host: " + p.getAddress());
			System.out.println("Host port: " + p.getPort());
			int len = p.getLength();
			System.out.println("Length: " + len);
			System.out.println("Packet type: " + mtype[opcode]);
			if (opcode == 1 || opcode == 2) {
				System.out.println("Filename: " + parseFilename(new String(p.getData(), 0, len)));
			} 
			else if (opcode == 3){
				System.out.println("Number of bytes: " + (len - 4));
				System.out.println("Block number " + parseBlock(p.getData()));
			}
			else if (opcode == 4){
				System.out.println("Block number " + parseBlock(p.getData()));
			}	
			else if (opcode == 5) {
				System.out.println("Sending an Error Message!");
				if(!parseErrorPacket(p)){
					System.exit(1);
				}
			}
			else if (opcode == 0){
				System.out.println("Sending an unknown TFTP Message!");
			}
			System.out.println();
		}
	}	

	/*
	 * write takes a file outputstream and a communication socket as arguments
	 * it waits for data on the socket and writes it to the file
	 */	
	protected void write(BufferedOutputStream out, DatagramSocket sendReceiveSocket, int simCheck, DatagramPacket sendPacketP, boolean quietToggle) throws IOException, AlreadyExistsException, WriteAccessException {
		byte[] resp = new byte[4];
		resp[0] = 0;
		resp[1] = 4;
		byte[] data = new byte[516];
		int port;
		boolean bool;
		try {
			do {// until receiving a packet <516
				bool = true;
				receivePacket = new DatagramPacket(data, 516);
				// validate and save after we get it
				timeout = true;
				while (timeout) {// wait to receive a data packet
					timeout = false;
					while(bool)
					try {
						bool=false;					
						sendReceiveSocket.setSoTimeout(25000);
						sendReceiveSocket.setSoTimeout(delayTime);	
						delayTime = 25000;
						sendReceiveSocket.receive(receivePacket);
						bool=false;
						if (!validate(receivePacket)) {
							printIncomingInfo(receivePacket, "ERROR", verbose);
							out.close();
							System.exit(0);
						}
					} 
					
					catch (SocketTimeoutException e) {
						if(rePrompt()){
                    		System.out.println("How long would you like to wait for?(Enter 0 for infinite)");
                    		delayTime = sc.nextInt();    
                    		System.out.println();  
                    		System.out.println("waiting for: "+delayTime+"ms.");   
                    		bool= true;
                    	}
						
						else{
							try{
								sendReceiveSocket.send(sendPacketP);	
								printOutgoingInfo(sendPacketP, "Write", verbose);
								bool= true;
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

					port = receivePacket.getPort();
	
					printIncomingInfo(receivePacket, "Write", verbose);
	
					// write the data received and verified on the output file
					out.write(data, 4, receivePacket.getLength() - 4);
					// copy the block number received in the ack response
	
					System.arraycopy(receivePacket.getData(), 2, resp, 2, 2);
					if(simCheck==23){
		                	sendPacket = new DatagramPacket(resp, resp.length, InetAddress.getLocalHost(), simCheck);
		            } 
					else {
		                	sendPacket = new DatagramPacket(resp, resp.length,receivePacket.getAddress(), receivePacket.getPort());
		            }
					
					bool = true;	
					while (bool){
						try {
							sendReceiveSocket.setSoTimeout(delayTime);
							delayTime = 25000;
							sendReceiveSocket.send(sendPacket);
							sendPacketP = sendPacket;
							bool = false;
						} 
						catch (SocketException e) {
							bool= true;
		                	System.out.println("Socket timed out. Will re-send packet");
						}				
						catch (IOException e) {
							bool= true;
							e.printStackTrace();
							System.exit(1);
						}	
					}
					printOutgoingInfo(sendPacket, "Client", verbose);
					parseBlock(sendPacket.getData());
				}
			}
			while (receivePacket.getLength() == 516);
			System.out.println("write: File transfer ends");
			out.close();
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/*
	 * read takes an input stream, a socket and a port as arguments reads data
	 * from the file in 512 byte chunks and sends them over the socket to the
	 * port on localhost
	 */
	protected void read(BufferedInputStream in, DatagramSocket sendReceiveSocket, InetAddress add, int port, boolean quietToggle) throws IOException, AlreadyExistsException, WriteAccessException {
		// here the client has waited the ack00 before starting reading=sending
		// data
		int n;
		byte block1 = 0;
		byte block2 = 0;
		int numberblock = 0;
		byte[] data = new byte[512];
		byte[] resp = new byte[4];
		byte[] message;
		boolean endFile = false;
		DatagramPacket receivePacket = new DatagramPacket(resp, 4);
		try {
			while (((n = in.read(data)) != -1) || endFile == false) {
				numberblock++;
				// create the corresponding block number in 2 bytes
				block1 = (byte) (numberblock / 256);
				block2 = (byte) (numberblock % 256);

				// prepare the data message
				if (n == -1) { // last packet
					message = new byte[4];
					message[0] = 0;
					message[1] = 3;
					message[2] = block1;
					message[3] = block2;
					// create the last packet to be sent
					sendPacket = new DatagramPacket(message, 4, add, port);
					endFile = true;
				} 
				else {
					message = new byte[n + 4];
					message[0] = 0;
					message[1] = 3;
					message[2] = block1;
					message[3] = block2;
					// fill the message array with data read
					for (int i = 0; i < n; i++) {
						message[i + 4] = data[i];
					}
					
					// create the packet containing 03 block# and data
		            sendPacket = new DatagramPacket(message, n + 4, add, port);
		                
					if (n < 512) {
						endFile = true;
					}
				}
				timeout = true;
				// send the data packet
				while(timeout){
					try {
						sendReceiveSocket.send(sendPacket);
						timeout = false;
					} 
					catch (SocketException d) {
						timeout= true;
	                	System.out.println("Socket timed out. Will re-send packet");
					}				
					catch (IOException j) {
						j.printStackTrace();
						System.exit(1);
					}	
				}
				
				printOutgoingInfo(sendPacket, "Read", quietToggle);


				timeout = true;
				while (timeout) {// wait for the ack of the data sent

					timeout = false;

					try {
						sendReceiveSocket.setSoTimeout(25000);
						sendReceiveSocket.receive(receivePacket);						
						timeout = false;
						if (!validate(receivePacket)) {
							printIncomingInfo(receivePacket, "ERROR", quietToggle);
							in.close();
							System.exit(0);
						}
					} 
					catch (SocketTimeoutException x) {
						if(rePrompt()){
                    		System.out.println("How long would you like to wait for?(Enter 0 for infinite)");
                    		int delayTime = sc.nextInt();    
                    		System.out.println();  
                    		System.out.println("waiting for: "+delayTime+"ms.");   
                    		timeout = true;
                    	}
						else{
							try{
								timeout = true;
								sendReceiveSocket.send(sendPacket);	
								System.out.println("Re-sent last packet.");
							}
							catch (IOException a) {
								a.printStackTrace();
								System.exit(1);
							}
						}
                       		
					}
					catch (IOException w) {
						w.printStackTrace();
						System.exit(1);
					}		

				}
					// check if the ack corresponds to the data sent just before
					
				System.out.println("Read : File transfer ends");
			}
		
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}		


	// this function check if a packet is valid : ACK or DATA packet only for
	// now on . RRQ and WRQ checked on ServerHandler
	protected static boolean validate(DatagramPacket receivePacket) {
		byte[] data = receivePacket.getData();
		boolean rep;

		if (data[0] != 0)
			rep = false; // bad
		// check for data packet
		else if (data[1] == (byte) 3) {
			if (receivePacket.getLength() < 4 || receivePacket.getLength() > 516) {
				rep = false;
			} else {
				rep = true;
			}
		}
		// check for ack packet 04+block number
		else if (data[1] == (byte) 4) {
			if (receivePacket.getLength() != 4) {
				rep = false;
			} else {
				rep = true;
			}
		} else {
			rep = false;
		}
		return rep;
	}
	
	//Creates the error buffer
	protected byte[] createErrorByte(byte errorCode, String errorMsg){
		//Creation of error bytes
		byte[] error = new byte[errorMsg.length() + 4 + 1]; 
		//1st 2 bytes are 05
		error[0] = 0; error[1] = 5;
		//Followed by a 0 byte, and errorCode
		error[2] = 0; error[3] = errorCode;
		//received error converted to bytes
		byte[] rError = new byte[errorMsg.length()];
		rError = errorMsg.getBytes();
		//Copy into error bytes
		System.arraycopy(rError, 0, error, 4, rError.length);
		//Last byte must be a 0 byte
		error[error.length -1] = 0;
		return error;
	}//createErrorBytes() ends
	
	// Returns a false if an error packet is not recieved
	protected boolean parseErrorPacket(DatagramPacket e) {

		// Get the bytes of the packet
		e.getData();
		// Get the error message received
		byte[] rError = new byte[e.getData().length - 4];
		System.arraycopy(e.getData(), 4, rError, 0, e.getData().length - 4);

		// Get the error code received
		byte errorCode = e.getData()[3];

		// Display error type to the user
		if (errorCode == 1) {
			System.out.println("Error Packet: 01: File Not Found!");
			// Display the error message
			try {
				message = new String(rError, "UTF-8");
				System.out.println("Error Message: " + message);
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			return true;
		}

		else if (errorCode == 2) {
			System.out.println("Error Packet: 02: Access Violation!");
			// Display the error message
			try {
				message = new String(rError, "UTF-8");
				System.out.println("Error Message: " + message);
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			return true;
		}

		else if (errorCode == 3) {
			System.out.println("Error Packet: 03: Disk Full!");
			// Display the error message
			try {
				message = new String(rError, "UTF-8");
				System.out.println("Error Message: " + message);
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			return true;
		}
		
		else if (errorCode == 4){
			System.out.println("Error Packet: 04: Illegal TFTP Operation!");
			// Display the error message
			try {
				message = new String(rError, "UTF-8");
				System.out.println("Error Message: " + message);
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}	
			return true;
		}
		
		else if (errorCode == 5){
			System.out.println("Error Packet: 05: Unknown Transfer ID!");
			// Display the error message
			try {
				message = new String(rError, "UTF-8");
				System.out.println("Error Message: " + message);
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}		
			return true;
		}
		
		else if (errorCode == 6) {
			System.out.println("Error Packet: 06: File Already Exists!");
			// Display the error message
			try {
				message = new String(rError, "UTF-8");
				System.out.println("Error Message: " + message);
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			return true;
		}
		return false;
	}// parseErrorPacket() ends
	
	//Returns true if expected address and port match the packet's port number and address, otherwise
	//retrns false
	protected boolean checkPort(DatagramPacket dP, int expectedPortNum, InetAddress expectedAddress){
		if(dP.getAddress() == expectedAddress && dP.getPort() == expectedPortNum){
			return true;
		}
		
		else{
			System.out.println("Unknown port!! Unknown Transfer ID. CODE: 0505");
			System.out.println("Transfer ID Received: " + dP.getPort());
			System.out.println("Expected Transfer ID: " + expectedPortNum);
			return false;
		}
	}// checkPort() ends
	 
	 public boolean rePrompt() throws UnknownHostException, AlreadyExistsException, WriteAccessException
	 {//TODO A1
		 boolean waiting = false;	
		 boolean bool = true;
	    	String x;
	    	System.out.println("Would you like to re-transmit [Y]/[N]? or [W]ait");
	        while(bool){
		        x = sc.next();	
	        	if (x.equals("Y")||x.equals("y")) {
			         waiting = false;
			         bool = false;	
		        }
		        else if(x.equals("N")|| x.equals("n")){  
		        		bool = false;
		            	System.out.println("system closing");
		            	System.exit(0);
		            }
		        else if(x.equals("W")||x.equals("w")){
		        	
		        	waiting= true;
		        	bool = false;
		        }
		        else{
		        	System.out.println("Invalid character detected");
		        	System.out.println("Would you like to re-transmit Y/N? or (W)ait");
		        	}	
	        }
	        
	        sc.reset();
	        return waiting;
	        }
	 
	 
}