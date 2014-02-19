package dshell.exception;

import java.lang.annotation.Annotation;

import dshell.lib.DerivedFromErrno;

public class RelatedSyscallException extends DShellException {
	private static final long serialVersionUID = -2322285443658141171L;

	private String syscallName;
	private String param;
	private String errno;

	public RelatedSyscallException(String message) {
		super(message);
		this.syscallName = "";
		this.param = "";
		this.errno = "";
	}

	public String getSyscallName() {
		return this.syscallName;
	}

	public String getParam() {
		return this.param;
	}

	public String getErrno() {
		Annotation[] anos = this.getClass().getDeclaredAnnotations();
		if(anos.length == 1 && anos[0] instanceof DerivedFromErrno) {
			return ((DerivedFromErrno)anos[0]).value().name();
		}
		return this.errno;
	}

	public void setSyscallInfo(String[] syscalls) {
		this.syscallName = syscalls[0];
		this.param = syscalls[1];
		this.errno = syscalls[2];
	}
}