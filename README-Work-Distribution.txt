
README.txt

SYSC3303 - Iteration 3

TEAM #5

Team Members:

Adam Berg - 100978623
Rohil - 100969189
Saketh -100830684
Robert Mcmullen - 100977031
Survesh Srinivasan - 100985810


This is a new team, after our old ones were disbanded. So only included is the current work distrubtion for latest iteration.


Work Distribution for third Iteration-

Adam Berg - Timing Diagrams, UML's, UCM's
Saketh - coded for delayed of packet,
Rohil - coded for duplicate of packet, test menu, testing and debugging
Robert Mcmullen - coded for loss of packet, test menu, testing and debugging
Survesh Srinivasan - Merging of one of the teams iterations 1 and others iteration 2's (since we are a team with new members, used a combination of our old teams code),  testing and debugging


//INFO REGARDING PROJECT

This is a TFTP file transfer program, that sends files between a server and client.

The program is built using UDP packets and a simple protocol to differentiate between reads and writes, acknowledgement packets and request packets.

A read request is sent to port 69, and the server replies with an acknowledgement on a new thread with a port chosen by the JVM.
That port is then used by the server to handle the TFTP transfer.
Each packet is 512 bytes in size, except the last packet, which must be less than 512 bytes, this terminates the transfer.
If the client requests a read, the server sends packets and the client acknowledges them, if the client requests a write, the server sends packets and the client acknowledges.

Our code requires the user to have a "Server" folder on the desktop
Within this folder, all of the "test.txt" should be included
When choosing a read request, the program will search inside the Server folder for the test cases



//ITERATION 2, there had to be added support for ERROR packets (1,2,3 and 6). I/O erros had to be assumed to occer, so error packets dealing with this had to be prepared, transmitted, received, and/or handled.
Error codes:
   1         File not found.
   2         Access violation.
   3         Disk full or allocation exceeded.
   6         File already exists.

   
//ITERATION 3, Adding Network Error Handling (Timeout/Retransmission) we had to add a test menu to handle errors, the 4 tests include:
	0		Normal operation
	1		Lose a packet
	2		Delay a packet
	3		Duplicate a packet
Each test would test the following packets, DATA,RRQ,WRQ,ACK, and which block number for each of those packet types. The delay will also prompt the user for a time in ms. 
	
//TEST CASES
test0.txt is an empty text file
test1.txt contains a medium amount of bytes
test2.txt contains a low amount of bytes
test3.txt contains a large amount of bytes
test4.txt contains a medium amount of bytes



	
//HOW TO LAUNCH
Setting up and testing instructions:

1. Download .zip folder
2. Extract contents
3. Create Server folder on Desktop
4. Add all test.txt into the folder
5. Launch Eclipse
6. Open Project and add all .java files into project
7. Run TFTPServer.java  class first
8. Run TFTPClient.java  class second
9. Run TFTPSim.java 	class last
10. Console (TFTPClient.java - console) will offer commands.
10.(OPTIONAL) TFTPServer will ask if you would like to close or not
11. To reach the error test menu in TFTPSim you must select (O)ptions in TFTPClient console then enter Y for test mode 


EXAMPLE __________CONSOLE OUTPUT FOR TFTPClient____________


(R)ead, (w)rite, (o)ptions, or (q)uit?

o

Would you like to turn off verbose mode? Y/N

y

Would you like to turn on test mode?

y

(R)ead, (w)rite, (o)ptions, or (q)uit?

r

Please enter a filename

test3.txt

Client: Packet sent.
Where would you like to save the file?
C:\Users\"USERNAME"\Desktop\

//NOTE "username" would be the username of the machine the project is running on
//MAKE SURE TO END THE FILE PATH WITH "\"


EXAMPLE __________CONSOLE OUTPUT FOR TFTPServer____________

ServerListener: Waiting for packet.
In order to shutdown the server, press (Y) during the prompt.
Shutdown the server? (Y|N)
y
Server is now shutting down.

//NOTE if you don't input anything, then the server will run forever


EXAMPLE __________CONSOLE OUTPUT FOR TFTPSim____________


Simulator: Waiting for packet.
What would you like to simulate:
Enter 0: Normal Operation
Enter 1: Lose a Packet
Enter 2: Delay a Packet
Enter 3: Duplicate a Packet
1
Lose packet selected
Which packet would you like to act on?
[1]: RRQ
[2]: ACK
1


















