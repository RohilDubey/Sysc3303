import java.io.*;
//Host.java
//This class is the parent class of TFTPClient, TFTPSim, TFTPServer containing 
//the function to print information , common to children.
//Created by Lisa Martini
//September 17th, 2016

import java.net.*;
import java.util.Scanner;

public class TFTPHost {

	protected boolean shutdown;
	protected boolean timeout,clientPrompt;
	protected byte[] error;
	protected String message;
	protected DatagramPacket sendPacket, receivePacket, errorPacket;

	protected static final String[] mtype = {"nothing", "RRQ", "WRQ", "DATA", "ACK", "ERROR" };

	protected static Scanner sc;
	protected boolean verbose;

	public TFTPHost() {
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

	// prints relevent information about an incoming packet
	protected void printIncomingInfo(DatagramPacket p, String name, boolean verbose) {
		if (verbose) {
			int opcode = p.getData()[1];
			System.out.println(name + ": packet received.");
			System.out.println("From host: " + p.getAddress());
			System.out.println("Host port: " + p.getPort());
			int len = p.getLength();
			System.out.println("Length: " + len);
			System.out.println("Packet type: " + mtype[opcode]);
			if (opcode < 3) {
				System.out.println("Filename: " + parseFilename(new String(p.getData(), 0, len)));
			}
			else if (opcode == 3) {
				System.out.println("Number of bytes: " + (len - 4));
			}
			else if (opcode == 5){
				System.out.println("Sending an Error Message!");
				System.out.println("Block number " + parseBlock(p.getData()));
			}
			System.out.println();
		}
	}

	// prints information about an outgoing packet
	protected void printOutgoingInfo(DatagramPacket p, String name, boolean verbose) {
		if (verbose) {
			int opcode = p.getData()[1];
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
			}
			
			else if (opcode == 5) {
				System.out.println("Sending an Error Message!");
				System.out.println("Block number " + parseBlock(p.getData()));
			}
			System.out.println();
		}
	}

	/*
	 * write takes a file outputstream and a communication socket as arguments
	 * it waits for data on the socket and writes it to the file
	 */
	protected void write(BufferedOutputStream out, DatagramSocket sendReceiveSocket, int simCheck) throws IOException {
		byte[] resp = new byte[4];
		resp[0] = 0;
		resp[1] = 4;
		byte[] data = new byte[516];
		int port;
		boolean bool = true;
		try {
			do {// until receiving a packet <516
				receivePacket = new DatagramPacket(data, 516);
				// validate and save after we get it
				timeout = true;
				while (timeout) {// wait to receive a data packet
					timeout = false;
					do{
					try {
						bool=false;
						if(clientPrompt){
						sendReceiveSocket.setSoTimeout(25000);
						}
						sendReceiveSocket.receive(receivePacket);
						if (!validate(receivePacket)) {
							if (!parseErrorPacket(receivePacket)) {
								printIncomingInfo(receivePacket, "ERROR", verbose);
								System.exit(0);
							}
						}
					} 
					catch (SocketTimeoutException e) {// TODO Error handling
						rePrompt();						
					}
					}while(bool);
				}
				
				port = receivePacket.getPort();

				printIncomingInfo(receivePacket, "Write", verbose);

				// write the data received and verified on the output file
				out.write(data, 4, receivePacket.getLength() - 4);

				// copy the block number received in the ack response
				System.arraycopy(receivePacket.getData(), 2, resp, 2, 2);
				  if(simCheck==23){
	                	sendPacket = new DatagramPacket(resp, resp.length,
	                			InetAddress.getLocalHost(), simCheck);
	                } else {
	                	sendPacket = new DatagramPacket(resp, resp.length,
	                            receivePacket.getAddress(), receivePacket.getPort());
	                }
				
				try {
					sendReceiveSocket.setSoTimeout(25000);
					sendReceiveSocket.send(sendPacket);
					System.out.print(sendPacket.getData());
				} 
				catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}

				printOutgoingInfo(sendPacket, this.toString(), verbose);
				parseBlock(sendPacket.getData());

			} while (receivePacket.getLength() == 516);
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
	protected void read(BufferedInputStream in, DatagramSocket sendReceiveSocket, int port) throws IOException {
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
					sendPacket = new DatagramPacket(message, 4, InetAddress.getLocalHost(), port);
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
		            sendPacket = new DatagramPacket(message, n + 4, InetAddress.getLocalHost(), port);
		                
					if (n < 512) endFile = true;
				}
				// send the data packet
				try {
					sendReceiveSocket.send(sendPacket);
				} 
				catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}

				printOutgoingInfo(sendPacket, "Read", verbose);

				// create the receivePacket that should contains the ack or an
				// error

				receivePacket = new DatagramPacket(resp, 4);

				timeout = true;
				while (timeout) {// wait for the ack of the data sent
					timeout = false;
					
					try {
						if(clientPrompt){
						sendReceiveSocket.setSoTimeout(25000);
						}
						sendReceiveSocket.receive(receivePacket);
						if (!validate(receivePacket)) {
							if (!parseErrorPacket(receivePacket)) {
								printIncomingInfo(receivePacket, "ERROR", true);
								System.exit(0);
							}
						}
					} 
					catch (SocketTimeoutException e) {
							rePrompt();
					}
					printIncomingInfo(receivePacket, "Read", verbose);
	
					// check if the ack corresponds to the data sent just before
					if (!(parseBlock(receivePacket.getData()) == parseBlock(message))) {
						System.out.println("ERROR: Acknowledge does not match block sent "
								+ parseBlock(receivePacket.getData()) + "    " + parseBlock(message));
						
		    			error = createErrorByte((byte)4, "Unknown TFTP Error. CODE: 0504");
		    			//Create Error Packet
		        		sendPacket = new DatagramPacket(error, error.length, receivePacket.getAddress(), receivePacket.getPort()); 
		        		sendReceiveSocket.send(sendPacket);
					}
				}
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
	
	protected void rePrompt(){
			Scanner s = new Scanner(System.in);
	    	String x;
	        System.out.println("Would you like to re-transmit Y/N?");
	        x = s.next();
	        if (x.contains("Y")||x.contains("y")) {
	            s.reset();
	            TFTPClient c = new TFTPClient();
	            c.promptUser();
	        }
	        else if(x.contains("N")|| x.contains("n")){
	        	s.reset();
	        	System.out.println("The system is closing");
	        	System.exit(1);
	        }
	        else{
	        	System.out.println("Invalid character detected");
	        	rePrompt();
	        }
	}//rePrompt() ends
}
