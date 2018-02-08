package migrationServer;

import java.io.*;
import java.net.*;
import java.util.Properties;

import dataType.KeyPair;

public class Server {
	//Logger logger=Logger.getLogger("MyLog");
	//FileHandler fh;
	private static KeyPair kp;
	private static String lookupTable;
	public void runServer(){	 
    	 ServerSocket server=null;    	 
    	 String port=System.getProperty("port");
    	 try{
    		 server=new ServerSocket(Integer.parseInt(port));	
    		 System.out.println("Binding to port "+port+",waiting for connection......");
    	 }catch(IOException e){
    		 System.out.println(e);
    	 }
    	 try{
    		while(true){
    	     Socket socket=server.accept();
    	     new ClientWorker(socket).start();   		
    		 }
    	 }catch(Exception e){
    		 System.out.print(e);
    	 }
     } 
   public static void setLookupTable(String lookuptable){
	   lookupTable=lookuptable;
   }
   public static String getLookupTable(){
	   return lookupTable;
   }
   public static void setKeyPair(KeyPair kpair){
	   kp=kpair;
   }
   public static KeyPair getKeyPair(){
	   return kp;
   }
   public void initializeFromConfigurationFileForJARFile() throws IOException{
	   Properties mainProperties=new Properties(System.getProperties());
	   FileInputStream file;
	   String path= "configuration";
	   file =new FileInputStream(path);
	   mainProperties.load(file);
	   System.setProperties(mainProperties);
	   file.close();
   }
	public  void initializeFromConfigurationFile() throws Exception{
		URL resource=ClassLoader.getSystemResource("configuration");
		File file=new File(resource.toURI());
		FileInputStream confFile=new FileInputStream(file);
	    Properties p=new Properties(System.getProperties());
	    p.load(confFile);
	    System.setProperties(p);
	}		
	 public void startConnection(String ip,int port){
   	  try {
			Socket clientSocket = new Socket(ip,port);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
     }		
		
	}

