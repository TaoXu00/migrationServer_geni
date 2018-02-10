package migrationServer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import ServerInfoLog.MsgLog;
import dataType.KeyPair;
import dataType.Range;

public class ClientWorker extends Thread {
	 private MsgLog msglog=new MsgLog();
	 private Socket socket;
	 private String log;
	 private String container;
	 private String ip;
	// private String srcIp;
	//private String destIp;
	 private String src="srcHost";
	 private String dest="destHost";
	 int port=Integer.parseInt(System.getProperty("port"));
	 String password=System.getProperty("password");
	 private DataOutputStream  serverOut;
	 private DataInputStream  serverIn;
	 private DataOutputStream ClientOut;
	 private DataInputStream  ClientIn;
     private int fileNumber=0;
	 private String baseDir=System.getProperty("user.home")+"/"+System.getProperty("migrationServerDirName")+"/";

	public ClientWorker(Socket socket){
		this.socket=socket;	
	}
	public void run(){
		try{
		     log="connected:"+socket;
		     writeTolog(log);
			 serverIn =new DataInputStream(socket.getInputStream());
			 serverOut=new DataOutputStream(socket.getOutputStream());
			// BufferedReader inBufferReader=new BufferedReader(new InputStreamReader(serverIn));
		     @SuppressWarnings("deprecation")
			 String s=serverIn.readLine();
		     log="receive from client:"+s;
		     writeTolog(log);
		     excuteCommand(s);
		     closeConnection(serverIn,serverOut,socket);
		     terminateThread();
		 }catch(Exception e){
			 e.printStackTrace();
		 }
	}
	public int excuteCommand(String command){
		String action=null;
		String[] parts=null;
        parts=command.split(" ");
        action=parts[0];
		//}
		int res = 0;
		try{
		switch(action){
		   case "START":
			   String image=parts[1];
			   container=parts[2];
			   startContainer(image,container);
/**********************for test the migration**************h1 as the iperf client******************************************/
			   //startIperfServer();
			   writeTolog("iperf server started...."+ getCurrentTime());
			   
/***************************************************************************************************************************/
			   break;
		   case "MIGRATE":
			   ip=parts[1];
			   String targetIP=parts[2];
			   String session_id=parts[3].split(":")[1];
			   res= migrateContainer(targetIP,session_id);
			   return res;
		   case "RESTART":
			   //first read the total file number
			    ip=parts[1];
			    writeTolog("["+dest+"]in RESTART mode.");
			    DataOutputStream dos=new DataOutputStream(serverOut);
			    DataInputStream dis=new DataInputStream(serverIn); 	
			    String imageRestart=dis.readLine();
			    writeTolog("["+dest+"]will restart image "+ imageRestart);
			    int tol=dis.readInt();
			    writeTolog("["+dest+"]will receive "+tol+" files");
			    writeTolog("["+dest+"]starting to receive the files....");
			    int rescode1=receivefiles(tol,dis);
			    if(rescode1==0){
			    writeTolog("["+dest+"]file receive done");
			    writeTolog("["+dest+"]Restarting new container...");
			    int rescode2=restartContainer(imageRestart);
			    //after restart corrently,inform the source host success information
			    if(rescode2==0){ //means everything is okay for the migration process.
			    //send success message to the source host to stop the container
			    dos.writeBytes("SUCCESS\n");
			    writeTolog("["+dest+"]new container "+ip+"-"+imageRestart+"starts successfully!");
			    writeTolog("["+dest+"]send 'SUCCESS' msg to source host.");
/***************First kill the server then restart the iperf server,only for test***************************************/
			    //killIperfServer();
			    //System.out.println("iperf server stoped...."+getCurrentTime());
			   // startIperfServer();
			    writeTolog("iperf server restarted...."+getCurrentTime());
			    Thread.sleep(120000);
			    /****************************test container and migration directory clean up *************/
			    cleanup();
			    }
			    else{
			     dos.writeBytes("ERROR\n");
			     dos.writeInt(rescode2);
			     writeTolog("["+dest+"]new container "+imageRestart+"-clone "+"starts Error!");
				 writeTolog("["+dest+"]send 'ERROR' msg with err code "+rescode2+" to source host.");
			    }
			    }else{
			    	dos.writeBytes("ERROR\n");
				    dos.writeInt(rescode1);
				    writeTolog("["+dest+"]new container "+imageRestart+"-clone "+"starts Error!");
					writeTolog("["+dest+"]send 'ERROR' msg with err code "+rescode1+" to source host.");	
			    }
			   /* closeConnection(dis,dos,socket);
			    terminateThread();*/
			    break; 
		   case "IperfClient":
			   String IperfServer=parts[1];
			   startIperfClient(IperfServer);
			   writeTolog("iperf client started...."+getCurrentTime());
			   break;
		   case "REC_SECINFO":
			   String keyPair_X=parts[1];
			   String keyPair_Y=parts[2];
			   KeyPair kp=new KeyPair(Integer.parseInt(keyPair_X),Integer.parseInt(keyPair_Y));
			   Server.setKeyPair(kp);
			   DataInputStream dis1=new DataInputStream(serverIn); 
			   String filename=dis1.readUTF();
	    	   long filesize=dis1.readLong();
	    	   String path=System.getProperty("lookupTableDir")+"/"+filename;
	    	   Server.setLookupTable(path);
	    	   receivefile(path,filename,filesize,dis1);
	    	   break;
		   case "SECURE_MIGRATE":
			   String mode=parts[1];
			   String randomNumber=parts[2];
			   int keyNumber=Integer.parseInt(parts[3]);
			   ip=parts[4];
			   String session_Id=parts[5].split(":")[1];
			   ObjectInputStream objin=new ObjectInputStream(socket.getInputStream());
			   HashMap<String,Integer> lantencyTable=(HashMap)objin.readObject(); 
			   writeTolog("receive lantencyTable length:"+lantencyTable.size());
			  
			   if(mode.equals("Digital_Fountain"))
				   res=excuteDigitalFountain(randomNumber,lantencyTable,keyNumber,session_Id);
			   else if(mode.equals("Shamir")){
				   res=executeShamir(randomNumber,lantencyTable,keyNumber,session_Id);
			   }
			   return res;
		case "GET_KEY":
			   sendKeyPair();
		   }   
		    }catch(Exception e){
		    	e.printStackTrace();
		    }   
			return 0;  
		}
	private int excuteDigitalFountain(String randomNumber, HashMap<String, Integer> lantencyTable,int keyNumber,String session_id) throws Exception {
		// TODO Auto-generated method stub
		writeTolog("i am excuting Digital_Fountain");
		HashMap<String,Double> probability=generateProbabilityTable(lantencyTable);
		HashMap<Range,String> checkupTable=generateCheckupTable(probability);
		LinkedList<String> selectedhosts=selectedHosts(checkupTable,keyNumber);
		//key pairs in order to decrypt the file
		List<KeyPair> keypairs=getKeyPairs(selectedhosts);
		int index=applyHashFunction(randomNumber,lantencyTable.size()+1);
		String targetIP=decryptGetDstHost(index);
		return migrateContainer(targetIP,session_id);
	}
	
