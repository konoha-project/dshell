package dshell.lib;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dshell.util.Utils;

public interface CauseInferencer {
	public ArrayList<String> doInference(SubProc proc);
}

class CauseInferencer_ltrace implements CauseInferencer {
	private final String mainName = "__libc_start_main";
	private final String unfinished = "<unfinished ...>";
	private final Pattern linePattern = Pattern.compile("(^[1-9][0-9]*)( +)([0-9]+:[0-9]+:[0-9]+)( +)(.+)");
	private final Pattern syscallPattern = Pattern.compile("(SYS_)(.+)(\\(.*\\))( +)(=)( +)(.+)");
	private final Pattern unfinishedFuncPattern = Pattern.compile("(.+)(\\(.*)( +)(" + unfinished + ")");
	private final Pattern funcPattern = Pattern.compile("(.+)(\\(.*\\))( +)(=)( +)(.+)");
	private final Pattern resumedPattern = Pattern.compile("(<.+)( +)(.+)( +)(resumed>.+\\))( +)(=)( +)(.+)");

	private static enum IgnoreFunction {
		setlocale,
		dcgettext,
		error;

		public static boolean match(String funcName) {
			IgnoreFunction[] values = IgnoreFunction.values();
			for(IgnoreFunction value : values) {
				if(value.name().equals(funcName)) {
					return true;
				}
			}
			return false;
		}
	}

	public ArrayList<String> doInference(SubProc proc) {
		String logFilePath = proc.getLogFilePath();
		ArrayList<String[]> lineList = new ArrayList<String[]>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(logFilePath));
			String line;
			boolean foundMain = false;
			while((line = br.readLine()) != null) {
				Matcher m = linePattern.matcher(line);
				if(!m.find()) {
					Utils.fatal(1, "not match: " + line);
				}
				if(foundMain || m.group(5).startsWith(mainName)) {
					foundMain = true;
					lineList.add(new String[] {m.group(1), m.group(3), m.group(5)});
				}
			}
			br.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		FunctionContext topLevelContext = createTopLevelFuncContext(lineList);
		proc.retValue = Integer.parseInt(topLevelContext.getRetValue());
		return this.findCauseInfo(topLevelContext);
	}

	private FunctionContext createTopLevelFuncContext(final ArrayList<String[]> lineList) {
		if(lineList.size() == 0) {
			Utils.fatal(1, "empty lineList");
		}
		String[] parsedInfo = lineList.get(0);
		int pid = Integer.parseInt(parsedInfo[0]);
		FunctionContext context = new FunctionContext(mainName, pid, parsedInfo[1], null);
		int index = 1;
		do {
			index = this.createFuncContext(lineList, context, index);
		} while(index != -1 && context.getRetValue() == null);
		return context;
	}

	private int createFuncContext(final ArrayList<String[]> lineList, final FunctionContext parentContext, final int index) {
		if(index >= lineList.size()) {
			Utils.fatal(1, "index = " + index + ", size = " + lineList.size());
		}
		String[] parsedInfo = lineList.get(index);
		String calledFunc = parsedInfo[2];
		if(calledFunc.startsWith("SYS_")) {
			parentContext.appendFuncContext(this.matchSyscall(parsedInfo));
			return index + 1;
		}
		if(calledFunc.startsWith("--")) {
			return index + 1;
		}
		if(calledFunc.startsWith("++")) {
			Utils.fatal(1, "match: " + calledFunc);
		}
		if(!calledFunc.startsWith("<")) {
			if(calledFunc.startsWith("exit")) {
				FunctionContext exitContext = this.matchUnfinishedFunc(parsedInfo);
				if(parentContext.funcName.equals(mainName)) {
					parentContext.setRetValue(exitContext.param);
					return index;
				}
				Utils.fatal(1, "invalid funcname: " + parentContext.funcName + ", " + calledFunc);
			}
			if(calledFunc.endsWith(unfinished)) {
				FunctionContext unfinishedContext = this.matchUnfinishedFunc(parsedInfo);
				int localIndex = index + 1;
				do {
					localIndex = this.createFuncContext(lineList, unfinishedContext, localIndex);
				} while(localIndex != -1 && unfinishedContext.getRetValue() == null);
				parentContext.appendFuncContext(unfinishedContext);
				return localIndex;
			}
			parentContext.appendFuncContext(this.matchFunc(parsedInfo));
			return index + 1;
		}
		else {
			Matcher matcher = resumedPattern.matcher(calledFunc);
			if(!matcher.find()) {
				Utils.fatal(1, "not match: " + calledFunc);
			}
			if(matcher.group(3).equals(parentContext.funcName)) {
				String ret = matcher.group(9);
				parentContext.setRetValue(ret);
				return index + 1;
			}
		}
		Utils.fatal(1, "not match: " + calledFunc);
		return -1;
	}

	private SyscallContext matchSyscall(String[] parsedInfo) {
		int pid = Integer.parseInt(parsedInfo[0]);
		String time = parsedInfo[1];
		Matcher matcher = syscallPattern.matcher(parsedInfo[2]);
		if(!matcher.find()) {
			Utils.fatal(1, "not match: " + parsedInfo[2]);
		}
		String syscallName = matcher.group(2);
		String param = matcher.group(3);
		String actualParam = param.substring(1, param.length() - 1);
		SyscallContext context = new SyscallContext(syscallName, pid, time, actualParam);
		context.setRetValue(matcher.group(7));
		return context;
	}

	private FunctionContext matchUnfinishedFunc(String[] parsedInfo) {
		int pid = Integer.parseInt(parsedInfo[0]);
		String time = parsedInfo[1];
		Matcher matcher = unfinishedFuncPattern.matcher(parsedInfo[2]);
		if(!matcher.find()) {
			Utils.fatal(1, "not match: " + parsedInfo[2]);
		}
		String funcName = matcher.group(1);
		String param = matcher.group(2).substring(1);
		return new FunctionContext(funcName, pid, time, param);
	}

	private FunctionContext matchFunc(String[] parsedInfo) {
		int pid = Integer.parseInt(parsedInfo[0]);
		String time = parsedInfo[1];
		Matcher matcher = funcPattern.matcher(parsedInfo[2]);
		if(!matcher.find()) {
			Utils.fatal(1, "not match: " + parsedInfo[2]);
		}
		String funcName = matcher.group(1);
		String param = matcher.group(2);
		String actualParam = param.substring(1, param.length() - 1);
		String ret = matcher.group(6);
		FunctionContext context = new FunctionContext(funcName, pid, time, actualParam);
		context.setRetValue(ret);
		return context;
	}

	private ArrayList<String> findCauseInfo(FunctionContext context) {
		ArrayList<String> causeInfo = new ArrayList<String>();
		if(context.getRetValue().equals("0")) {
			causeInfo.add("empty");
			causeInfo.add("empty");
			causeInfo.add(Errno.SUCCESS.name());
		}
		else {
			SyscallContext causedContext = this.findCausedContext(context);
			if(causedContext == null) {
				causeInfo.add("empty");
				causeInfo.add("empty");
				causeInfo.add(Errno.LAST_ELEMENT.name());
			}
			else {
				causeInfo.add(causedContext.funcName);
				causeInfo.add(causedContext.param);
				String errnoString = Errno.toErrrno((int)(-1 * causedContext.getExitStatus())).name();
				causeInfo.add(errnoString);
			}
		}
		return causeInfo;
	}

	private SyscallContext findCausedContext(FunctionContext parentContext) {
		int size = parentContext.getFuncContextList().size();
		for(int i = size - 1; i > -1; i--) {
			FuncContextStub localContext = parentContext.getFuncContext(i);
			if(localContext.failed) {
				if(localContext instanceof SyscallContext) {
					return (SyscallContext)localContext;
				}
				if(localContext instanceof FunctionContext && !IgnoreFunction.match(localContext.funcName)) {
					return this.findCausedContext((FunctionContext)localContext);
				}
			}
		}
		return null;
	}
}

