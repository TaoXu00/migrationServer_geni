import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Properties;

import ServerInfoLog.MsgLog;
import migrationServer.Server;

public class ServerApplication {
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
         Server server=new Server();
         server.initializeFromConfigurationFileForJARFile();
      //    server.initializeFromConfigurationFile();
        server.runServer();
	}

}
