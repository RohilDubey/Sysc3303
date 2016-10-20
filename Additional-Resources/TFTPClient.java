/*
 * 
 *  TFTPClient 
 * 
 * 
 * 
 * 
 */

import java.net.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.io.*;

public class TFTPClient extends UDPparent{
	
	//Variables used
	private static DatagramSocket transferSocket;
	private static String filename;
	private static int sendPort,listenPort;
	private String readFileName,writeFileName;
	private static boolean readRequest,writeRequest;
	private boolean completed = false;
	public int blockNum = 0;
	public BufferedInputStream in;
	public BufferedOutputStream out;
	
	public TFTPClient(){
		super();
		try{
	        // Construct a datagram socket and bind it to any available
	        // port on the local host machine. This socket will be used to
	        // send and receive UDP Datagram packets.
			transferSocket = new DatagramSocket();
		}catch(SocketException se){// Can't create the socket.
			se.printStackTrace();
			System.exit(1);
		}
	}

	/*
	 * Prompts the user
	 */
	private void promptRequest(){
		
		//Prepare for user input
		Scanner scan = new Scanner(System.in);
		
		while(true){
			//Read or Write
			System.out.println("Read or write request? Please enter 'r' for read, or 'w' for write: ");
			System.out.println("[Press 'e' to Shutdown the Client]");
			String selectedValue = scan.nextLine().toLowerCase();
		
			//User has selected to perform a read
			if(selectedValue.equals("r")||selectedValue.equals("R")){
				readRequest=true;
				System.out.println("You have chosen a read request.");
				break;
			}
		
			//User has selected to perform a write
			else if(selectedValue.equals("w")||selectedValue.equals("W")){
				writeRequest=true;
				System.out.println("You have chosen a write request.");
				break;
			}
		
			//Quit client
			else if(selectedValue.equals("e")||selectedValue.equals("E")){
				System.out.println("You have chosen to exit the client.");
				System.exit(1);
			}
		
			//Invalid choice
			else{
				System.out.println("Incorrect Choice, please renter.");
			}
		}
		
		while(true){
			//Test or Normal
			System.out.println("Test or Normal Mode? Please enter 'n' for normal, or 't' for test: ");
			System.out.println("[Press 'e' to Shutdown the Client]");
			String mode = scan.nextLine().toLowerCase();
		
			//User has selected normal mode which will send to the server directly
			if(mode.equals("N")||mode.equals("n")){
				listenPort = 69;
				System.out.println("Sending your request to the server directly.");	
				break;
			}
		
			//User has selected test mode which will send to intermediate, which will pass to the server
			else if(mode.equals("t")||mode.equals("t")){
				listenPort=23;
				System.out.println("Sending your request to error simulator, which is then passed to the server.");
				break;
			}
		
			//Quit client
			else if(mode.equals("e")||mode.equals("E")){
				System.out.println("You have chosen to exit the client.");
				System.exit(1);
			}
		
			//Invalid choice
			else{
				System.out.println("Incorrect Choice, please renter.");
			}
		}
		
		//Quiet or Verbose MUST BE IMPLEMENTED
		//System.out.println(");
		//TODO Quiet vs. Verbose
		//
		//
		
		//User inputs the filename
		System.out.println("Enter filename with the directory its associated in.\n");
		String filenameInput = scan.nextLine();
		readFileName=filenameInput;
		writeFileName=filenameInput;
		//System.out.println("You have chosen: "+selectedValue+","+ mode+" filename: "+filenameInput);
		scan.close();
	}//promptRequest() ends
	
	
	private void buildSendRequest() throws IOException{
		//String filename = null;
		String type = "octet";
		byte[] send = new byte[100];
		 
		//Client is sending the request type either read or write
		System.out.println("Client is creating a send request...");
		send[0]=0;//first byte is 0 regardless of read/write
		
		//Read opcode 01
		if(readRequest){
			readRequest = true;
			send[1]=1;
			System.out.println("Read Request is valid...");
			filename=readFileName;
			}
		
		//Write opcode 02
		else if(writeRequest){
			writeRequest = true;
			send[1]=2;
			System.out.println("Write Request is valid...");
			filename=writeFileName;
		}
		
		//Formatting the request
		byte[] temp =filename.getBytes();
		System.arraycopy(temp, 0, send, 2, temp.length);							//send contains 0_1|2_filename(without underscores)
		send[temp.length+2]=0;														//0_1|2_filename_0
		byte[] mode = type.getBytes();
		System.arraycopy(mode, 0, send, mode.length+temp.length - 2, mode.length);	//send contains 0_1|2_filename_0_octet(in bytes)
		int length = mode.length+temp.length+4;
		send[length - 1]=0;															//send contains 0_1|2_filename_0_octet_0
		
		//Construct a datagram packet that is to be sent to a specified port
        //depending on whether normal or test mode is chosen respectively
		try {
			sendPacket=new DatagramPacket(send,length,InetAddress.getLocalHost(),listenPort);
			
		}catch (UnknownHostException e) {// Can't create the packet.
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Client is sending packet....");
		
		//NEED TO MAKE THE BELOW ONLY IMPLEMENT IN VERBOSE
		System.out.println("To server: "+ sendPacket.getAddress());
        System.out.println("To host: " + sendPacket.getAddress());
        System.out.println("Destination host port: " + sendPacket.getPort());
        length = sendPacket.getLength();
        System.out.println("Length: " + length);
        //////////////////
        
		//Sending the request packet 
		try {
			transferSocket.send(sendPacket);
		}catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Client packet is sent");
		
	}//buildSendRequest() ends
	
	//Sends the acknowledge packet in case of read
	private void ackSend(int blockNumber){
		//turn the int into a big endian
		byte[] blockNum = intToByteArray(blockNumber);
		
		//opcode is 04 for ack packet
		//there are only two bits so use the two least significant bits of blockNum
		byte[] temp = new byte[4];
		temp[0] = 0;
		temp[1] = 4;
		temp[2] = blockNum[2];
		temp[3] = blockNum[3];
		DatagramPacket sendP = null;
		
		//Constructs an acknowledge packet that is to be sent to a specified port
        //depending on whether normal or test mode is chosen respectively
		try {
			sendP=new DatagramPacket(temp,temp.length,InetAddress.getLocalHost(),listenPort);
		} catch (UnknownHostException e) {// Can't create the packet.
			System.out.println("Creating Datagram failed");
			e.printStackTrace();
			System.exit(1);
		}
		
		//Sending the acknowledge packet
		System.out.println("Sending Acknowledgment....");
		try {
			transferSocket.send(sendP);
		}catch (IOException e) {
			System.out.println("Acknowledgment sending failed");
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Acknowledgment was sent sucessfully!");
	}//ackSend() ends
	
	/*
	 * Receives the data packet
	 */
	private byte[] receiveDataPacket(){
		//waits (currently indefinitely) to receive a packet on the specified socket
		byte[] buffer = new byte[516];
		System.out.println("Waiting to receive data packet...");
		DatagramPacket p = null;
		
		//Construct a datagram packet that is to be sent to a specified port
        //depending on whether normal or test mode is chosen respectively
		try {
			p = new DatagramPacket(buffer, buffer.length,InetAddress.getLocalHost(),sendPort);
		} catch (UnknownHostException e1) {
			System.out.println("Creating Datagram failed");
			e1.printStackTrace();
			System.exit(1);
		}
		
		//Receives the packet
		try {
			transferSocket.receive(p);
		} catch (IOException e) {
			System.out.println("Receiving from the port failed");
			e.printStackTrace();
			System.exit(1);
		}
		buffer=p.getData();
		System.out.println("DatagramPacket received successfully,contains: ");
		for(byte j: buffer){
			System.out.print(j);
		}
		return buffer;
	}//receiveDataPacket() ends
		
	//Reads the file and performs file transfer
	private boolean readFile(){
		byte [] file,fileInfo;
		blockNum++;			//Update blockNum
		
		//Preparing to receive data
		file = receiveDataPacket();		//receive's a data packet

		//Checks if we have received a error packet
		//If its an error we end
		if (parseErrorPacket(file)){
			System.exit(1);
		}
		
		//Check if we received valid data
		if(!validateDataPacket(file, blockNum)){
			System.out.println("Recieved an invalid data packet!!");
			System.exit(1);
		}
		
		System.out.println("Client has received data....");
		fileInfo=Arrays.copyOfRange(file, 4, file.length-1);	//create a new byte[] with only the data/information/bytes/relevant information to be written		
		System.out.println("Client is send acknowledge...");
		ackSend(file[3]);										//Sends an acknowledge packet 
		File tempFile = new File(filename);

		//Writing what we have got into the file give a fileName
		try {
			out = new BufferedOutputStream(new FileOutputStream(tempFile));
		}catch (FileNotFoundException e) {//File Not found
			System.out.println("Output Stream failure");
			error = createErrorByte((byte)1, "File " + filename + " does not exists!");
			errorPacket = generateDatagram(error, errorPacket.getAddress(), listenPort);
			sendDatagram(errorPacket, transferSocket);
			System.exit(1); //Stop transfer
		}
		
		//Send and Receive packets till the file transfer is complete 
		while(!completed){	
			//Does transfer
			try {
				out.write(fileInfo, 0, fileInfo.length);
			} catch (IOException e) {//Disk Full
				System.out.println("Output stream failed to write");
				error = createErrorByte((byte)3, "File " + filename + "cannot be written.");
				errorPacket = generateDatagram(error, errorPacket.getAddress(), listenPort);
				sendDatagram(errorPacket, transferSocket);
				System.exit(1);
			}
			System.out.println("Client has received data....");
		
			//Checks if file transfer is complete, terminate if last transfer is less then 512 bytes + 
			//the 4 bytes of opcode so 516 bytes
			if(fileInfo.length < 516){
				try {
					out.close();
				} catch (IOException e) {
					System.out.println("Output stream failed to close");
					e.printStackTrace();
					System.exit(1);
				}
				completed = true;		//Completed the file transfer
			
				//Create final acknowledgement packet and send to the server
				System.out.println("Client is sending the last ack packet...");
				ackSend(blockNum);		//Sends an acknowledge packet 
				
				//read transfer is complete
				return true;
			}
		
			//More transfers are still needed to be completed
			else{
				//Acknowledging the data, send the ack packet
				System.out.println("Client is sending an ack packet...\n");
				ackSend(blockNum);
			
				//Preparing to receive data
				fileInfo = receiveDataPacket();
				System.out.println("Client has received data....");
			}
		}
		//transfer did not complete
		return false;
	}//readFile() ends
	

	//Writes the file
	private boolean writeFile(){
		byte[] data = new byte[516];
   		byte[] ackR = {0, 4, 0, 0};
   		
   		try{
   			//Preparing to read from the file

   			try{
   				in = new BufferedInputStream(new FileInputStream(filename));
   			}catch (FileNotFoundException se) {//File Not found
   				System.out.println("Input Stream failure");
				error = createErrorByte((byte)1, "File " + filename + " does not exists!");
				errorPacket = generateDatagram(error, InetAddress.getLocalHost(), listenPort);
				sendDatagram(errorPacket, transferSocket);
				System.exit(1); //Stop transfer
   			}
		
   			while(!completed){
   				byte[] read = new byte[512];
   				int n;
   				
   				//Checks if we aren't done reading yet
   				if((n = in.read(read)) != -1){
				
   					//Waiting for the first ack block
   					//Preparing to receive the ack packet
   					//Constructs an acknowledge packet that is to be sent to a specified port
   					//depending on whether normal or test mode is chosen respectively
   					receivePacket = new DatagramPacket(ackR, ackR.length);
   					try{
   						System.out.println("Waiting for Acknowledge...");
   						transferSocket.receive(receivePacket);
   					}catch(IOException se){
   						se.printStackTrace();
   						System.exit(1);
   					}
   					
   					//Checks if we have received a error packet
   					//If its an error we end
   					if (parseErrorPacket(ackR)){
   						System.exit(1);
   					}
   					
   					//Checks if it is a valid ack packet
   					if(!validateACKPacket(ackR, blockNum)){
   						System.out.println("We have received an invalid ack packet.");
						System.exit(1);
   					}
   					
   					//Received a valid ack
   					System.out.println("We have received a valid ack packet...");
				
   					blockNum++;		//Update blockNum
   					//Preparing to send a data packet
   					//Construct a datagram packet that is to be sent to a specified port
   					//depending on whether normal or test mode is chosen respectively
   					System.arraycopy(read, 0, data, 4, read.length);
				
   					//Send the packet
   					sendPacket = generateDatagram(generateDataBlock(data, blockNum), InetAddress.getLocalHost(), listenPort);
   					sendDatagram(sendPacket, transferSocket);
   					System.out.println("Client sending the packet...");
   				}
			
   				//Prepare to stop reading
   				else{
   					//Waiting for the last ack packet
   					in.close();
   					completed = true;
				
   					//Preparing to receive the ack packet
   					try{
   						System.out.println("Waiting for the last ack....");
   						transferSocket.receive(receivePacket);
   					}catch(IOException se){
   						se.printStackTrace();
   						System.exit(1);
   					}	
				
   					//Checks if we have received a error packet
   					//If its an error we end
   					if (parseErrorPacket(ackR)){
   						System.exit(1);
   					}
   					
   					//Checks if it is a valid ack packet
   					if(!validateACKPacket(ackR, blockNum)){
   						System.out.println("We have received an invalid ack packet.");
   						System.exit(1);
   					}
   					System.out.println("We have recieved the last ack packet.");
   				}
   			}
   			//write transfer is complete
   			return true;
   		}catch(IOException se){
   			se.printStackTrace();
   		}
   		//Tranfer did not complete
   		return false;
	}//write file ends
	
	//Test
	public static void main(String[] args) throws IOException{
		TFTPClient c1 = new TFTPClient();
		c1.promptRequest();
		c1.buildSendRequest();
		boolean done = false;
		System.out.println("[Press 'e' to Shutdown the Client]");
		//Scanner selection = new Scanner(System.in);
		//String keyPress = selection.next().toLowerCase();
		while(true){
			//If its a read request read the file
			if(readRequest==true){
				done = c1.readFile();
				if(done){
					System.out.println("File read is complete!");
				}
				System.out.println("File read is not complete!");
				break;
			}
			
			//If its a write request write the file
			else if(writeRequest==true){
				done = c1.writeFile();
				if(done){
					System.out.println("File write is complete!");
				}
				System.out.println("File write is not complete!");
				break;
			}	
			/*
			//Exits the client
			else if(keyPress.equals('e') || keyPress.equals('E')){
				System.out.println("You have chosen to exit the client");
				System.exit(1);
			}*/	
		}
		transferSocket.close();		
	}//main ends
}//client class ends