class FuncContextStub {
	public final String funcName;
	public final int pid;
	public final String calledTime;
	public final String param;
	private String retValue = null;
	public boolean failed = false;

	public FuncContextStub(String funcName, int pid, String calledTime, String param) {
		this.funcName = funcName;
		this.pid = pid;
		this.calledTime = calledTime;
		this.param = param;
	}

	public void setRetValue(String retValue) {
		this.retValue = retValue;
	}

	public String getRetValue() {
		return this.retValue;
	}

	@Override
	public String toString() {
		return this.funcName;
	}
}

class FunctionContext extends FuncContextStub {
	private final ArrayList<FuncContextStub> funcContextList;
	
	public FunctionContext(String funcName, int pid, String calledTime, String param) {
		super(funcName, pid, calledTime, param);
		this.funcContextList = new ArrayList<FuncContextStub>();
	}

	public void appendFuncContext(FuncContextStub funcContext) {
		this.funcContextList.add(funcContext);
		if(!this.failed && funcContext.failed) {
			this.failed = true;
		}
	}

	public ArrayList<FuncContextStub> getFuncContextList() {
		return this.funcContextList;
	}

	public FuncContextStub getFuncContext(int index) {
		return this.funcContextList.get(index);
	}
}

class SyscallContext extends FuncContextStub {
	public SyscallContext(String funcName, int pid, String calledTime, String param) {
		super(funcName, pid, calledTime, param);
	}

	@Override
	public void setRetValue(String retValue) {
		long value = 0;
		if(retValue.startsWith("0x")) {
			value = Long.parseLong(retValue.substring(2), 16);
		}
		else {
			if(retValue.startsWith("-")) {
				this.failed = true;
			}
			value = Long.parseLong(retValue);
		}
		super.setRetValue("" + value);
	}

	public String getSyscallName() {
		return this.funcName;
	}

	public long getExitStatus() {
		return Long.parseLong(super.getRetValue());
	}

	@Override
	public String toString() {
		String suffix = "";
		if(this.failed) {
			suffix = " :" + this.getExitStatus();
		}
		return "SYS_" + super.toString() + suffix;
	}
}