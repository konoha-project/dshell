package dshell.rec;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import libbun.util.LibBunSystem;

import net.arnx.jsonic.JSON;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import dshell.lib.CommandArg;
import dshell.lib.Task;
import dshell.lib.TaskBuilder;
import dshell.lib.TaskOption;
import dshell.lib.Utils;
import static dshell.lib.TaskOption.Behavior.returnable;
import static dshell.lib.TaskOption.Behavior.printable ;
import static dshell.lib.TaskOption.RetType.TaskType   ;

/**
params: {
  type: StringTest.ds,           // test file name
  location: 192.168.59.150,      // host ip address
  data: 1,                       // 0: assert error, 1: assert success, 2: compile error, 3: file not found, 4: assert not found
  authid: xxx@gmail.com,         // mail address
  context: {
    assertpoint: StringTest.ds:12,       // if data is 2, 3 or 4, this is empty string
    content: content of StringTest.ds    // if data is 3, this is empty string
  }
} 
**/
public class RECWriter {
	// data definition
	public final static int asseetError    = 0;
	public final static int assertSuccess  = 1;
	public final static int compileError   = 2;
	public final static int fileNotFound   = 3;
	public final static int assertNotFound = 4;

	public static void invoke(String RECServerURL, String[] scriptArgs) {
		String type = scriptArgs[0];
		String localtion = "";
		String authid = "xyx@gmail.com";
		try {
			localtion = InetAddress.getLocalHost().getHostAddress();
		}
		catch (UnknownHostException e) {
			e.printStackTrace();
			Utils.fatal(1, "Host Problem");
		}
		// execute script file
		ArrayList<ArrayList<CommandArg>> cmdsList = new ArrayList<ArrayList<CommandArg>>();
		ArrayList<CommandArg> cmdList = new ArrayList<CommandArg>();
		cmdList.add(CommandArg.createCommandArg("dshell"));
		for(int i = 0; i < scriptArgs.length; i++) {
			cmdList.add(CommandArg.createCommandArg(scriptArgs[i]));
		}
		cmdsList.add(cmdList);
		TaskOption option = TaskOption.of(TaskType, returnable, printable);
		Task task = (Task) new TaskBuilder(cmdsList, option).invoke();
		// send to REC
		String[] parsedData = parseErrorMessage(task.getErrorMessage());
		if(parsedData != null) {
			int data = Integer.parseInt(parsedData[0]);
			RecAPI.pushRawData(RECServerURL, type, localtion, data, authid, new RecAPI.RecContext(parsedData[1], type));
		}
		else {
			int data = resolveData(task.getExitStatus(), type);
			RecAPI.pushRawData(RECServerURL, type, localtion, data, authid, new RecAPI.RecContext("", type));
		}
		System.exit(0);
	}

	private static String[] parseErrorMessage(String errorMessage) {	// status, failpoint
		String[] lines = errorMessage.split("\n");
		for(String line : lines) {
			if(line.startsWith("REC assert")) {
				line = line.trim();
				String[] splittedLine = line.split(" ");
				if(splittedLine.length == 4) {
					String statusString = splittedLine[2];
					String failPoint = splittedLine[3];
					try {
						int status = Integer.parseInt(statusString);
						if((status == asseetError || status == assertSuccess) && failPoint.startsWith("@(") && failPoint.endsWith(")")) {
							return new String[] {statusString, failPoint.substring(2, failPoint.length() - 1)};
						}
					}
					catch(Exception e) {
					}
				}
				Utils.fatal(1, "invalid assertion message: " + line);
			}
		}
		return null;
	}

	private static int resolveData(int exitStatus, String fileName) {
		if(exitStatus == 0) {
			return assertNotFound;
		}
		if(Utils.isFileReadable(fileName)) {
			return compileError;
		}
		return fileNotFound;
	}
}

class RecAPI {
	/* TODO: return result of rpc */ 
	private static void remoteProcedureCall(String RECServerURL, RecRequest Params) throws IOException {
		String Json = JSON.encode(new JSONRPC(Params));
		StringEntity Body = new StringEntity(Json);

		HttpClient Client = HttpClientBuilder.create().build();
		HttpPost Request = new HttpPost(RECServerURL);
		Request.addHeader("Content-type", "application/json");
		Request.setEntity(Body);
		try {
			HttpResponse response = Client.execute(Request);
			System.out.println(EntityUtils.toString(response.getEntity()));
		}
		catch(Exception e) {
			Utils.fatal(1, "cannot send to REC due to " + e.getMessage());
		}
	}

	public static void pushRawData(String RECServerURL, String type, String location, int data, String authid, RecContext context) {
		try {
			remoteProcedureCall(RECServerURL, new RecRequest(type, location, data, authid, context));
		}
		catch(IOException e) {
			e.printStackTrace();
			Utils.fatal(1, "IO problem");
		}
	}

	public static class JSONRPC {
		public String jsonrpc;
		public String method;
		public int id;
		public RecRequest params;

		private static double JsonRPCVersion = 2.0;
		private static int Id = 0;

		public JSONRPC(RecRequest recRequest) {
			this.jsonrpc = Double.toString(JsonRPCVersion);
			this.method = "pushRawData";
			this.id = Id;
			this.params = recRequest;
		}
	}

	public static class RecRequest {
		public String type;
		public String location;
		public int data;
		public String authid;
		public RecContext context;

		public RecRequest(String type, String location, int data, String authid, RecContext context) {
			this.type = type;
			this.location = location;
			this.data = data;
			this.authid = authid;
			this.context = context;
		}
	}

	public static class RecContext {
		public String assertpoint;
		public String content;

		public RecContext(String assertpoint, String fileName) {
			String fileContent = LibBunSystem._LoadTextFile(fileName);
			if(fileContent == null) {
				fileContent = "";
			}
			this.assertpoint = assertpoint;
			this.content = fileContent;
		}
	}
}