package dshell.internal.lib;

import static dshell.internal.lib.TaskOption.Behavior.sender;
import static dshell.internal.lib.TaskOption.Behavior.throwable;
import static dshell.internal.lib.TaskOption.Behavior.timeout;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import dshell.lang.DShellException;
import dshell.lang.Errno;
import dshell.lang.MultipleException;

public class ShellExceptionBuilder {
	public static DShellException getException(final PseudoProcess[] procs, final TaskOption option, final ByteArrayOutputStream[] eachBuffers) {
		if(option.is(sender) || !option.is(throwable) || option.is(timeout)) {
			return DShellException.createNullException("");
		}
		ArrayList<DShellException> exceptionList = new ArrayList<DShellException>();
		for(int i = 0; i < procs.length; i++) {
			PseudoProcess proc = procs[i];
			String errorMessage = eachBuffers[i].toString();
			createAndAddException(exceptionList, proc, errorMessage);
		}
		int size = exceptionList.size();
		if(size == 1) {
			if(!(exceptionList.get(0) instanceof DShellException.NullException)) {
				return exceptionList.get(0);
			}
		}
		else if(size > 1) {
			int count = 0;
			for(DShellException exception : exceptionList) {
				if(!(exception instanceof DShellException.NullException)) {
					count++;
				}
			}
			if(count != 0) {
				return new MultipleException("", exceptionList.toArray(new DShellException[size]));
			}
		}
		return DShellException.createNullException("");
	}

	private static void createAndAddException(ArrayList<DShellException> exceptionList, PseudoProcess proc, String errorMessage) {
		CauseInferencer inferencer = CauseInferencer_ltrace.getInferencer();
		String message = proc.getCmdName();
		if(proc.isTraced() || proc.getRet() != 0) {
			DShellException exception;
			if(proc.isTraced()) {
				ArrayList<String> infoList = inferencer.doInference((SubProc)proc);
				exception = createException(message, infoList.toArray(new String[infoList.size()]));
			}
			else {
				exception = new DShellException(message);
			}
			exception.setCommand(message);
			exception.setErrorMessage(errorMessage);
			exceptionList.add(exception);
		}
		else {
			exceptionList.add(DShellException.createNullException(message));
		}
		if(proc instanceof SubProc) {
			((SubProc)proc).deleteLogFile();
		}
	}

	private static DShellException createException(String message, String[] causeInfo) {
		// syscall: syscallName: 0, param: 1, errno: 2
		Class<?>[] types = {String.class};
		Object[] args = {message};
		String errnoString = causeInfo[2];
		if(Errno.SUCCESS.match(errnoString)) {
			return DShellException.createNullException(message);
		}
		if(Errno.LAST_ELEMENT.match(errnoString)) {
			return new DShellException(message);
		}
		Class<?> exceptionClass = Errno.getExceptionClass(errnoString);
		try {
			Constructor<?> constructor = exceptionClass.getConstructor(types);
			Errno.DerivedFromErrnoException exception = (Errno.DerivedFromErrnoException) constructor.newInstance(args);
			exception.setSyscallInfo(causeInfo);
			return exception;
		}
		catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		catch (SecurityException e) {
			e.printStackTrace();
		}
		catch (InstantiationException e) {
			e.printStackTrace();
		}
		catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		Utils.fatal(1, "Creating Exception failed");
		return null;	// unreachable 
	}
}
