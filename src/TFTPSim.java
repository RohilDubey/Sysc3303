
//This class is the beginnings of an error simulator for a simple TFTP server 
//based on UDP/IP. The simulator receives a read or write packet from a client and
//passes it on to the server.  Upon receiving a response, it passes it on to the 
//client.
//One socket (23) is used to receive from the client, and another to send/receive
//from the server.  A new socket is used for each communication back to the client.  
//based on SampleSolution for assignment1 given the Sept 19th,2016

import java.io.*;
import java.net.*;
import java.util.Scanner;
//import java.util.*;

public class TFTPSim extends TFTPHost {

	// UDP datagram packets and sockets used to send / receive
	private String newMode,newFileName,newOpcode;
	private DatagramSocket receiveSocket, sendSocket, sendReceiveSocket;
	private DatagramPacket checkPacket;
	private boolean finalMessage;
	private String packet;
	private int clientPort, serverPort = 69, j = 0, len, debugChoice, actBlock, delay,opcodeC;
	private boolean transferStatus, readTransfer, firstTransfer, lengthCheck,promptCheck;
	private Request req; // READ, WRITE or ERROR
	// clientOrServer true = server, false = client
	private boolean clientOrServer,selectionFlag;
	private byte[] data;
	private Scanner sx = new Scanner(System.in);
	private boolean loseZero;


	// responses for valid requests
	public static final int MAXLENGTH = 516;

	public static enum Request {
		READ, WRITE, ERROR
	};

	public static final byte[] readResp = { 0, 3, 0, 1 };
	public static final byte[] writeResp = { 0, 4, 0, 0 };

