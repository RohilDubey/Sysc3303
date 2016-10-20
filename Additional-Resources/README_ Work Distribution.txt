



README.txt

SYSC3303 - Iteration 2

TEAM #5

Team Members:

Adam Berg - 100978623
Brydon Gibson - 100975274
Robert Mcmullen - 100977031
Survesh Srinivasan - 100985810


Work Distribution for second Iteration-

Adam Berg - Timing Diagrams, UML's, UCM's, Debugging/Testing
Brydon Gibson - Coding, Debugging
Robert Mcmullen - Coding, Debugging/Testing
Survesh Srinivasan - Majority of Coding, Debugging/Testing



This is a TFTP file transfer program, that sends files between a server and client.

The program is built using UDP packets and a simple protocol to differentiate between reads and writes, acknowledgement packets and request packets.

A read request is sent to port 69, and the server replies with an acknowledgement on a new thread with a port chosen by the JVM.
That port is then used by the server to handle the TFTP transfer.
Each packet is 512 bytes in size, except the last packet, which must be less than 512 bytes, this terminates the transfer.
If the client requests a read, the server sends packets and the client acknowledges them, if the client requests a write, the server sends packets and the client acknowledges.

At the moment, packet loss, duplication, or delay will crash the program. It will currently only function on a perfect network.


Iteration 2 -

Iteration 2, there had to be added support for ERROR packets (1,2,3 and 6). I/O erros had to be assumed to occer, so error packets dealing with this had to be prepared, transmitted, received, and/or handled.
Error codes:
   1         File not found.
   2         Access violation.
   3         Disk full or allocation exceeded.
   6         File already exists.



Setting up and testing instructions:

1. Download .zip folder
2. Extract contents
3. Launch Eclipse
4. Open Project and select extracted files
5. Run TFTPServer.java  class first
6. Run Intermediate.java class second
7. Run TFTPClient.java  class third
8. Console (TFTPClient.java - console) will offer commands.


For example _______________________________________________________

Read or write request? Please enter 'r' for read, or 'w' for write: 
[Press 'e' to Shutdown the Client]

r

You have chosen a read request.
Test or Normal Mode? Please enter 'n' for normal, or 't' for test: 
[Press 'e' to Shutdown the Client]

n


Sending your request to the server directly.
Enter filename with the directory its associated in.

Desktop/Test.txt

____________________________________________________________________


Type (r) or (w) + press Enter, for whether trying to make a read or write request.
Type (n) or (t) + press Enter, for whether trying to run in test or normal mode.
Type the Directory of the test.file being used, followed by a backslash and file name with extension.



For normal mode, run TFTPServer.java followed by TFTPClient.java
For test mode, runTFTPServer.java, followed by Intermediate.java followed by TFTPClient.java (and in the prompts specify the test)


