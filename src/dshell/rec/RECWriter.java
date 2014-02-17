package dshell.rec;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import zen.deps.LibZen;

import dshell.lib.Task;
import dshell.lib.TaskBuilder;
import dshell.lib.TaskOption;
import dshell.util.Utils;

import static dshell.lib.TaskOption.Behavior.returnable;
import static dshell.lib.TaskOption.Behavior.printable ;

import static dshell.lib.TaskOption.RetType.TaskType   ;

/*
 * params: {
 *   type: StringTest.ds,           // test file name
 *   location: 192.168.59.150,      // host ip address
 *   data: 1,                       // 0: assert error, 1: assert success, 2: compile error, 3: file not found, 4: assert not found
 *   authid: xxx@gmail.com,         // mail address
 *   context: {
 *     failpoint: StringTest.ds:12,         // if data is 2, 3 or 4, this is empty string
 *     content: content of StringTest.ds    // if data is 3, this is empty string
 *   }
 * }
 * 
 * */
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
		ArrayList<ArrayList<String>> cmdsList = new ArrayList<ArrayList<String>>();
		ArrayList<String> cmdList = new ArrayList<String>();
		cmdList.add("dshell");
		for(int i = 0; i < scriptArgs.length; i++) {
			cmdList.add(scriptArgs[i]);
		}
		cmdsList.add(cmdList);
		TaskOption option = TaskOption.of(TaskType, returnable, printable);
		Task task = (Task) new TaskBuilder(cmdsList, option).invoke();
		// send to REC
		String[] parsedData = parseErrorMessage(task.getErrorMessage());
		if(parsedData != null) {
			int data = Integer.parseInt(parsedData[0]);
			RecAPI.pushRawData(RECServerURL, type, localtion, data, authid, createContext(parsedData[1], type));
		}
		else {
			int data = resolveData(task.getExitStatus(), type);
			RecAPI.pushRawData(RECServerURL, type, localtion, data, authid, createContext("", type));
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

	private static String createContext(String failPoint, String fileName) {
		String fileContent = LibZen.LoadTextFile(fileName);
		if(fileContent == null) {
			fileContent = "";
		}
		String json = "{" + RecAPI.quote("failpoint") + ": " + RecAPI.quote(failPoint) + ", "
						  + RecAPI.quote("content") + ": " + RecAPI.quote(fileContent) +
					  "}";
		return json;
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
	private static void remoteProcedureCall(String RECServerURL, String Method, String Params) throws IOException {
		double JsonRPCVersion = 2.0;
		int Id = 0;
		String Json = "{" + quote("jsonrpc") + ": " + quote(Double.toString(JsonRPCVersion)) + ", "
						  + quote("method") + ": " + quote(Method) + ", "
						  + quote("id") + ": " + Integer.toString(Id) + ", "
						  + quote("params") + ": " + Params + 
					  "}";

		StringEntity Body = new StringEntity(Json);

		HttpClient Client = HttpClientBuilder.create().build();
		HttpPost Request = new HttpPost(RECServerURL);
		Request.addHeader("Content-type", "application/json");
		Request.setEntity(Body);
		try {
			Client.execute(Request);   // TODO: check response
		}
		catch(Exception e) {
			Utils.fatal(1, "cannot send to REC due to " + e.getMessage());
		}
	}

	public static void pushRawData(String RECServerURL, String Type, String Location, int Data, String AuthId, String Context) {
		String Params = "{" + quote("type") + ": " + quote(Type) + ", "
						    + quote("localtion") + ": " + quote(Location) + ", "
						    + quote("data") + ": " + Integer.toString(Data) + ", "
						    + quote("authid") + ": " + quote(AuthId) + ", "
						    + quote("context") + ": " + Context +
						"}";
		try {
			remoteProcedureCall(RECServerURL, "pushRawData", Params);
		}
		catch(IOException e) {
			e.printStackTrace();
			Utils.fatal(1, "IO problem");
		}
	}

	public static String quote(String str) {
		return "\"" + str + "\"";
	}
}