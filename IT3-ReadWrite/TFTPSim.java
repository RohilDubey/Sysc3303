
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
	boolean finalMessage;
	String packet;
	int clientPort, serverPort = 69, j = 0, len, debugChoice, actBlock, delay;
	boolean transferStatus, readTransfer, firstTransfer, lengthCheck, clientOrServer, selectionFlag;
	Request req; // READ, WRITE or ERROR

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
				actBlock = -1; // Set so none of the if statements go off
				return; // Just exit out of method in case of normal operation
			} else if (choice.contains("1")) {// lose packet
				System.out.println("Lose packet selected");
				debugChoice = 1;
				packet = selectPacket();
				if (packet.contains("2")) { // specific block (data/ack)
					System.out.println("Which block would you like to lose?");
					num = sc.next();
					actBlock = Integer.parseInt(num);
				} else { // request packet
					actBlock = 0;
				}
				loop = false;
			} else if (choice.contains("2")) {// delay packet
				System.out.println("Delay packet selected");
				debugChoice = 1;
				packet = selectPacket();
				if (packet.contains("2")) { // specific block (data/ack)
					System.out.println("Which block would you like to delay?");
					num = sc.next();
					System.out.println("How long do you want to delay the block?");
					delay = sc.nextInt();
					actBlock = Integer.parseInt(num);
				} else { // request packet
					actBlock = 0;
				}
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
		readTransfer = checkRequest();
		String choice = sc.next();
		do {
			System.out.println("Would you like to act on the [S]erver or [C]lient?");
			if (choice.contains("S") || choice.contains("s")) {
				clientOrServer = true;
				selectionFlag = true;
			} else if (choice.contains("C") || choice.contains("c")) {
				clientOrServer = false;
				selectionFlag = true;
			}
		} while (!selectionFlag);
		selectionFlag = false;
		System.out.println("Which packet type would you like to act on?");
		do {
			if (readTransfer) {
				System.out.println("[1]: RRQ");
				System.out.println("[2]: ACK");
			} else {
				System.out.println("[1]: WRQ");
				System.out.println("[2]: DATA");
			}
			if ((!choice.contains("1")) && (!choice.contains("2"))) {
				System.out.println("Choice is invalid, please choose again");
				choice = selectPacket();
				selectionFlag = false;
			} else {
				selectionFlag = true;
			}
		} while (!selectionFlag);
		selectionFlag = false;
		return choice;
	}

	public void passOnTFTP() {

		byte[] data;
		for (;;) { // loop forever
			transferStatus = false;
			finalMessage = false;
			firstTransfer = false;
			lengthCheck = false;
			do {
				data = new byte[516];
				receivePacket = new DatagramPacket(data, data.length);
				// Block until a datagram packet is received from receiveSocket.
				System.out.println("Simulator: Waiting for packet.");
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

				len = sendPacket.getLength();

				// Send the datagram packet to the server via the send/receive
				// socket.
				if (!readTransfer) {
					checkPacket = sendPacket;
				}
				// Debug options for Client
				if ((actBlock == 0 && (firstTransfer) && !clientOrServer)) { // For
																				// request
																				// debug
					// only
					if (debugChoice == 1) {
						System.out.println(); // Empty because we will skip this
												// packet
					} else if (debugChoice == 2) {
						try {
							Thread.sleep(delay);
						} catch (InterruptedException e) {
						}
						printOutgoingInfo(sendPacket, "Simulator", verbose);
						try {
							sendReceiveSocket.send(sendPacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}

					} else if (debugChoice == 3) {
						printOutgoingInfo(sendPacket, "Simulator", verbose);
						printOutgoingInfo(sendPacket, "Simulator", verbose);
						// send twice
						try {
							sendReceiveSocket.send(sendPacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
						try {
							sendReceiveSocket.send(sendPacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
					}
					firstTransfer = false;
				}

				else if ((actBlock == parseBlock(sendPacket.getData()) && !clientOrServer)) { // check
					// for
					// block
					if (debugChoice == 1) {
						System.out.println(); // Empty because we will skip this
												// packet
					} else if (debugChoice == 2) {
						try {
							Thread.sleep(delay);
						} catch (InterruptedException e) {
						}
						printOutgoingInfo(sendPacket, "Simulator", verbose);
						try {
							sendReceiveSocket.send(sendPacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}

					} else if (debugChoice == 3) {
						printOutgoingInfo(sendPacket, "Simulator", verbose);
						printOutgoingInfo(sendPacket, "Simulator", verbose);
						// Send twice
						try {
							sendReceiveSocket.send(sendPacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
						try {
							sendReceiveSocket.send(sendPacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
					}
					if (!finalMessage && lengthCheck) {
						finalMessage = true;
					}
				} else {
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
						// Block until a datagram is received via
						// sendReceiveSocket.
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
						// Construct a new datagram socket and bind it to any
						// port
						// on the local host machine. This socket will be used
						// to
						// send UDP Datagram packets.
						sendSocket = new DatagramSocket();
					} catch (SocketException se) {
						se.printStackTrace();
						System.exit(1);
					}
					if (readTransfer) {
						checkPacket = sendPacket;
					}
					// Debug options for Server
					if ((actBlock == 0 && (firstTransfer) && clientOrServer)) { // For
																					// request
																					// debug
						// only
						if (debugChoice == 1) {
							System.out.println(); // Empty because we will skip
													// this
							// packet
						} else if (debugChoice == 2) {
							try {
								Thread.sleep(delay);
							} catch (InterruptedException e) {
							}
							printOutgoingInfo(sendPacket, "Simulator", verbose);
							try {
								sendReceiveSocket.send(sendPacket);
							} catch (IOException e) {
								e.printStackTrace();
								System.exit(1);
							}

						} else if (debugChoice == 3) {
							printOutgoingInfo(sendPacket, "Simulator", verbose);
							printOutgoingInfo(sendPacket, "Simulator", verbose);
							// send twice
							try {
								sendReceiveSocket.send(sendPacket);
							} catch (IOException e) {
								e.printStackTrace();
								System.exit(1);
							}
							try {
								sendReceiveSocket.send(sendPacket);
							} catch (IOException e) {
								e.printStackTrace();
								System.exit(1);
							}
						}
						firstTransfer = false;
					}

					else if ((actBlock == parseBlock(sendPacket.getData()) && !clientOrServer)) { // check
						// for
						// block
						if (debugChoice == 1) {
							System.out.println(); // Empty because we will skip
													// this
							// packet
						} else if (debugChoice == 2) {
							try {
								Thread.sleep(delay);
							} catch (InterruptedException e) {
							}
							printOutgoingInfo(sendPacket, "Simulator", verbose);
							try {
								sendSocket.send(sendPacket);
							} catch (IOException e) {
								e.printStackTrace();
								System.exit(1);
							}

						} else if (debugChoice == 3) {
							printOutgoingInfo(sendPacket, "Simulator", verbose);
							printOutgoingInfo(sendPacket, "Simulator", verbose);
							// Send twice
							try {
								sendSocket.send(sendPacket);
							} catch (IOException e) {
								e.printStackTrace();
								System.exit(1);
							}
							try {
								sendSocket.send(sendPacket);
							} catch (IOException e) {
								e.printStackTrace();
								System.exit(1);
							}
						}
						if (!finalMessage && lengthCheck) {
							finalMessage = true;
						}
					} else {
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
			System.out.println("error3");
			// We're finished with this socket, so close it.
			transferStatus = false;
			sendSocket.close();
		} // end of loop

	}

	public static void main(String args[]) {
		TFTPSim s = new TFTPSim();
		s.passOnTFTP();
		sc.close();
	}
}
