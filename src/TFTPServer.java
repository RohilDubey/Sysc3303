

import java.util.Scanner;

public class TFTPServer {
	public static void main(String[] args) {
		
		Thread c = new  TFTPServerListener(); 
        c.start();
        String x;
        Scanner sc= new Scanner (System.in);
        // Ask to shutdown 
        System.out.println("In order to shutdown the server, press (Y) during the prompt.");
        System.out.println("Shutdown the server? (Y|N)");
        x = sc.next();
        // If operator says yes then the server shutdown as threads act independently of the server once created.
        if (x.contains("Y")|| x.contains("y")){
            System.out.println("Server is now shutting down.");
            sc.reset();
            sc.close();
            ((TFTPServerListener) c).setStatus();
            c.interrupt();
            System.exit(0);
         
        }
	}
}
