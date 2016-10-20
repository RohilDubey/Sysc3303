/*
 * 
 *  UDPparent is the parent class to TFTPClient and TFTPServer
 * 
 * 
 * 
 * 
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
 


public class UDPparent{

	protected byte[] error;
	protected DatagramPacket sendPacket, receivePacket, errorPacket;
	//private boolean verbose;
	//private boolean testMode;
	
	
	
	/*
	 * Converts an array of ints to bytes
	 */
	protected static byte[] intToByteArray(int a)
	{
		byte[] ret = new byte[4];
		ret[0] = (byte) (a & 0xFF);   
		ret[1] = (byte) ((a >> 8) & 0xFF);   
		ret[2] = (byte) ((a >> 16) & 0xFF);   
		ret[3] = (byte) ((a >> 24) & 0xFF);
		return ret;
	}//intToByteArray() ends

	
	protected boolean validateRequestPacket(byte[] byteArray){
		if(byteArray[0] != 0) return false;
		if(byteArray[1] != 1 && byteArray[1] != 2) return false;
		int i = 0;
		for (; (i < byteArray.length) && (byteArray[i] != 0); i++) //exit when you hit the end or you find a zero
		if (i == byteArray.length - 1) return false; //otherwise i points to the zero in the bytearray, if the next char is 0, it's invalid, otherwise we assume it's valid
		if(byteArray[i+1] == 0) return false;
		return true; //packet passed all tests, it is a valid request packet
	}//validateRequestPacket() ends

	
	protected boolean validateACKPacket(byte[] byteArray, int blockNumber){
		byte[] blockNum = intToByteArray(blockNumber); //turn the int into a big endian byte array
		if (byteArray[0] == 0 && byteArray[1] == 1 && byteArray[2] == blockNum[2] && byteArray[3] == blockNum[3]) return true;
		return false;
	}//validateAckPacket() ends

	
	protected boolean validateDataPacket(byte[] byteArray, int blockNumber){
		byte[] blockNum = intToByteArray(blockNumber); //turn the int into a big endian byte array
		if (byteArray[0] == 0 && byteArray[1] == 3 && byteArray[2] == blockNum[2] && byteArray[3] == blockNum[3]){
		return true;//data
		}
		return false;
	}//validateDataPacket() ends
	
	
	protected byte[] generateDataBlock(byte[] data, int blockNumber){
		byte[] blockNum = intToByteArray(blockNumber); //turn the int into a big endian byte array
		byte[] dataBlock = new byte[data.length + blockNum.length + 2];
		dataBlock[0] = 0;
		dataBlock[1] = 3; //03 means data
		System.arraycopy(blockNum, 2, dataBlock, 2, blockNum.length - 2);
		System.arraycopy(data, 0, dataBlock, 4, dataBlock.length); //copy the arrays into one big array
		return dataBlock;
	}//generateDataBlock() ends
	
	
	protected boolean sendDatagram(DatagramPacket packetToSend, DatagramSocket socketToUse){ 
		//This just tries to send the packet, unless there's an IOexception it will always return true
		try {
			socketToUse.send(packetToSend);
		} catch (IOException e) {
			System.out.println("Sending the packet failed");
			e.printStackTrace();
			return false;
		}
		return true;
	}//sendDatagram()ends
	
	
	protected DatagramPacket generateDatagram(byte[] byteArray, InetAddress IPaddress, int portNumber){ 
		//this method really doesn't do much, however it makes code readable and may do more later
		DatagramPacket packetToSend;
		packetToSend = new DatagramPacket(byteArray, byteArray.length, IPaddress, portNumber);
		return packetToSend;
	}//generateDatagram() ends
	
	
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
	
	
	protected boolean parseErrorPacket(byte[] e){
		//Get the error message received
		byte[] rError = new byte[e.length - 5];
		System.arraycopy(e, 4, rError, 0, e.length - 5);
		
		//Get the error code received
		byte errorCode = e[3];
		
		//Display error type to the user
		if(errorCode == 1){
			System.out.println("Error Packet: 01: File Not Found!");
			//Display the error message
			String message = new String(rError);
			System.out.println("Error Message: " + message);
			return true;
		}
		
		else if(errorCode == 2){
			System.out.println("Error Packet: 02: Access Violation!");
			//Display the error message
			String message = new String(rError);
			System.out.println("Error Message: " + message);
			return true;
		}
		
		else if(errorCode == 3){
			System.out.println("Error Packet: 03: Disk Full!");
			//Display the error message
			String message = new String(rError);
			System.out.println("Error Message: " + message);
			return true;
		}
		
		else if(errorCode == 6){
			System.out.println("Error Packet: 06: File Already Exists!");
			//Display the error message
			String message = new String(rError);
			System.out.println("Error Message: " + message);
			return true;
		}
		return false;
	}//parseErrorPacket() ends
	
	
}//end of class UDPparent