	private int executeShamir(String randomNumber, HashMap<String, Integer> lantencyTable,int keyNumber,String session_id) throws Exception {
		// TODO Auto-generated method stub
		//if receive shamir comand it need to ask for other random N hosts for the key
		//first select N random other host from the lantency table it received
		LinkedList<String> hosts=new LinkedList<String>(lantencyTable.keySet());
		LinkedList<String> selectedhosts=selectKhosts(hosts,keyNumber);
		List<KeyPair> keypairs=getKeyPairs(selectedhosts);
		int index=applyHashFunction(randomNumber,lantencyTable.size()+1);
		String targetIP=decryptGetDstHost(index);
		return migrateContainer(targetIP,session_id);
	}
	private String decryptGetDstHost(int index) throws Exception {
		// TODO Auto-generated method stub
		String lookupTable=Server.getLookupTable();
		FileInputStream fstream=new FileInputStream(lookupTable);
		DataInputStream in=new DataInputStream(fstream);
		BufferedReader br=new BufferedReader(new InputStreamReader(in));
		String strLine;
		String destHost = null;
		while((strLine=br.readLine())!=null){
			String[] parts=strLine.split(" ");
			if(parts[0].equals(Integer.toString(index))){
				 destHost=parts[1];
				 break;
			}
		}
		in.close();
		return destHost;
	}
	public int applyHashFunction(String randomNumber,int N) throws InvalidKeyException, NoSuchAlgorithmException {
		// TODO Auto-generated method stub
		KeyPair kp=Server.getKeyPair();
		String x=Integer.toString(kp.getX());
		String y=Integer.toString(kp.getY());
		String data=x.concat(y);
		String key=randomNumber;
		String res=generateHMacSHA256(key,data);
		String num1=res.replaceAll("[^0-9]","");
		String num;
		//System.out.println("Hash num:"+num1+" length:"+num1.length());
		if(num1.length()>9)
			num=num1.substring(0,9);
		else
			num=num1;
		//System.out.println("num:"+num);
		int index=Integer.parseInt(num)%N;
		return index;
	}
	public String generateHMacSHA256(String key, String data)
			throws InvalidKeyException, NoSuchAlgorithmException {
		Mac hMacSHA256 = Mac.getInstance("HmacSHA256");
		byte[] hmacKeyBytes = key.getBytes();
		final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA256");
		hMacSHA256.init(secretKey);
		byte[] dataBytes = data.getBytes();
		byte[] res = hMacSHA256.doFinal(dataBytes);
		return DatatypeConverter.printBase64Binary(res);
	}
	private int migrateContainer(String targetIP,String session_id) throws Exception{
		   log="["+src+"]in MIGRATE mode.";
		   writeTolog(log);
		   Runtime r=Runtime.getRuntime();
		   String checkpointDir=baseDir+System.getProperty("ContainerStopDir");
		   System.out.println("checkpointdir:"+checkpointDir);
		   String container=ip+"-"+System.getProperty("container"); 
		   String[] cmd={"/bin/bash","-c","echo "+password+" | sudo -S docker checkpoint create --checkpoint-dir="+checkpointDir+" "+container+" checkpoint --leave-running"};
		   Process p=r.exec(cmd);
		   BufferedReader stdInput=new BufferedReader(new InputStreamReader(p.getInputStream()));
		   String res=stdInput.readLine();
		   //System.out.println("response:"+res);
		   if(!res.equals("checkpoint")){
			 log="["+src+"]Error:"+res;
			 writeTolog(log);
			 return -1;      //err code
		   }else{
			   writeTolog("["+src+"]Container "+container+" current status stored");
			   log="["+src+"]targetIP:"+targetIP;
			   writeTolog(log);
			   //now the server as a client, open another connection with the destination Host(Server)
			  int errcode=excuteFileTrasfer(targetIP);
			  if(errcode!=0){
			  //write to app thStart container msgat the error code
			   String response="Session_Id:"+session_id+"  ERROR\n";
			   serverOut.writeBytes(response);
			   serverOut.writeInt(errcode);
			  // terminateThread();
			  }else if(errcode==0){
				  log="["+src+"]Session_Id:"+session_id+" receive 'SUCCESS' from destination host";
				  writeTolog(log);
				  String response="Session_Id:"+session_id+" SUCCESS\n";
				  serverOut.writeBytes(response);
				  stopcontainer(container); 
				  writeTolog("["+src+"]container "+container+" stopped"); 
				  writeTolog("["+src+"]'SUCCESS' msg send to Migration Controller'");
				  /***********************clean container and folder************************************/
				  cleanup();
			  }
			  
		   }
		   p.waitFor();
		   return 0;
	}
	private void sendKeyPair() throws IOException {
		// TODO Auto-generated method stub
		 ObjectOutputStream objOut=new ObjectOutputStream(socket.getOutputStream());
		 KeyPair kp=Server.getKeyPair();
		 objOut.writeObject(kp);
		 writeTolog("send KeyPair with x="+kp.getX()+"y="+kp.getY());
		 socket.close();
	}
	
	
	private LinkedList<String> selectedHosts(HashMap<Range, String> checkupTable,int keyNumber) {
		// TODO Auto-generated method stub
		LinkedList<String> hosts=new LinkedList<String>();
		while(keyNumber!=0){
		   double num=Math.random();
		   String host=getHost(num,checkupTable);
		  // System.out.println("Selected host:"+host);
		   hosts.add(host);
		   keyNumber--;
		}
		return hosts;
	}
	private String getHost(double num, HashMap<Range, String> checkupTable) {
		// TODO Auto-generated method stub
		for(Map.Entry<Range, String> entry:checkupTable.entrySet()){
			if(num>=entry.getKey().getMin() && num<entry.getKey().getMax())
				return entry.getValue();
		}
		return null;
	}
	private HashMap<Range,String> generateCheckupTable(HashMap<String, Double> probability) {
		// TODO Auto-generated method stub
		HashMap<Range,String> checkTable=new HashMap<Range,String>();
	    double min=0;
		//for(int n:probability.values()){
		  for(Map.Entry<String, Double> entry:probability.entrySet()){
			double max=min+entry.getValue();
			Range r=new Range(min,max);
			checkTable.put(r, entry.getKey());
			//System.out.println("min:"+min+" max:"+max+" "+entry.getKey());
			min=max;
		}
		
		return checkTable;
	}
	private HashMap<String, Double> generateProbabilityTable(HashMap<String, Integer> lantency) {
		// TODO Auto-generated method stub
		HashMap<String,Double> probTable=new HashMap<String,Double>();
		double sum=0;
		for(int l:lantency.values()){
			sum+=1.0/(double)l;
		//	System.out.println("pro:"+1.0/(double)l);
		//	System.out.println("sum:"+sum);
		}
		for(Map.Entry<String, Integer> entry:lantency.entrySet()){
			double prob=(1.0/(double)entry.getValue())/sum;
			//int pro=(int)prob*100;
			probTable.put(entry.getKey(),prob);
			//System.out.println(entry.getKey()+" lantency "+entry.getValue()+" prob:"+prob);
		}
		//return null;
		return probTable;
	}
	
	
	
