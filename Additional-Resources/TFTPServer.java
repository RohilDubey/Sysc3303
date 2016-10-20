/*
 * 
 *  TFTPServer 
 * 
 * 
 * 
 * 
 */

import java.net.*;
import java.awt.List;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.io.IOException;
import java.awt.*;
import javax.swing.*;
import java.lang.*;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;


public class TFTPServer extends UDPparent{
	
	private boolean writeReq,readReq;
	private DatagramSocket receivingSocket;
	private DatagramPacket receivingPacket;
	private int blockNum = 0;
	
	
	public TFTPServer(){
		super();
		try {
			receivingSocket=new DatagramSocket(69);
		} catch (SocketException e) {
			System.out.println("Failure in creating receiving socket");
			e.printStackTrace();
			System.exit(1);
		}
		new Thread(){//each new server will create a new thread that listens for a exit key.
			
			public void run(){
				Scanner exit = new Scanner(System.in);
				String keyPress=null;
				System.out.println("TO EXIT SERVER PRESS E");
				
				while(true){
					keyPress=exit.next();
					if(keyPress.equals("e")){
						System.out.println("SERVER EXITING");
						System.exit(0);
						exit.close();
						
					}
					else if(keyPress.equals("E")){
						System.out.println("SERVER EXITING");
						System.exit(0);
						exit.close();
					}
				}				
			}
		}.start();
	}

	public void receiveTFTPRequest(){
		int j;
		int i = 0;
		String fileName,mode;
		byte[] temp;
		readReq=false;
		writeReq=false;
		
		while(true){
			temp = new byte[100];
			receivingPacket= new DatagramPacket(temp,temp.length);
			System.out.println("Server is waiting for request packet ");
			
			try {
				receivingSocket.receive(receivingPacket);
			} catch (IOException e) {
				
				
				e.printStackTrace();
				System.exit(1);
			}
			
			System.out.println("Request Packet received...");
			System.out.println("From host: "+ receivingPacket.getAddress());
			System.out.println("Port: " + receivingPacket.getPort());
			System.out.println("Length: "+receivingPacket.getLength());
			System.out.println("Contents: ");
			
			temp = receivingPacket.getData();
			for(Byte z :temp){
				System.out.print(z);
			}
			System.out.println(" ");
			String receivedPortBytes = new String(temp,0,receivingPacket.getLength()-7);
			System.out.println(receivedPortBytes);
			
			if(temp[0]!=0){
				System.out.println("Packet is INVALID");
				System.exit(1);
			}
			else if(temp[1]==2){//write request
				writeReq=true;
				
			}
			else if(temp[1]==1){//read request
				readReq =true;
			}
			else{
				System.out.println("Packet is INVALID");
				System.exit(1);
			}
			
			int packetLen = receivingPacket.getLength();
			if(readReq||writeReq == true){//need to parse through bytes for 0, then everything before that is the filename
				for(i=2;i<packetLen;i++){
					if(temp[i]==0){
						break;//we've found the 0 byte and everything we've read so far has been the filename
					}
					if(i==packetLen){//no luck finding 0byte and we've reached the end of the data
						System.out.println("Couldnt find filename");
						System.exit(1);
					}
					
				}
				
				
			}
			fileName = new String(temp,2,i-2);
			if(readReq||writeReq == true){//check for type ex. OCTET
				for(j=i+1;j<packetLen;j++){
					if(temp[j]==0){
						break;
					}
					if(j==packetLen){
						System.out.println("Could not find mode");
						System.exit(1);
					}
				}
				
				
				mode = new String(temp,i,j-i-1);
				new clientConnectionThread(receivingPacket,readReq,writeReq,receivingPacket.getPort(), fileName).start(); //ROBERT, I changed this from 69, the clientconnectionThread shouldn't be sending data to port 69, it should be sent to the port that the client sent the request FROM (not TO)
			}
			
		}
	}//end of receiveTFTPRequest
	
	class clientConnectionThread extends Thread{
		
		private DatagramSocket sendReceive;
		private String file_Name;
		private boolean read,write;
		private int sendBackPort;
		private String filename;
		private DatagramPacket dp;
		private InetAddress address;
		
