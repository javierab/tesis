import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class getConfig {


public static void main(String[] args) throws IOException{
	System.out.println(args[0]);
	try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
	    String line;
	    int count = 0;
	    while ((line = br.readLine()) != null) {
	    	String[] l = line.split(" ");
	    	System.out.println("JaviRouter.staticNode" + count + ".id = " + l[1]);
	    	System.out.println("JaviRouter.staticNode" + count + ".x = " + l[2]);
	    	System.out.println("JaviRouter.staticNode" + count + ".y = " + l[3]);
		count++;
	    }
	}
	
}

}