	public TFTPSim() {
		super();
		try {
			// Construct a datagram socket and bind it to port 23
			// on the local host machine. This socket will be used to
			// receive UDP Datagram packets from clients.
			receiveSocket = new DatagramSocket(23);
			// Construct a datagram socket and bind it to any available
			// port on the local host machine. This socket will be used to
			// send and receive UDP Datagram packets from the server.
			sendReceiveSocket = new DatagramSocket(0);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	@SuppressWarnings("null")
	public DatagramPacket removeZero(DatagramPacket p){//Only applicable to RRQ/WRQ
		//working : modify center 0 remove center 0,modify last,
		//testing: remove last
		
		int value = 0;
		boolean remove = false;
		int num = 0;
		byte[] data = new byte[100];
		data=p.getData();
		boolean loop = true;
		boolean loop2=true;
		while(loop){
		System.out.println("Which 0 would you like to modify?");
		System.out.println("Enter 1: second 0(BETWEEN FILENAME AND MODE)");
		System.out.println("Enter 2: third 0(END OF MSG)");
		num = sc.nextInt();
		
		/*
		 * The following prompts the user to select which 
		 * 0 the would like to remove
		 * 1st zero is part of 2 byte opcode, we can ignore that since we already
		 * have the change opcode
		 * The next 0 is between the file name and the mode
		 * the last zero is after the mode, you can't remove it since it will just be 0 anyway	
		 */
		
		if(num>2||num<0){
			loop = true;
		}
		else{
			loop =false;
		}
		} 
		
		
		while(loop2){
		System.out.println("Would you like to (R)emove or (C)hange the 0?");
		String dec=sc.next();
		if(dec.equals("R")||dec.equals("r")){
			remove =true;
			loop2=false;
		}
		
		
		else if(dec.equals("c")||dec.equals("C")){
			System.out.println("What value would you like change it to?");
			sc.reset();
			value=sc.nextInt();
			remove=false;
			loop2=false;
		}
		else{
		loop2=true;
		}
		}
		//build the conformation message
		
		System.out.println();
		System.out.print("You have chose to");
		if(remove==true){
			System.out.print(" remove");
		}
		else{
			System.out.print(" modify");
		}
		if(num==2){
			System.out.print(" the final 0");
		}
		if(num==1){
			System.out.print(" the middle 0");
		}
		
		int location=1;
		if(remove){//remove
			while(data[location]!=0){ //iterate through the bytes looking for the first 0
				location++;
			}
			if(num==2){//last zero
				//don't need to do anything since you replace 0 with 0
				
			}
			if(num==1){
				byte[] temp =new byte[100];
				System.arraycopy(data, 0, temp, 0, location);//copy everything before the 0
				System.arraycopy(data, location, temp, location-1, temp.length-location-1);
							
			}
			
		}
		else{//modify
			while(data[location]!=0){
				location++;//iterate through the byte array
			}
			if(num==1){				
				data[location]=(byte) value;	//change the found 0 to whatever the user asked for		
			}
			if(num==2){
				location++;
				while(data[location]!=0){//iterate through a second time to find the 2nd 0
				location++;
			}
				data[location]=(byte) value;	//change the value of the last 0
				
			}
			
			
		}
		
		p.setData(data);	
		
		return p;
	}

	public DatagramPacket changePacket(String newName, String newM, int opC, InetAddress address, int sendPort){//change the packet opcode,mode and filename all at once
		byte[] msg = new byte[100], fn, md, data; 
		DatagramPacket newPacket;
		fn=newName.getBytes();
		md=newM.getBytes();
		//form a request with filename, format, and message type
		msg=formatRequest(fn,md,opC);
		len = fn.length+md.length+4;	
		return newPacket = new DatagramPacket(msg, len,
                address, sendPort);
	}
	
	public void simPrompt() {// menu for choosing errors
		Scanner s = new Scanner(System.in);
		String choice;
		Boolean loop = true;// if the choice is valid, loop becomes false and we
							// won't need to re-choose
		while (loop) {
			System.out.println("What would you like to simulate:");
			System.out.println("Enter 0: Normal Operation");
			System.out.println("Enter 1: Lose a Packet");
			System.out.println("Enter 2: Delay a Packet");
			System.out.println("Enter 3: Duplicate a Packet");
			System.out.println("Enter 4: Invalid TFTP Operation");
			System.out.println("Enter 5: Unknown port");
			choice = sx.next();
			if (choice.equals("0")) {// normal operation
				System.out.println("Normal Operation Selected");
				loop = false;
				debugChoice = 0;
				actBlock = -1; // Set so none of the if statements go off
				return; // Just exit out of method in case of normal operation
			} 
			else if (choice.equals("1")) {// lose packet
				System.out.println("Lose packet selected");
				debugChoice = 1;
				loop = false;
			}
			else if (choice.equals("2")) {// delay packet
				System.out.println("Delay packet selected");
				debugChoice = 2;				
				loop = false;
			}
			else if (choice.equals("3")) {// duplicate packet
				System.out.println("Duplicate Packet selected");
				debugChoice = 3;
				loop = false;
			}
			else if(choice.equals("4")){
				System.out.println("Invalid TFTP operation selected");
				debugChoice =4;
				loop=false;				
			}
			else if (choice.equals("5")) {// duplicate packet
				System.out.println("Unknown Port selected");
				debugChoice = 5;
				loop = false;
			}
			else {// invalid input
				System.out.println("Value entered is not valid");
			}
		}
		
	}
	
	public void blockSelection(){//chose the block being acted on
		String num;		
		if(debugChoice==1){
			packet = selectPacket();
			if (packet.equals("2")) { // specific block (data/ack)
				System.out.println("Which block would you like to lose?");
				num = sx.next();
				actBlock = Integer.parseInt(num);
			} 
			else { // request packet
				actBlock = 0;
			}
		}
		else if(debugChoice==2){
			packet = selectPacket();
			if (packet.equals("2")) { // specific block (data/ack) 
				System.out.println("Which block would you like to delay?");
				num = sx.next();
				actBlock = Integer.parseInt(num);
			} else { // request packet
				actBlock = 0;
			}
			System.out.println("How long do you want to delay the block?");
			sx.reset();
			delay = sx.nextInt();
		}
		else if(debugChoice==3){
			packet = selectPacket();
			if (packet.equals("2")) { // specific block (data/ack)
				System.out.println("Which block would you like to duplicate?");
				num = sx.next();
				actBlock = Integer.parseInt(num);
			} else { // request packet
				actBlock = 0;
			}
		}
		else if(debugChoice==4){
			packet = selectPacket();
			if (packet.equals("2")) { // specific block (data/ack)
				System.out.println("Which block would you like to change?");
				num = sx.next();
				actBlock = Integer.parseInt(num); 
			} else { // request packet
				actBlock = 0;
			}
			if(actBlock == 0){	
					System.out.println("Enter new Opcode");
					newOpcode = sx.next();
				    opcodeC = Integer.parseInt(newOpcode);
				
					System.out.println("Enter new Filename");
					newFileName = sx.next();				
				
					System.out.println("Enter new Mode");
					newMode = sx.next();
			}
			else{
				System.out.println("Enter new Opcode");
				newOpcode = sx.next();
			    opcodeC = Integer.parseInt(newOpcode);
			}
		}
		else if(debugChoice==5){
			packet = selectPacket();
			if (packet.equals("2")) { // specific block (data/ack)
				System.out.println("Which block would you like to change the port on?");
				num = sx.next();
				actBlock = Integer.parseInt(num);
			} else { // request packet
				actBlock = 0;
			}
		}
	}
	
	public int portChange(int port){//Change the port based on input
		System.out.println("Enter the new port that you'd like to use");
		int newPort=sx.nextInt();
		return newPort;
	}

	public boolean checkRequest() {//checks for RRQ/WRQ/ERROR
		byte[] data = receivePacket.getData();
		if (data[0] != 0)
			req = Request.ERROR; // bad
		else if (data[1] == 1) {
			req = Request.READ; // could be read
			readTransfer = true;
		} 
		else if (data[1] == 2) {
			req = Request.WRITE; // could be write
			readTransfer = false;
		}
		return readTransfer;
	}

	public String selectPacket() {//Chose which packets gets acted on
		readTransfer = checkRequest();
		String choice;
		do {
			System.out.println("Would you like to act on the [S]erver or [C]lient?");
			choice = sx.next();
			if (choice.equals("S") || choice.equals("s")) {
				clientOrServer = true;
				selectionFlag = true;
			} else if (choice.equals("C") || choice.equals("c")) {
				clientOrServer = false;
				selectionFlag = true;
			}
		} while (!selectionFlag);
		selectionFlag = false;
		sx.reset();
		System.out.println("Which packet type would you like to act on?");
		do {
			if (readTransfer) {
				System.out.println("[1]: RRQ");
				System.out.println("[2]: DATA/ACK");
			} 
			else {
				System.out.println("[1]: WRQ");
				System.out.println("[2]: DATA/ACK");
			}
			choice = sx.next();
			if (choice.equals("1") && readTransfer && !clientOrServer){
				System.out.println("------RRQ SELECTED------");
				selectionFlag = true;
			}
			else if(choice.equals("1") && !readTransfer && !clientOrServer){
				System.out.println("------WRQ SELECTED------");
				selectionFlag=true;
			}
			else if(choice.equals("2")){
				System.out.println("------DATA/ACK SELECTED------");
				if(clientOrServer){
					System.out.println("------SERVER------");
				}
				else{
					System.out.println("------CLIENT------");
				}
				selectionFlag=true;
			}
			else {
				System.out.println("Choice is invalid, please choose again");
				selectionFlag = false;
			}
		} while (!selectionFlag);
		selectionFlag = false;
		return choice;
	}
		
	public void delayPacket(){//will delay for a specified amount of time, may cause timeoutException that will be handled
		
		transferStatus = false;
		finalMessage = false;
		firstTransfer = false;
		lengthCheck = false;	
		byte[] data;			
				
			do {
				data = new byte[516];
				receivePacket = new DatagramPacket(data, data.length);
				System.out.println("Simulator: Waiting for packet.");	
				try {
					receiveSocket.receive(receivePacket);//wait until you receive a packet
				} 
				catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				if (!transferStatus && this.promptCheck) {//if already prompted 
					blockSelection(); 
					firstTransfer = true;
				}
				else if(!promptCheck){
					this.promptCheck =true;
					simPrompt();
					filter();
				}	
				printIncomingInfo(receivePacket, "Simulator", verbose);//print the received packet details
				len = receivePacket.getLength();
				clientPort = receivePacket.getPort();
				sendPacket = new DatagramPacket(data, len, receivePacket.getAddress(), serverPort);
				len = sendPacket.getLength();
				// Send the datagram packet to the server via the send/receive
				// socket.
				if (readTransfer==false) {//if write
					checkPacket = sendPacket;
				}
				// Debug options for Client
				if ((actBlock == 0 && (firstTransfer) && !clientOrServer)) { //1st block for client request						
					try {
						System.out.println();
						System.out.println("Delaying for: "+delay+"ms.");
						System.out.println();
						Thread.sleep(delay);
					} 
					catch (InterruptedException e) {
					}	
					printOutgoingInfo(sendPacket, "Simulator", verbose);
						try {
							sendReceiveSocket.send(sendPacket);
						} 
						catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
					firstTransfer = false;
				}
				else if ((actBlock == parseBlock(sendPacket.getData()) && !clientOrServer)) { //nth block of client request
						System.out.println();
						System.out.println("Delaying for: "+delay+"ms.");
						System.out.println();
						printOutgoingInfo(sendPacket, "Simulator", verbose);
						try {
							sendReceiveSocket.send(sendPacket);
						}
						catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
					if (!finalMessage && lengthCheck) {//check if this was the last block 
						finalMessage = true;
					}
				}
				else {//this wasn't the act block
					printOutgoingInfo(sendPacket, "Simulator", verbose);
					try {
						sendReceiveSocket.send(sendPacket);
					}
					catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
				}
				// Construct a DatagramPacket for receiving packets up
				// to 100 bytes long (the length of the byte array)
				if (!finalMessage) {
					data = new byte[516];
					receivePacket = new DatagramPacket(data, data.length);
					System.out.println("Simulator: Waiting for packet.");
					try {
						sendReceiveSocket.receive(receivePacket);
					}
					catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
					serverPort = receivePacket.getPort();
					printIncomingInfo(receivePacket, "Simulator", verbose);
					len = receivePacket.getLength();
					sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(),clientPort);
					printOutgoingInfo(sendPacket, "Simulator", verbose);
					len = sendPacket.getLength();

					// Send the datagram packet to the client via a new socket.
						/*
						 * Construct a new datagram socket and bind it to any port
						 * on the local host machine. This socket will be used
						 * to send UDP Datagram packets.
						 *  
						 */
					try {
						sendSocket = new DatagramSocket();
					}
					catch (SocketException se) {
						se.printStackTrace();
						System.exit(1);
					}
					if (readTransfer) {//if read
						checkPacket = sendPacket;
					}
					// Debug options for Server
					if ((actBlock == 0 && (firstTransfer) && clientOrServer)) { //1st block for serverOperations
							System.out.println();
							System.out.println("Delaying for: "+delay+"ms.");
							System.out.println();
							printOutgoingInfo(sendPacket, "Simulator", verbose);
							try {
								sendReceiveSocket.send(sendPacket);
							} catch (IOException e) {
								e.printStackTrace();
								System.exit(1);
							}
							
						firstTransfer = false;
					}

					else if ((actBlock == parseBlock(sendPacket.getData()) && clientOrServer)) { //nth block of server side operations
							System.out.println();
							System.out.println("Delaying for: "+delay+"ms.");
							System.out.println();
							printOutgoingInfo(sendPacket, "Simulator", verbose);
							try {
								sendSocket.send(sendPacket);
							} catch (IOException e) {
								e.printStackTrace();
								System.exit(1);
							}

						
						if (!finalMessage && lengthCheck) {
							finalMessage = true;
						}
					} 
					else {
						printOutgoingInfo(sendPacket, "Simulator", verbose);
						try {
							sendSocket.send(sendPacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
					}
				}

				System.out.println("Simulator: packet sent using port " + sendSocket.getLocalPort());
				System.out.println();
				transferStatus = true;
				if (checkPacket.getLength() == MAXLENGTH) {
					lengthCheck = true;
				}
			} while ((lengthCheck && !finalMessage) || (firstTransfer && !readTransfer));
			//System.out.println("error3");
			// We're finished with this socket, so close it.
			transferStatus = true;
			sendSocket.close();
			simPrompt();
	}
	
	public void losePacket(){//instead of sending a packet, when the actBlock is reached it skip the send
		transferStatus = false;
		finalMessage = false;
		firstTransfer = false;
		lengthCheck = false;
		byte[] data;
			do {
				data = new byte[516];
				receivePacket = new DatagramPacket(data, data.length);
				System.out.println("Simulator: Waiting for packet.");
				
				try {
					receiveSocket.receive(receivePacket);//wait untill you receive a packet
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				
				
				
				if (!transferStatus && promptCheck) {//if already prompted 
					blockSelection(); 
					firstTransfer = true;
				}
				else if(!promptCheck){
					this.promptCheck =true;
					this.simPrompt();
					this.filter();
				}
				
				printIncomingInfo(receivePacket, "Simulator", verbose);//print the received packet details
				len = receivePacket.getLength();
				clientPort = receivePacket.getPort();
				sendPacket = new DatagramPacket(data, len, receivePacket.getAddress(), serverPort);
				len = sendPacket.getLength();
				// Send the datagram packet to the server via the send/receive
				// socket.
				if (readTransfer==false) {//if write
					checkPacket = sendPacket;
				}
				// Debug options for Client
				if ((actBlock == 0 && (firstTransfer) && !clientOrServer)) { //1st block for client request						
					System.out.println();//lost this packet therefore do nothing
					firstTransfer = false;
				}

				else if ((actBlock == parseBlock(sendPacket.getData()) && !clientOrServer)) { //nth block of client request
						System.out.println();//lost this packet therefore do nothing
						
					if (!finalMessage && lengthCheck) {//check if this was the last block 
						finalMessage = true;
					}
				}
				else {//this wasn't the act block
					printOutgoingInfo(sendPacket, "Simulator", verbose);
					try {
						sendReceiveSocket.send(sendPacket);
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
				}

				// Construct a DatagramPacket for receiving packets up
				// to 100 bytes long (the length of the byte array)
				if (!finalMessage) {
					data = new byte[516];
					receivePacket = new DatagramPacket(data, data.length);
					System.out.println("Simulator: Waiting for packet.");
					try {
						sendReceiveSocket.receive(receivePacket);
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
					
					serverPort = receivePacket.getPort();
					printIncomingInfo(receivePacket, "Simulator", verbose);
					len = receivePacket.getLength();
					sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(),clientPort);
					printOutgoingInfo(sendPacket, "Simulator", verbose);
					len = sendPacket.getLength();

					// Send the datagram packet to the client via a new socket.

					
						/*
						 * Construct a new datagram socket and bind it to any port
						 * on the local host machine. This socket will be used
						 * to send UDP Datagram packets.
						 *  
						 */
					try {
						sendSocket = new DatagramSocket();
					} catch (SocketException se) {
						se.printStackTrace();
						System.exit(1);
					}
					if (readTransfer) {//if read
						checkPacket = sendPacket;
					}
					// Debug options for Server
					if ((actBlock == 0 && (firstTransfer) && clientOrServer)) { //1st block for serverOperations
						
						System.out.println();//lost this packet therefore do nothing
						firstTransfer = false;
					}

					else if ((actBlock == parseBlock(sendPacket.getData()) && clientOrServer)) { //nth block of server side operations
						System.out.println();//lost this packet therefore do nothing
						
						if (!finalMessage && lengthCheck) {
							finalMessage = true;
						}
					} 
					else {
						printOutgoingInfo(sendPacket, "Simulator", verbose);
						try {
							sendSocket.send(sendPacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
					}
				}

				System.out.println("Simulator: packet sent using port " + sendSocket.getLocalPort());
				System.out.println();
				transferStatus = true;
				if (checkPacket.getLength() == MAXLENGTH) {
					lengthCheck = true;
				}
			} while ((lengthCheck && !finalMessage) || (firstTransfer && !readTransfer));
			//System.out.println("error3");
			// We're finished with this socket, so close it.
			transferStatus = true;
			sendSocket.close();
			simPrompt();
	}
	
	public void duplicatePacket(){//send off 2 packets instead of 1
		
			transferStatus = false;
			finalMessage = false;
			firstTransfer = false;
			lengthCheck = false;
						
		
		byte[] data;
			do {
				data = new byte[516];
				receivePacket = new DatagramPacket(data, data.length);
				System.out.println("Simulator: Waiting for packet.");
				
				try {
					receiveSocket.receive(receivePacket);//wait until you receive a packet
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				
				printIncomingInfo(receivePacket, "Simulator", verbose);//print the received packet details
				len = receivePacket.getLength();
				clientPort = receivePacket.getPort();
				sendPacket = new DatagramPacket(data, len, receivePacket.getAddress(), serverPort);
				len = sendPacket.getLength();
				// Send the datagram packet to the server via the send/receive
				// socket.
				if (readTransfer==false) {//if write
					checkPacket = sendPacket;
				}
				// Debug options for Client
				if ((actBlock == 0 && (firstTransfer) && !clientOrServer)) { //1st block for client request						
						try {
							sendReceiveSocket.send(sendPacket);
							printOutgoingInfo(sendPacket, "Simulator", verbose);
							sendReceiveSocket.send(sendPacket);
							printOutgoingInfo(sendPacket, "Simulator", verbose);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
					firstTransfer = false;
				}

				else if ((actBlock == parseBlock(sendPacket.getData()) && !clientOrServer)) { //nth block of client request
						printOutgoingInfo(sendPacket, "Simulator", verbose);
						printOutgoingInfo(sendPacket, "Simulator", verbose);
						try {
							sendReceiveSocket.send(sendPacket);
							sendReceiveSocket.send(sendPacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}

					 
					if (!finalMessage && lengthCheck) {//check if this was the last block 
						finalMessage = true;
					}
				}
				else {//this wasn't the act block
					printOutgoingInfo(sendPacket, "Simulator", verbose);
					try {
						sendReceiveSocket.send(sendPacket);
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
				}

				// Construct a DatagramPacket for receiving packets up
				// to 100 bytes long (the length of the byte array)
				if (!finalMessage) {
					data = new byte[516];
					receivePacket = new DatagramPacket(data, data.length);
					System.out.println("Simulator: Waiting for packet.");
					try {
						sendReceiveSocket.receive(receivePacket);
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
					
					serverPort = receivePacket.getPort();
					printIncomingInfo(receivePacket, "Simulator", verbose);
					len = receivePacket.getLength();
					sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(),clientPort);
					printOutgoingInfo(sendPacket, "Simulator", verbose);
					len = sendPacket.getLength();

					// Send the datagram packet to the client via a new socket.

					
						/*
						 * Construct a new datagram socket and bind it to any port
						 * on the local host machine. This socket will be used
						 * to send UDP Datagram packets.
						 *  
						 */
					try {
						sendSocket = new DatagramSocket();
					} catch (SocketException se) {
						se.printStackTrace();
						System.exit(1);
					}
					if (readTransfer) {//if read
						checkPacket = sendPacket;
					}
					// Debug options for Server
					if ((actBlock == 0 && (firstTransfer) && clientOrServer)) { //1st block for serverOperations
							printOutgoingInfo(sendPacket, "Simulator", verbose);
							printOutgoingInfo(sendPacket, "Simulator", verbose);
							try {
								sendReceiveSocket.send(sendPacket);
								sendReceiveSocket.send(sendPacket);
							} catch (IOException e) {
								e.printStackTrace();
								System.exit(1);
							}
							
						firstTransfer = false;
					}

					else if ((actBlock == parseBlock(sendPacket.getData()) && clientOrServer)) { //nth block of server side operations
							printOutgoingInfo(sendPacket, "Simulator", verbose);
							printOutgoingInfo(sendPacket, "Simulator", verbose);
							try {
								sendSocket.send(sendPacket);
								sendSocket.send(sendPacket);
							} catch (IOException e) {
								e.printStackTrace();
								System.exit(1);
							}

						
						if (!finalMessage && lengthCheck) {
							finalMessage = true;
						}
					} 
					else {
						printOutgoingInfo(sendPacket, "Simulator", verbose);
						try {
							sendSocket.send(sendPacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
					}
				}

				System.out.println("Simulator: packet sent using port " + sendSocket.getLocalPort());
				System.out.println();
				transferStatus = true;
				if (checkPacket.getLength() < MAXLENGTH) {
					lengthCheck = true;
				}
			} while ((lengthCheck && !finalMessage) || (firstTransfer && !readTransfer));
			//System.out.println("error3");
			// We're finished with this socket, so close it.
			transferStatus = true;
			sendSocket.close();
			simPrompt();
	}
	
	public void passPacket(){//basic pass
		transferStatus = false;
		finalMessage = false;
		firstTransfer = false;
		lengthCheck = false;
		byte[] data;
			do {
				data = new byte[516];
				receivePacket = new DatagramPacket(data, data.length);
				System.out.println("Simulator: Waiting for packet.");
				
				try {
					receiveSocket.receive(receivePacket);//wait untill you receive a packet
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				
				
				
				if (!transferStatus && promptCheck) {//if already prompted 
					blockSelection(); 
					firstTransfer = true;
				}
				else if(!promptCheck){
					this.promptCheck =true;
					this.simPrompt();
					this.filter();
				}
				
				printIncomingInfo(receivePacket, "Simulator", verbose);//print the received packet details
				len = receivePacket.getLength();
				clientPort = receivePacket.getPort();
				sendPacket = new DatagramPacket(data, len, receivePacket.getAddress(), serverPort);
				len = sendPacket.getLength();
				// Send the datagram packet to the server via the send/receive
				// socket.
				if (readTransfer==false) {//if write
					checkPacket = sendPacket;
				}
				// Debug options for Client
				if ((actBlock == 0 && (firstTransfer) && !clientOrServer)) { //1st block for client request						
						printOutgoingInfo(sendPacket, "Simulator", verbose);
						try {
							sendReceiveSocket.send(sendPacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
					firstTransfer = false;
				}

				else if ((actBlock == parseBlock(sendPacket.getData()) && !clientOrServer)) { //nth block of client request
					
						printOutgoingInfo(sendPacket, "Simulator", verbose);
						try {
							sendReceiveSocket.send(sendPacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}

					 
					if (!finalMessage && lengthCheck) {//check if this was the last block 
						finalMessage = true;
					}
				}
				else {//this wasn't the act block
					printOutgoingInfo(sendPacket, "Simulator", verbose);
					try {
						sendReceiveSocket.send(sendPacket);
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
				}

				// Construct a DatagramPacket for receiving packets up
				// to 100 bytes long (the length of the byte array)
				if (!finalMessage) {
					data = new byte[516];
					receivePacket = new DatagramPacket(data, data.length);
					System.out.println("Simulator: Waiting for packet.");
					try {
						sendReceiveSocket.receive(receivePacket);
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
					
					serverPort = receivePacket.getPort();
					printIncomingInfo(receivePacket, "Simulator", verbose);
					len = receivePacket.getLength();
					sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(),clientPort);
					printOutgoingInfo(sendPacket, "Simulator", verbose);
					len = sendPacket.getLength();

					// Send the datagram packet to the client via a new socket.

					
						/*
						 * Construct a new datagram socket and bind it to any port
						 * on the local host machine. This socket will be used
						 * to send UDP Datagram packets.
						 *  
						 */
					try {
						sendSocket = new DatagramSocket();
					} catch (SocketException se) {
						se.printStackTrace();
						System.exit(1);
					}
					if (readTransfer) {//if read
						checkPacket = sendPacket;
					}
					// Debug options for Server
					if ((actBlock == 0 && (firstTransfer) && clientOrServer)) { //1st block for serverOperations
						
							printOutgoingInfo(sendPacket, "Simulator", verbose);
							try {
								sendReceiveSocket.send(sendPacket);
							} catch (IOException e) {
								e.printStackTrace();
								System.exit(1);
							}
							
						firstTransfer = false;
					}

					else if ((actBlock == parseBlock(sendPacket.getData()) && clientOrServer)) { //nth block of server side operations
						 
							printOutgoingInfo(sendPacket, "Simulator", verbose);
							try {
								sendSocket.send(sendPacket);
							} catch (IOException e) {
								e.printStackTrace();
								System.exit(1);
							}

						
						if (!finalMessage && lengthCheck) {
							finalMessage = true;
						}
					} 
					else {
						printOutgoingInfo(sendPacket, "Simulator", verbose);
						try {
							sendSocket.send(sendPacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
					}
				}

				System.out.println("Simulator: packet sent using port " + sendSocket.getLocalPort());
				System.out.println();
				transferStatus = true;
				if (checkPacket.getLength() == MAXLENGTH) {
					lengthCheck = true;
				}
			} while ((lengthCheck && !finalMessage) || (firstTransfer && !readTransfer));
			//System.out.println("error3");
			// We're finished with this socket, so close it.
			transferStatus = true;
			sendSocket.close();
			simPrompt();
	}
		
	public void invalidPort(){//invalid port
		transferStatus = false;
		finalMessage = false;
		firstTransfer = false;
		lengthCheck = false;
		byte[] data;
			do {
				data = new byte[516];
				receivePacket = new DatagramPacket(data, data.length);
				System.out.println("Simulator: Waiting for packet.");
				
				try {
					receiveSocket.receive(receivePacket);//wait untill you receive a packet
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				
				
				
				if (!transferStatus && promptCheck) {//if already prompted 
					blockSelection(); 
					firstTransfer = true;
				}
				else if(!promptCheck){
					this.promptCheck =true;
					this.simPrompt();
					this.filter();
				}
				
				printIncomingInfo(receivePacket, "Simulator", verbose);//print the received packet details
				len = receivePacket.getLength();
				clientPort = receivePacket.getPort();
				sendPacket = new DatagramPacket(data, len, receivePacket.getAddress(), serverPort);
				len = sendPacket.getLength();
				// Send the datagram packet to the server via the send/receive
				// socket.
				if (readTransfer==false) {//if write
					checkPacket = sendPacket;
				}
				// Debug options for Client
				if ((actBlock == 0 && (firstTransfer) && !clientOrServer)) { //1st block for client request	
						int newPort=portChange(clientPort);
						sendPacket.setPort(newPort);
						printOutgoingInfo(sendPacket, "Simulator", verbose);
						try {
							sendReceiveSocket.send(sendPacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
					firstTransfer = false;
				}

				else if ((actBlock == parseBlock(sendPacket.getData()) && !clientOrServer)) { //nth block of client request
						int newPort=portChange(clientPort);
						sendPacket.setPort(newPort);
						printOutgoingInfo(sendPacket, "Simulator", verbose);
						try {
							sendReceiveSocket.send(sendPacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}

					 
					if (!finalMessage && lengthCheck) {//check if this was the last block 
						finalMessage = true;
					}
				}
				else {//this wasn't the act block
					printOutgoingInfo(sendPacket, "Simulator", verbose);
					try {
						sendReceiveSocket.send(sendPacket);
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
				}

				// Construct a DatagramPacket for receiving packets up
				// to 100 bytes long (the length of the byte array)
				if (!finalMessage) {
					data = new byte[516];
					receivePacket = new DatagramPacket(data, data.length);
					System.out.println("Simulator: Waiting for packet.");
					try {
						sendReceiveSocket.receive(receivePacket);
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
					
					serverPort = receivePacket.getPort();
					printIncomingInfo(receivePacket, "Simulator", verbose);
					len = receivePacket.getLength();
					sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(),clientPort);
					printOutgoingInfo(sendPacket, "Simulator", verbose);
					len = sendPacket.getLength();

					// Send the datagram packet to the client via a new socket.

					
						/*
						 * Construct a new datagram socket and bind it to any port
						 * on the local host machine. This socket will be used
						 * to send UDP Datagram packets.
						 *  
						 */
					try {
						sendSocket = new DatagramSocket();
					} catch (SocketException se) {
						se.printStackTrace();
						System.exit(1);
					}
					if (readTransfer) {//if read
						checkPacket = sendPacket;
					}
					// Debug options for Server
					if ((actBlock == 0 && (firstTransfer) && clientOrServer)) { //1st block for serverOperations
							int newPort=portChange(serverPort);
							sendPacket.setPort(newPort);
							printOutgoingInfo(sendPacket, "Simulator", verbose);
							try {
								sendReceiveSocket.send(sendPacket);
							} catch (IOException e) {
								e.printStackTrace();
								System.exit(1);
							}
							
						firstTransfer = false;
					}

					else if ((actBlock == parseBlock(sendPacket.getData()) && clientOrServer)) { //nth block of server side operations
							int newPort=portChange(serverPort);
							sendPacket.setPort(newPort);
							printOutgoingInfo(sendPacket, "Simulator", verbose);
							try {
								sendSocket.send(sendPacket);
							} catch (IOException e) {
								e.printStackTrace();
								System.exit(1);
							}

						
						if (!finalMessage && lengthCheck) {
							finalMessage = true;
						}
					} 
					else {
						printOutgoingInfo(sendPacket, "Simulator", verbose);
						try {
							sendSocket.send(sendPacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
					}
				}

				System.out.println("Simulator: packet sent using port " + sendSocket.getLocalPort());
				System.out.println();
				transferStatus = true;
				if (checkPacket.getLength() == MAXLENGTH) {
					lengthCheck = true;
				}
			} while ((lengthCheck && !finalMessage) || (firstTransfer && !readTransfer));
			//System.out.println("error3");
			// We're finished with this socket, so close it.
			transferStatus = true;
			sendSocket.close();
			simPrompt();
	}
	
	public void opcodePacket(){//this is for creating all the illegal TFTP 
		/*
		 * this is for creating all the illegal TFTP 
		 * new Opcode,new fileName,new mode,delete/modify 0  
		 * 
		 */
		transferStatus = false;
		finalMessage = false;
		firstTransfer = false;
		lengthCheck = false;
					
	
	byte[] data;
		do {
			data = new byte[516];
			receivePacket = new DatagramPacket(data, data.length);
			System.out.println("Simulator: Waiting for packet.");
			
			try {
				receiveSocket.receive(receivePacket);//wait until you receive a packet
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			if (!transferStatus && promptCheck) {//if already prompted 
				blockSelection(); 
				firstTransfer = true;
			}
			else if(!promptCheck){
				this.promptCheck =true;
				this.simPrompt();
				this.filter();
			}
			
			printIncomingInfo(receivePacket, "Simulator", verbose);//print the received packet details
			len = receivePacket.getLength();
			clientPort = receivePacket.getPort();
			sendPacket = new DatagramPacket(data, len, receivePacket.getAddress(), serverPort);
			len = sendPacket.getLength();
			// Send the datagram packet to the server via the send/receive
			// socket.
			if (readTransfer==false) {//if write
				checkPacket = sendPacket;
			}
			// Debug options for Client
			if ((actBlock == 0 && (firstTransfer) && !clientOrServer)) { //1st block for client request	
					try {
						if(loseZero){//if the user chose to remove/modify a 0
							sendPacket =removeZero(sendPacket);
						}
						sendPacket = changePacket(newFileName, newMode, opcodeC, sendPacket.getAddress(), sendPacket.getPort());						
						sendReceiveSocket.send(sendPacket);
						printOutgoingInfo(sendPacket, "Simulator", verbose);
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
				firstTransfer = false;
			}

			else if ((actBlock == parseBlock(sendPacket.getData()) && !clientOrServer)) { //nth block of client request
					try {
						if(loseZero){
							sendPacket =removeZero(sendPacket);
						}
						sendPacket = changePacket(newFileName, newMode, opcodeC, sendPacket.getAddress(), sendPacket.getPort());						
						sendReceiveSocket.send(sendPacket);
						printOutgoingInfo(sendPacket, "Simulator", verbose);
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}

				 
				if (!finalMessage && lengthCheck) {//check if this was the last block 
					finalMessage = true;
				}
			}
			else {//this wasn't the act block
				printOutgoingInfo(sendPacket, "Simulator", verbose);
				try {
					sendReceiveSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}

			// Construct a DatagramPacket for receiving packets up
			// to 100 bytes long (the length of the byte array)
			if (!finalMessage) {
				data = new byte[516];
				receivePacket = new DatagramPacket(data, data.length);
				System.out.println("Simulator: Waiting for packet.");
				try {
					sendReceiveSocket.receive(receivePacket);
					printIncomingInfo(receivePacket, "Simulator", verbose);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				
				serverPort = receivePacket.getPort();
				len = receivePacket.getLength();
				sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(),clientPort);
				len = sendPacket.getLength();

				// Send the datagram packet to the client via a new socket.

				
					/*
					 * Construct a new datagram socket and bind it to any port
					 * on the local host machine. This socket will be used
					 * to send UDP Datagram packets.
					 *  
					 */
				try {
					sendSocket = new DatagramSocket();
				} catch (SocketException se) {
					se.printStackTrace();
					System.exit(1);
				}
				if (readTransfer) {//if read
					checkPacket = sendPacket;
				}
				// Debug options for Server
				if ((actBlock == 0 && (firstTransfer) && clientOrServer)) { //1st block for serverOperations
						try {
							if(loseZero){
								sendPacket =removeZero(sendPacket);
							}
							sendPacket = changePacket(newFileName, newMode, opcodeC, sendPacket.getAddress(), sendPacket.getPort());	
							sendReceiveSocket.send(sendPacket);
							printOutgoingInfo(sendPacket, "Simulator", verbose);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
						
					firstTransfer = false;
				}

				else if ((actBlock == parseBlock(sendPacket.getData()) && clientOrServer)) { //nth block of server side operations
						printOutgoingInfo(sendPacket, "Simulator", verbose);
						try {
							if(loseZero){
								sendPacket =removeZero(sendPacket);
							}
							sendPacket = changePacket(newFileName, newMode, opcodeC, sendPacket.getAddress(), sendPacket.getPort());	
							sendSocket.send(sendPacket);
							printOutgoingInfo(sendPacket, "Simulator", verbose);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}

					
					if (!finalMessage && lengthCheck) {
						finalMessage = true;
					}
				} 
				else {
					try {
						sendSocket.send(sendPacket);
						printOutgoingInfo(sendPacket, "Simulator", verbose);
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
				}
			}

			System.out.println("Simulator: packet sent using port " + sendSocket.getLocalPort());
			System.out.println();
			transferStatus = true;
			if (checkPacket.getLength() == MAXLENGTH) {
				lengthCheck = true;
			}
		} while ((lengthCheck && !finalMessage) || (firstTransfer && !readTransfer));
		//System.out.println("error3");
		// We're finished with this socket, so close it.
		transferStatus = true;
		sendSocket.close();
		simPrompt();
}
	
	public void filter(){//based on the debugChoice, select the correct way to pass.
		
		if(debugChoice ==0){
			System.out.println();
			System.out.println("------Normal Operation------");
			System.out.println();
			passPacket();
		}
		else if(debugChoice ==1){
			System.out.println();
			System.out.println("------Losing Packet------");
			System.out.println();
			losePacket();
		}
		else if(debugChoice == 2){
			System.out.println();
			System.out.println("------Delaying Packet------");
			System.out.println();
			delayPacket();			
		}
		else if(debugChoice==3){
			System.out.println();
			System.out.println("------Duplicating Packet------");
			System.out.println();
			duplicatePacket();
		}
		else if(debugChoice==4){
			System.out.println();
			System.out.println("------Changing Packet Opcode------");
			System.out.println();
			opcodePacket();
		}
		else if(debugChoice==5){
			System.out.println();
			System.out.println("------Changing Port on Packet------");
			System.out.println();
			invalidPort();
		}
		
		
	}

	public static void main(String args[]) {//main
		TFTPSim t = new TFTPSim();
		t.promptCheck=true;
		t.simPrompt();
		t.filter();
		sc.close();
		
	}
}