		public clientConnectionThread(DatagramPacket dp,boolean read,boolean write,int sendBackPort, String filename){
			this.read = read;
			this.write =write;
			this.dp = dp;
			this.address = dp.getAddress();
			this.sendBackPort = dp.getPort(); //I don't think sendBackPort is the right name for this. This port is being used as the port that we're sending datagram packets to
			byte[] temp = dp.getData();
			this.filename = filename;
			
			String tempToString = new String(temp);
		
			try { 
				this.sendReceive=new DatagramSocket();
				
			} catch (SocketException e) {
				System.out.println("Datagram Socket creation failure");
				e.printStackTrace();
				System.exit(1);
				
			}
			
			//This should skip the 1st 2 bytes 01 start on the 2nd byte then move to a 0 and 
			//take everything between the 1st 2 bytes to elem of the 0-1.												
			int i = 2;
			for (; temp[i] != 0; ++i);
			file_Name = tempToString.substring(2, i - 1);	
		}
		
		public void run(){//spawns new thread
			
			//If its a read request
			if(read == true){
				InputStream in =null;
				File temp_file=new File(file_Name);
				if(temp_file.exists()){
					try {
						in =new FileInputStream(temp_file);
					} catch (FileNotFoundException e) {
						System.out.println("Failure in read file name");
						e.printStackTrace();
					}
					
				}
				
				else{//file exists already, so can't do read
					error = createErrorByte((byte)6, "File " + filename + " already exists on the server!");
					errorPacket = generateDatagram(error, address, sendBackPort);
					sendDatagram(errorPacket, sendReceive);
					System.exit(1); //Stop transfer
				}
				
				ArrayList<Byte> temp;
				byte[] bits = new byte [4];
				byte[] data = new byte[512];
				
				bits[0]=0;
				bits[1]=3;
				bits[2]=0;
				bits[3]=1;
				
				try {
					for(int dataBlockNumber = 1; in.read(data)!=-1; ++dataBlockNumber){//readRequest
						int i=0;
						bits[2] = (byte)((dataBlockNumber & (0xFF << 8)) >> 8); //the datablocknumber has to change every time there's a new block
						bits[3] = (byte)(dataBlockNumber & (0xFF)); 
						temp = new ArrayList<Byte>();

						for(byte j: bits){
							temp.add(j);
						}

						for(byte j:data){
							temp.add(j);
						}
						
						Byte[] temp2 = temp.toArray(new Byte[516]);
						
						//test purposes only
						System.out.println("");
						System.out.println("Printing contents of read request: ");
						for(byte j:temp2){
							System.out.println(j);
						}
						
						System.out.println("");//new line
						sendDataPack(temp2);
						receiveAck(); //assume this works for now
					}//end for loop
					//TODO at some point after this we have to check if the last byte[] sent was less than 512, if it wasn't then we have to send a datapacket with data of length 0	
					
				} catch (IOException e) {//Access Violation
					System.out.println("Data read failure");
					error = createErrorByte((byte)2, "Access Violation for:  " + filename);
					errorPacket = generateDatagram(error, errorPacket.getAddress(), sendBackPort);
					sendDatagram(errorPacket, sendReceive);
					System.exit(1); //Stop transfer
				}
				
				try {
					in.close();
				} catch (IOException e) {//Disk Full
					error = createErrorByte((byte)3, "Disk Full");
					errorPacket = generateDatagram(error, errorPacket.getAddress(), sendBackPort);
					sendDatagram(errorPacket, sendReceive);
					System.exit(1); //Stop transfer
				}
				
				System.out.println("FILE TRANSFERED SUCCESSFULLY");
				this.sendReceive.close();
			}
			
			
			//Write request
			else if(write==true){//write request


				OutputStream out = null;
				try{
					out = new FileOutputStream(filename);
				} catch (FileNotFoundException e){//File not found
					error = createErrorByte((byte)1, "File " + filename + " does not exist!");
					errorPacket = generateDatagram(error, errorPacket.getAddress(), sendBackPort);
					sendDatagram(errorPacket, sendReceive);
					System.exit(1); //Stop transfer
				}

				ackSend((byte)0); //send the first ack
				byte[] receivedData = receiveDataPack(); //get data 

				for (int blockNumber = 1; receivedData.length == 516; ++blockNumber){

					try{//write the data to file
						out.write(Arrays.copyOfRange(receivedData, 4, receivedData.length));
					} catch (IOException e){//Disk Full
						error = createErrorByte((byte)3, "Disk Full");
						errorPacket = generateDatagram(error, errorPacket.getAddress(), sendBackPort);
						sendDatagram(errorPacket, sendReceive);
						System.exit(1); //Stop transfer
					}

					ackSend((byte)blockNumber);	//Send the ack

					receivedData = receiveDataPack(); //get another chunk of data
					//Need to add 
				} 
			}
			
		}//end of run()
		//-------------------------------------------------------------------------------------
						
