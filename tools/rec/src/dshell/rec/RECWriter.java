package dshell.rec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

import libbun.encode.jvm.DShellByteCodeGenerator;
import libbun.encode.jvm.Generator4REC;
import libbun.util.LibBunSystem;

import net.arnx.jsonic.JSON;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import dshell.DShell.GeneratorFactory;
import dshell.lib.Utils;

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

	private boolean isWriterMode = true;
	private String recURL;
	private String[] scriptArgs;

	public static void main(String[] args) {
		new RECWriter(args).execute();
	}

	public RECWriter(String[] args) {
		for(int i = 0; i < args.length; i++) {
			String option = args[i];
			if(option.startsWith("--")) {
				if(option.equals("--writer") && i + 1 < args.length) {
					this.isWriterMode = true;
					this.recURL = args[++i];
				}
				else if(option.equals("--executor")) {
					this.isWriterMode = false;
				}
				else {
					System.err.println("invalid option: " + option);
				}
			}
			else {
				int size = args.length - i;
				this.scriptArgs = new String[size];
				System.arraycopy(args, i, this.scriptArgs, 0, size);
				return;
			}
		}
		System.err.println("not found script");
		System.exit(1);
	}

	public void execute() {
		if(this.isWriterMode) {
			this.invoke(this.recURL, this.scriptArgs);
		}
		else {
			DShellByteCodeGenerator generator = new GeneratorFactory(Generator4REC.class, TypeChecker4REC.class).createGenerator();
			String scriptName = this.scriptArgs[0];
			generator.loadArg(this.scriptArgs);
			boolean status = generator.loadFile(scriptName);
			if(!status) {
				System.err.println("abort loading: " + scriptName);
				System.exit(1);
			}
			generator.invokeMain(); // never return
		}
	}

	private void invoke(String recServerURL, String[] scriptArgs) {
		String type = scriptArgs[0];
		String localtion = "";
		String authid = "xyx@gmail.com";
		try {
			localtion = InetAddress.getLocalHost().getHostAddress();
		}
		catch(UnknownHostException e) {
			e.printStackTrace();
			Utils.fatal(1, "Host Problem");
		}
		// execute script file
		ArrayList<String> argList = new ArrayList<String>();
		argList.addAll(Arrays.asList("java", "-jar", "rec.jar", "--executor"));
		argList.addAll(Arrays.asList(this.scriptArgs));

		final ByteArrayOutputStream streamBuffer = new ByteArrayOutputStream();
		ProcessBuilder procBuilder = new ProcessBuilder(argList);
		procBuilder.inheritIO();
		procBuilder.redirectError(Redirect.PIPE);
		try {
			final Process proc = procBuilder.start();
			Thread streamHandler = new Thread() {
				@Override public void run() {
					InputStream input = proc.getErrorStream();
					byte[] buffer = new byte[512];
					int ReadSize = 0;
					try {
						while((ReadSize = input.read(buffer, 0, buffer.length)) > -1) {
							streamBuffer.write(buffer, 0, ReadSize);
							System.err.write(buffer, 0, ReadSize);
						}
						input.close();
					}
					catch (IOException e) {
						return;
					}
				}
			};
			streamHandler.start();
			streamHandler.join();
			int exitStatus = proc.waitFor();
			// send to REC
			String[] parsedData = this.parseErrorMessage(streamBuffer.toString());
			if(parsedData != null) {
				int data = Integer.parseInt(parsedData[0]);
				RecAPI.pushRawData(recServerURL, type, localtion, data, authid, new RecAPI.RecContext(parsedData[1], type));
			}
			else {
				int data = this.resolveData(exitStatus, type);
				RecAPI.pushRawData(recServerURL, type, localtion, data, authid, new RecAPI.RecContext("", type));
			}
			System.exit(0);
		}
		catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private String[] parseErrorMessage(String errorMessage) {	// status, failpoint
		String[] lines = errorMessage.split("\n");
		for(String line : lines) {
			if(!line.startsWith("REC assert")) {
				continue;
			}
			line = line.trim();
			String[] splittedLine = line.split(" ");
			try {
				String statusString = splittedLine[2];
				String failPoint = splittedLine[3];
				int status = Integer.parseInt(statusString);
				if((status == asseetError || status == assertSuccess) && failPoint.startsWith("@(") && failPoint.endsWith(")")) {
					return new String[] {statusString, failPoint.substring(2, failPoint.length() - 1)};
				}
			}
			catch(Exception e) {
				Utils.fatal(1, "invalid assertion message: " + line);
			}
		}
		return null;
	}

	private int resolveData(int exitStatus, String fileName) {
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
	private static void remoteProcedureCall(String recServerURL, RecRequest params) throws IOException {
		String json = JSON.encode(new JSONRPC(params));
		StringEntity body = new StringEntity(json);

		HttpClient client = HttpClientBuilder.create().build();
		HttpPost request = new HttpPost(recServerURL);
		request.addHeader("Content-type", "application/json");
		request.setEntity(body);
		try {
			HttpResponse response = client.execute(request);
			System.out.println(EntityUtils.toString(response.getEntity()));
		}
		catch(Exception e) {
			Utils.fatal(1, "cannot send to REC due to " + e.getMessage());
		}
	}

	public static void pushRawData(String recServerURL, String type, String location, int data, String authid, RecContext context) {
		try {
			remoteProcedureCall(recServerURL, new RecRequest(type, location, data, authid, context));
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

		private static double jsonRPCVersion = 2.0;
		private static int jsonId = 0;

		public JSONRPC(RecRequest recRequest) {
			this.jsonrpc = Double.toString(jsonRPCVersion);
			this.method = "pushRawData";
			this.id = jsonId;
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