
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

	private DatagramSocket receiveSocket, sendSocket, sendReceiveSocket;
	private DatagramPacket checkPacket;
	String packet;
	int actBlock; // block to act upon (duplicate, lose, delay)
	int clientPort, serverPort = 69, j = 0, len, debugChoice;
	boolean transferStatus, readTransfer, firstTransfer;
	Request req; // READ, WRITE or ERROR

	// responses for valid requests
	public static final int MAXLENGTH = 516; 
	public static enum Request {
		READ, WRITE, ERROR
	};
	public static final byte[] readResp = { 0, 3, 0, 1 };
	public static final byte[] writeResp = { 0, 4, 0, 0 };
	public boolean losePacket;

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
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	public void simPrompt() {// menu for creating errors
		Scanner sc = new Scanner(System.in);
		String choice;
		Boolean loop = true;// if the choice is valid, loop becomes false and we
							// won't need to re-choose
		while (loop) {
			System.out.println("What would you like to simulate:");
			System.out.println("Enter 0: Normal Operation");
			System.out.println("Enter 1: Lose a Packet");
			System.out.println("Enter 2: Delay a Packet");
			System.out.println("Enter 3: Duplicate a Packet");
			String num;
			choice = sc.next();
			if (choice.contains("0")) {// normal operation
				System.out.println("Normal Operation Selected");
				loop = false;
				sc.close();
				actBlock = -1; // Set so none of the if statements go off
				return; // Just exit out of method in case of normal operation
			} else if (choice.contains("1")) {// lose packet
				System.out.println("Lose packet selected");
				debugChoice = 1;
				packet = selectPacket();
				if(packet.contains("2")) { // specific block (data/ack)
					System.out.println("Which block would you like to lose?");
					num = sc.next();
					actBlock = Integer.parseInt(num);
				} else { // request packet
					actBlock = 0;
				}				
				loop = false;
				
			} else if (choice.contains("2")) {// delay packet
				System.out.println("Delay Packet selected");
				packet = selectPacket();
				loop = false;
				
			} else if (choice.contains("3")) {// duplicate packet
				System.out.println("Duplicate Packet selected");
				debugChoice = 3;
				packet = selectPacket();
				if (packet.contains("2")) { // specific block (data/ack)
					System.out.println("Which block would you like to duplicate?");
					num = sc.next();
					actBlock = Integer.parseInt(num);
				} else { // request packet
					actBlock = 0;
				}
				loop = false;
			} else {// invalid input
				System.out.println("Value entered is not valid");
			}
		}
		sc.close();

	}

	public boolean checkRequest() {
		byte[] data = receivePacket.getData();
		if (data[0] != 0)
			req = Request.ERROR; // bad
		else if (data[1] == 1) {
			req = Request.READ; // could be read
			readTransfer = true;
		} else if (data[1] == 2) {
			req = Request.WRITE; // could be write
			readTransfer = false;
		}
		return readTransfer;
	}

	public String selectPacket() {
		Scanner sc = new Scanner(System.in);
		System.out.println("Which packet would you like to act on? [type out the full message]");
		if (readTransfer) {
			System.out.println("1: RRQ");
			System.out.println("2: ACK");
		} else {
			System.out.println("3: WRQ");
			System.out.println("4: DATA");
		}
		String choice = sc.next();

		if (choice.contains("1")) {
			System.out.println("RRQ Packet chosen");
		} else if (choice.contains("3")) {
			System.out.println("WRQ Packet chosen");
		} else if (choice.contains("2")) {
			System.out.println("ACK Packet chosen");
		} else if (choice.contains("4")) {
			System.out.println("DATA Packet chosen");
		} else {
			System.out.println("Choice is invalid, please choose again");
			choice = selectPacket();
		}
		sc.close();
		return choice;
	}

	public void passOnTFTP() {
		
		byte[] data;
		transferStatus = false;
		losePacket=false;

		for (;;) { // loop forever
			do {
				// Construct a DatagramPacket for receiving packets up
				// to 100 bytes long (the length of the byte array).

				data = new byte[100];
				receivePacket = new DatagramPacket(data, data.length);

				System.out.println("Simulator: Waiting for packet.");
				// Block until a datagram packet is received from receiveSocket.
				try {
					receiveSocket.receive(receivePacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				if (!transferStatus) {
					simPrompt(); // Prior to printing information, ask what
									// error debug
					// the user would like to do
					firstTransfer = true;
				}

				printIncomingInfo(receivePacket, "Simulator", verbose);
				len = receivePacket.getLength();
				clientPort = receivePacket.getPort();

				sendPacket = new DatagramPacket(data, len, receivePacket.getAddress(), serverPort);

				printOutgoingInfo(sendPacket, "Simulator", verbose);
				len = sendPacket.getLength();

				// Send the datagram packet to the server via the send/receive
				// socket.
				if(actBlock == 0){ // For request debug only
					if (debugChoice == 1){
						losePacket=true;	
					}
				}
				if((actBlock == parseBlock(sendPacket.getData()))){ //if this is the block num you want to lose
					if (debugChoice == 1){
						losePacket=true;
					}
				}
				
				
				if(!readTransfer){
					checkPacket = sendPacket;
				}
				try {
					sendReceiveSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}//after the send and receive check and see if we need to duplicate a packet
				
				
				
				if((actBlock == 0 && (firstTransfer))){ // For request debug only
					if (debugChoice == 3){
						// Send again
					
						try {
							sendReceiveSocket.send(sendPacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
					}
				}
				}
				
				if((actBlock == parseBlock(sendPacket.getData()))){ //Check for block
					if (debugChoice == 3){
						// Send again
					
						try {
							sendReceiveSocket.send(sendPacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
					}
				}
				}
				// Construct a DatagramPacket for receiving packets up
				// to 100 bytes long (the length of the byte array).

				data = new byte[100];
				receivePacket = new DatagramPacket(data, data.length);
				System.out.println("Simulator: Waiting for packet.");
				
			if(losePacket==false){
				try {
					// Block until a datagram is received via sendReceiveSocket.
					sendReceiveSocket.receive(receivePacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				
				
				
				serverPort = receivePacket.getPort();
				printIncomingInfo(receivePacket, "Simulator", verbose);
				len = receivePacket.getLength();

				sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(),
						clientPort);

				printOutgoingInfo(sendPacket, "Simulator", verbose);
				len = sendPacket.getLength();
				
				// Send the datagram packet to the client via a new socket.
				
				try {
					// Construct a new datagram socket and bind it to any port
					// on the local host machine. This socket will be used to
					// send UDP Datagram packets.
					sendSocket = new DatagramSocket();
				} catch (SocketException se) {
					se.printStackTrace();
					System.exit(1);
				}
				if(readTransfer){
					checkPacket = receivePacket;
				}
				try {
					sendSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}

				System.out.println("Simulator: packet sent using port " + sendSocket.getLocalPort());
				System.out.println();
				
				if(!readTransfer && firstTransfer){ // In case of write and its the first transfer
					// therefore ack will not have max length and we have to circumvent that.
					checkPacket.setLength(MAXLENGTH);
					firstTransfer = false;
				}
			}
			}//end of do
			
			while (checkPacket.getLength() == MAXLENGTH);
			// We're finished with this socket, so close it.
			if(losePacket==false){
			sendSocket.close();
			transferStatus = true;
			}
		} // end of for loop

	}//end PassOnTFTP

	public static void main(String args[]) {
		TFTPSim s = new TFTPSim();

		s.passOnTFTP();
	}
}