	private List<KeyPair> getKeyPairs(LinkedList<String> selectedhosts) {
		// TODO Auto-generated method stub
		List<KeyPair> keypairList=new LinkedList<KeyPair>();
		for(String host:selectedhosts){
			KeyPair kp=getkeypair(host);
			keypairList.add(kp);
		}
		return keypairList;
	}
	
	private KeyPair getkeypair(String targetIP) {   //this also can be a thread
		// TODO Auto-generated method stub
		    Socket clientSocket = null;
		    KeyPair kp;
			try {
				clientSocket = startConnection(targetIP);
				writeTolog("["+src+"]"+"now i am a client,connect to "+targetIP+" to ask key pair");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				return null;
			}
			PrintWriter clientOutPrintWriter=null;
		    try{
		    clientOutPrintWriter=new PrintWriter(clientSocket.getOutputStream(),true);
		    clientOutPrintWriter.print("GET_KEY\r\n");
		    clientOutPrintWriter.flush();
		    ObjectInputStream objIn=new ObjectInputStream(clientSocket.getInputStream());
		    kp=(KeyPair)objIn.readObject();
		    writeTolog("get KeyPair:"+kp.getX()+" "+kp.getY());
		    /*String res=ClientIn.readLine(); 
		    int x=Integer.parseInt(res.split(" ")[0]);
		    int y=Integer.parseInt(res.split(" ")[1]);
		    kp=new KeyPair(x,y);	*/
		    clientSocket.close();
		    }catch(Exception e){
				clientOutPrintWriter.close();
				return null;
			}
		    return  kp;
		}
		
	
	private LinkedList<String> selectKhosts(LinkedList<String> hosts, int keyNumber) {
		// TODO Auto-generated method stub
		LinkedList<String> selectedHosts=new LinkedList<String>();
		while(keyNumber!=0){
			int n=randomNumGeneration(hosts.size());
			String host=hosts.get(n);
			if(!selectedHosts.contains(host)){
				selectedHosts.add(host);
				keyNumber--;
			}
		}
		return selectedHosts;
	}
	public int randomNumGeneration(int length) {
		// TODO Auto-generated method stub
		Random rand=new Random(System.nanoTime()%100000);
	//	rand.setSeed(System.currentTimeMillis());
		int n=rand.nextInt(length);
		return n;
	}
	private void startIperfClient(String IperfServer) throws Exception{
		Runtime r=Runtime.getRuntime();
		String time=System.getProperty("iperfTime");
		String[] startIperf={"/bin/bash","-c","iperf -c "+IperfServer+" -u -t "+time+" -i 2"};
		Process P=r.exec(startIperf);
	}
	private void killIperfServer() throws Exception {
		// TODO Auto-generated method stub
		Runtime r=Runtime.getRuntime();
		String[] killIperf={"/bin/bash","-c","pkill iperf"};
		Process P=r.exec(killIperf);
		P.waitFor();
	}
	private void startIperfServer() throws Exception {
		// TODO Auto-generated method stub
		Runtime r=Runtime.getRuntime();
		String[] startIperf={"/bin/bash","-c","iperf -s"};
		Process P=r.exec(startIperf);
	}
	private void startContainer(String image, String Container) throws Exception{
		   Runtime r=Runtime.getRuntime();
		   String port=System.getProperty("containerExposePort");
		   String bindport="-p "+port+":"+port;
		   String[] createContainer={"/bin/bash","-c","echo "+password+" | sudo -S  docker create --name "+Container+" "+bindport+" "+image};
		   Process p1=r.exec(createContainer);	
		   p1.waitFor();
		   String[] startContainer={"/bin/bash","-c","echo "+password+" | sudo -S  docker start "+Container};
		   Process p2=r.exec(startContainer);	  
	       BufferedReader in=new BufferedReader(new InputStreamReader(p2.getInputStream()));
	       String line;
	       String res;
	      while((line=in.readLine())!=null){
	    	   if(line.equals(Container)){
                    res="OK\n";
                    writeTolog("start container "+Container+" ok!");
	    	   }
               else{
            	    res="Container Start Error\n";
            	    writeTolog("start container "+Container+" Error!");
            	}   
	    	   serverOut.writeBytes(res);
	       }   
		   p2.waitFor();
		
	}
    private void stopcontainer(String Container) throws Exception {
		// TODO Auto-generated method stub
       Runtime r=Runtime.getRuntime();
	   String[] stopContainer={"/bin/bash","-c","echo "+password+" | sudo -S  docker stop "+Container};
	   Process p1=r.exec(stopContainer);	
	   p1.waitFor();
	}
	private void terminateThread() {
		// TODO Auto-generated method stub
		Thread.currentThread().interrupt();
	}
	private void closeConnection(DataInputStream dis, DataOutputStream dos, Socket socket) throws IOException {
		// TODO Auto-generated method stub
		dis.close();
		dos.close();
		socket.close();
	}
	private int restartContainer(String imageRestart) {
		//first need to create a new container of the image then restart the the containner
    	   try{
    	   Runtime r=Runtime.getRuntime();
    	   String port=System.getProperty("containerExposePort");
    	   String bindport="-p "+port+":"+port;
    	   String newContainer=ip+"-"+System.getProperty("container");
    	   String ContainerRestoreDir=baseDir+System.getProperty("ContainerRestoreDir")+"/"+imageRestart;
		   String[] createContainer={"/bin/bash","-c","echo "+password+" | sudo -S  docker create --name "+newContainer+" "+bindport+" "+imageRestart};
		   Process p1=r.exec(createContainer);	
		   p1.waitFor();
		   writeTolog("["+dest+"]new container "+newContainer +" created.");
		   String[] restartContainer={"/bin/bash","-c","echo "+password+" | sudo -S  docker start --checkpoint-dir="+ContainerRestoreDir+" --checkpoint=checkpoint "+newContainer};
    	   Process P2=r.exec(restartContainer);
    	   P2.waitFor();
    	   writeTolog("["+dest+"]new container "+newContainer+" started.");
    	   }catch(Exception e){
    		   return -5;
    	   }
    	   return 0;
	}
	private int receivefiles(int tolfile,DataInputStream dis) {
		// TODO Auto-generated method stub
		try{
    	String basePath=System.getProperty("ContainerRestoreDir");
    	for(int i=0;i<tolfile;i++){
    		String filename=dis.readUTF();
    		long filesize=dis.readLong();
    		String path=basePath+"/"+filename;
    		receivefile(path,filename,filesize,dis);
    	}
		}catch(Exception e){
			return -4;
		}
		return 0;
	}
	private void receivefile(String path,String filename, long filesize,DataInputStream dis) throws IOException {
		// TODO Auto-generated method stub
		//first check if the parent dir exist or not
		int n=0;
		long filesizeReceive=filesize;
		byte[] buf=new byte[4092];
	    File file=new File(path);
	    if(!file.getParentFile().exists())
	    	file.getParentFile().mkdirs();
	    //System.out.println(path);
	    FileOutputStream fos=new FileOutputStream(path);
		while(filesize>0 &&(n=dis.read(buf,0,(int)Math.min(buf.length,filesize)))!=-1)
		{
			fos.write(buf,0,n);
			filesize-=n;
		}
		fos.close();
		writeTolog("["+dest+"]"+filename+" size "+filesizeReceive+" bytes received");
	}
	public int excuteFileTrasfer(String targetIP) throws IOException {
		// TODO Auto-generated method stub	
    	String image=System.getProperty("image");
    	
  /****************first need to open a new connection to the target host and send the command**************/
	    Socket clientSocket = null;
		try {
			clientSocket = startConnection(targetIP);
			writeTolog("["+src+"]"+"now i am a client,connect to "+targetIP);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			return -3;
		}
		PrintWriter clientOutPrintWriter=null;
	    try{
	    clientOutPrintWriter=new PrintWriter(clientSocket.getOutputStream(),true);
	    clientOutPrintWriter.print("RESTART "+targetIP+"\r\n");
	    clientOutPrintWriter.flush();
	    clientOutPrintWriter.print(image+"\r\n");
	    clientOutPrintWriter.flush();
	    ClientOut=new DataOutputStream(clientSocket.getOutputStream());
	    ClientIn=new DataInputStream(clientSocket.getInputStream());
	    writeTolog("["+src+"]send the RESTART "+image+" command to destination host");
		File[] files=fileIteration();
  /****************then start to send files**********************************/	
		int filenum=countFiles(files);
		ClientOut.writeInt(filenum);
		writeTolog("["+src+"]will send "+filenum+" files");
		writeTolog("["+src+"]starting transfer files...");
		//then write the fileName list,along with the size of the file.
		//first change the permission of the file to get the permission to read it [criu.work/dump.log]
		checkReadPermission(files);
		sendFiles(files,ClientOut,image);
		writeTolog("["+src+"]file transfer done.");
		writeTolog("["+src+"] waitting for destination host migration status...");
  /***********after send all the files wait for the reply if the container restart success or not***********/
		String res=ClientIn.readLine();
		if(res.equals("SUCCESS"))
			return 0;
		else if(res.equals("ERROR")){
			int errcode=ClientIn.readInt();
			return errcode;
		}
		}catch(Exception e){
			clientOutPrintWriter.close();
			closeConnection(ClientIn,ClientOut,clientSocket);
			return -2;
		}
	    clientOutPrintWriter.close();
	    closeConnection(ClientIn,ClientOut,clientSocket);
    	return 0; 
	 }
	 private void checkReadPermission(File[] files) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		 for(File file:files){
			 if(file.isDirectory()){
				// writeTolog("Directory:+ "+file.getName());
				 checkReadPermission(file.listFiles());
			 }else{
				 File myTestFile=new File(file.getPath());
				 if(!myTestFile.canRead()){
				 writeTolog("["+src+"]can not read File: "+file.getName());
				 changePermission(file.getPath());
				 }
				 
			 }
		 }
	}
	private void sendFiles(File[] files,DataOutputStream ClientOut,String image) throws IOException {
		// TODO Auto-generated method stub
		
		for(File file:files){
			 
			 if(file.isDirectory()){
				// writeTolog("Directory:+ "+file.getName());
				 sendFiles(file.listFiles(),ClientOut,image);
			 }else{
				 String path=file.getPath();
				 sendFile(path,ClientOut,image);
				
			 }
		 }
	}
	private void changePermission(String path) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		   Runtime r=Runtime.getRuntime();
		   String[] cmd={"/bin/bash","-c","echo "+password+" | sudo -S chmod +r -R "+path};
		   Process p=r.exec(cmd);	
		   p.waitFor();
		   writeTolog("["+src+"]now cheanged the permission of the file "+path);
	}
	public Socket startConnection(String ip) throws Exception{
		 Socket clientSocket=null;
			clientSocket=new Socket(ip,port);
		 return clientSocket;
     }
	 public  File[] fileIteration(){
		 String dir=System.getProperty("ContainerStopDir");
		 File[] files=new File(dir).listFiles();
		 return files;
	 }
	 public  void showFiles(File[] files) throws IOException{
		 for(File file:files){
			 if(file.isDirectory()){
				 writeTolog("["+src+"]Directory:+ "+file.getName());
				 showFiles(file.listFiles());
			 }else{
				 writeTolog("["+src+"]File: "+file.getName());
				 
			 }
		 }
	 }
	
	 public int countFiles(File[] files){		
		 for(File file:files){
			 if(file.isDirectory()){
				 countFiles(file.listFiles());
			 }else
				 fileNumber++;
		 }
		 return fileNumber;
	 }
	 
	private void sendFile(String path,DataOutputStream out,String image) throws IOException{
		// TODO Auto-generated method stub
		//first send file name,which is the relative path of checkpoint
		int start=path.indexOf("checkpoint");
		String oldfilename=path.substring(start);
		String filename=oldfilename.replaceAll("checkpoints", image);
		out.writeUTF(filename);
		out.flush();
		//second send the file size
		File file=new File(path);
		long filesize=file.length();
		out.writeLong(filesize);
		out.flush();
	//	writeTolog(" filesize:"+filesize);
        //finally send the file content of the file
		FileInputStream fis=new FileInputStream(path);
		int n=0;
		byte[] buf=new byte[4092];
		while((n=fis.read(buf))!=-1){
			out.write(buf,0,n);
			out.flush();
		}
		fis.close();
		writeTolog("["+src+"]"+filename+" size "+filesize+" bytes send");
	}
	public void writeTolog(String s) throws IOException{
		   System.out.println(s);
		   msglog.write(s);
	}
	public String getCurrentTime(){
 	    long unixTime=System.currentTimeMillis();
      Date date=new Date(unixTime);
      SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      sdf.setTimeZone(TimeZone.getTimeZone("UTC-6"));
      String time=sdf.format(date);
      return time;
      }
      public void cleanup() throws Exception{
		  String[] cmdScript = new String[]{"/bin/bash", "cleanup.sh"};
		  Process procScript = Runtime.getRuntime().exec(cmdScript);
	  }
}