		private void ackSend(byte blockNumber){
		byte[] temp ={0,4,0,blockNumber};//feel free to change variable names to make it easier
		DatagramPacket sendP=null;
		
		try {
			sendP=new DatagramPacket(temp,temp.length,InetAddress.getLocalHost(),sendBackPort);
		} catch (UnknownHostException e) {
			System.out.println("Creating Datagram failed");
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Sending Acknowledgment");
		
		try {
			sendReceive.send(sendP);
		} catch (IOException e) {
			System.out.println("Acknowledgment sending failed");
			e.printStackTrace();
			System.exit(1);
		}
		
		System.out.println("Acknowledgment was sent sucessfully!");
		
		}//end of ackSend()

		private byte [] receiveDataPack(){//returns byte array containing data from packet
			byte[] temp = new byte[516];
			DatagramPacket receivingPacket = null;
			
			receivingPacket= new DatagramPacket(temp,temp.length);
			
			//Check if we received valid data
			if(!validateDataPacket(temp, blockNum)){
				System.out.println("Recieved an invalid data packet!!");
				System.exit(1);
			}
			System.out.println("Receiving data packet...");
			
			try {
				sendReceive.receive(receivingPacket);
			} catch (IOException e) {
				System.out.println("Receiving Packet failure");
				//Checks if we have received a error packet
				//If its an error we end
				if (parseErrorPacket(temp)){
					System.exit(1);
				}
			}
			
			//Process the receieved packet
			temp = receivingPacket.getData();
			System.out.println("Received data packet containing: ");
			for(byte j:temp){
				System.out.print(j);
			}
			System.out.println(" ");
			return temp;
			
		}//end receiveDataPack()
		
		private byte[] receiveAck(){ // We should probably send the dataBlockNum to compare against
			byte[] temp = new byte[10]; //over sized for safety
			DatagramPacket receivingPacket=null;
			
			
			receivingPacket = new DatagramPacket(temp,temp.length);
			
			//Checks if it is a valid ack packet
			if(!validateACKPacket(temp, blockNum)){
				System.out.println("We have received an invalid ack packet.");
			System.exit(1);
			}		
			System.out.println("Receiving ack packet...");
			
			try {
				sendReceive.receive(receivingPacket);
			} catch (IOException e) {
				//Checks if we have received a error packet
				//If its an error we end
				if (parseErrorPacket(temp)){
					System.exit(1);
				}
			}
			
			//Proces received packet
			temp=receivingPacket.getData();
			System.out.println("Received Ackowledgment containing: ");
			for(byte j:temp){
				System.out.print(j);
			}
			System.out.println(" ");
			
			return temp;
			
		}//end of receiveAck()
	
		private void sendDataPack(Byte[] data){
			
			byte[] temp = new byte[516]; //not sure why this is done but doesn't really matter
			int i=0;
			
			for(i=0;i<data.length;i++){
				if(data[i] !=null);
				temp[i]=data[i].byteValue();
			}
			DatagramPacket sendingPacket = null;
			
			//Construct a datagram packet to be sent to the client			
			try {
				sendingPacket = new DatagramPacket(temp, temp.length, InetAddress.getLocalHost(), sendBackPort);
			} catch (UnknownHostException e) {
				System.out.println("Failure in creating datagramPacket");				
				e.printStackTrace();
			}
			
			//Process packet to be sent
			try {
				System.out.println("Sending packet from:" + InetAddress.getLocalHost() + " to port"+ this.sendBackPort + "Containing: ");
			} catch (UnknownHostException e) {
				System.out.println("Failure in locating locaHost address");
				e.printStackTrace();
				System.exit(1);
			}
			//Prints out contents
			for(byte j:temp){
				System.out.print(j);
			}
			
			//Sends the packet to the client
			try {
				sendReceive.send(sendingPacket);
			} catch (IOException e) {
				System.out.println("Could not send packet");
				e.printStackTrace();
				System.exit(1);
			}
			System.out.println("Packet was sucesffully sent to port: "+ this.sendBackPort);
			
				
			}//end sendDataPacket()
	
	}//end thread class (clientConnectionThread)

	public static void main(String args[]){
		TFTPServer s = new TFTPServer();
		s.receiveTFTPRequest();
	}
	
}//end of TFTPServer
