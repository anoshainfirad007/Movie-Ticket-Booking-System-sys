package oracle;
import java.sql.Statement;
import java.sql.*;
import java.net.Socket;

		public class testport {
		    public static void main(String[] args) {
		    	try (Socket socket = new Socket("10.11.0.22", 1521)) {
		            System.out.println("Port is open.");
		        } catch (Exception e) {
		            System.out.println("Port is closed: " + e.getMessage());
		        }
		          
		        }
		    }
	

	


