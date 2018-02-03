package ServerInfoLog;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class MsgLog {
  static int n=0;
  protected  String defaultLogFile;
  protected FileWriter writer;
  public MsgLog(){
	  defaultLogFile=System.getProperty("logfileDir")+"/"+"serverlog"+getN();
	 // defaultLogFile="serverlog"+getN();

	  try {
		writer=new FileWriter(defaultLogFile,false);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  }
  public void write(String s)throws IOException{
        String time=getCurrentTime();
		writer.write("["+time+"]"+s+"\n");
		writer.flush();
  }
  private void logTerminite() throws IOException {
	// TODO Auto-generated method stub
	 writer.close();
	 n++;
 }
 private int getN(){
	 n++;
	 return n;
 }
 public String getCurrentTime(){
        long unixTime=System.currentTimeMillis();
        Date date=new Date(unixTime);
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time=sdf.format(date);
        return time;
    }
}
