package dshell.exception;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

import dshell.lib.Utils;


@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@interface DerivedFromErrno {
	Errno value();
}

public enum Errno {
	SUCCESS			,
	EPERM			,
	ENOENT			,
	ESRCH			,
	EINTR			,
	EIO				,
	ENXIO			,
	E2BIG			,
	ENOEXEC			,
	EBADF			,
	ECHILD			,
	EAGAIN			,
	ENOMEM			,
	EACCES			,
	EFAULT			,
	ENOTBLK			,
	EBUSY			,
	EEXIST			,
	EXDEV			,
	ENODEV			,
	ENOTDIR			,
	EISDIR			,
	EINVAL			,
	ENFILE			,
	EMFILE			,
	ENOTTY			,
	ETXTBSY			,
	EFBIG			,
	ENOSPC			,
	ESPIPE			,
	EROFS			,
	EMLINK			,
	EPIPE			,
	EDOM			,
	ERANGE			,
	EDEADLK			,
	ENAMETOOLONG	,
	ENOLCK			,
	ENOSYS			,
	ENOTEMPTY		,
	ELOOP			,
	EWOULDBLOCK		,//EAGAIN
	ENOMSG			,
	EIDRM			,
	ECHRNG			,
	EL2NSYNC		,
	EL3HLT			,
	EL3RST			,
	ELNRNG			,
	EUNATCH			,
	ENOCSI			,
	EL2HLT			,
	EBADE			,
	EBADR			,
	EXFULL			,
	ENOANO			,
	EBADRQC			,
	EBADSLT			,
	EDEADLOCK		,//EDEADLK
	EBFONT			,
	ENOSTR			,
	ENODATA			,
	ETIME			,
	ENOSR			,
	ENONET			,
	ENOPKG			,
	EREMOTE			,
	ENOLINK			,
	EADV			,
	ESRMNT			,
	ECOMM			,
	EPROTO			,
	EMULTIHOP		,
	EDOTDOT			,
	EBADMSG			,
	EOVERFLOW		,
	ENOTUNIQ		,
	EBADFD			,
	EREMCHG			,
	ELIBACC			,
	ELIBBAD			,
	ELIBSCN			,
	ELIBMAX			,
	ELIBEXEC		,
	EILSEQ			,
	ERESTART		,
	ESTRPIPE		,
	EUSERS			,
	ENOTSOCK		,
	EDESTADDRREQ	,
	EMSGSIZE		,
	EPROTOTYPE		,
	ENOPROTOOPT		,
	EPROTONOSUPPORT	,
	ESOCKTNOSUPPORT	,
	EOPNOTSUPP		,
	EPFNOSUPPORT	,
	EAFNOSUPPORT	,
	EADDRINUSE		,
	EADDRNOTAVAIL	,
	ENETDOWN		,
	ENETUNREACH		,
	ENETRESET		,
	ECONNABORTED	,
	ECONNRESET		,
	ENOBUFS			,
	EISCONN			,
	ENOTCONN		,
	ESHUTDOWN		,
	ETOOMANYREFS	,
	ETIMEDOUT		,
	ECONNREFUSED	,
	EHOSTDOWN		,
	EHOSTUNREACH	,
	EALREADY		,
	EINPROGRESS		,
	ESTALE			,
	EUCLEAN			,
	ENOTNAM			,
	ENAVAIL			,
	EISNAM			,
	EREMOTEIO		,
	EDQUOT			,
	ENOMEDIUM		,
	EMEDIUMTYPE		,
	ECANCELED		,
	ENOKEY			,
	EKEYEXPIRED		,
	EKEYREVOKED		,
	EKEYREJECTED	,
	LAST_ELEMENT	;

	private final static EnumMap<Errno, Class<?>> exceptClassMap = getClassMap();
	public static Errno toErrrno(int errno) {
		if(errno <= 0 || errno >= Errno.LAST_ELEMENT.ordinal()) {
			Utils.fatal(1, "invalid errno: " + errno);
		}
		return Errno.values()[errno];
	}

	public boolean match(String errnoString) {
		return this.name().equals(errnoString);
	}

	public boolean match(int errno) {
		return this.ordinal() == errno;
	}

	private static EnumMap<Errno, Class<?>> getClassMap() {
		EnumMap<Errno, Class<?>> exceptMap = 
				new EnumMap<Errno, Class<?>>(Errno.class);
		Class<?>[] classes = Errno.class.getClasses();
		for(Class<?> exceptionClass : classes) {
			Annotation[] anos = exceptionClass.getDeclaredAnnotations();
			if(anos.length == 1 && anos[0] instanceof DerivedFromErrno) {
				Errno key = ((DerivedFromErrno)anos[0]).value();
				if(!exceptMap.containsKey(key)) {
					exceptMap.put(key, exceptionClass);
					continue;
				}
			}
		}
		exceptMap.put(Errno.EWOULDBLOCK, getFromMap(exceptMap, Errno.EAGAIN));
		exceptMap.put(Errno.EDEADLOCK, getFromMap(exceptMap, Errno.EDEADLK));
		return exceptMap;
	}

	private static Class<?> getFromMap(EnumMap<Errno, Class<?>> excepClassMap, Errno key) {
		if(key == Errno.SUCCESS || key == Errno.LAST_ELEMENT) {
			Utils.fatal(1, "inavlid errno: " + key.name());
		}
		if(excepClassMap.containsKey(key)) {
			Class<?> exceptionClass = excepClassMap.get(key);
			if(exceptionClass != null) {
				return excepClassMap.get(key);
			}
		}
		return Errno.UnimplementedErrnoException.class;
	}

	public static Class<?> getExceptionClass(int errno) {
		return getFromMap(exceptClassMap, Errno.toErrrno(errno));
	}

	public static Class<?> getExceptionClass(String errnoString) {
		return getFromMap(exceptClassMap, Errno.valueOf(errnoString));
	}

	public static Class<?> getExceptionClass(Errno key) {
		return getFromMap(exceptClassMap, key);
	}

	public static ArrayList<String> getUnsupportedErrnoList() {
		ArrayList<String> errnoList = new ArrayList<String>();
		Errno[] values = Errno.values();
		for(Errno value : values) {
			if(value == Errno.SUCCESS || value == Errno.LAST_ELEMENT) {
				continue;
			}
			if(!exceptClassMap.containsKey(value)) {
				errnoList.add(value.name());
			}
		}
		return errnoList;
	}

	public static ArrayList<Class<?>> getExceptionClassList() {
		ArrayList<Class<?>> classList = new ArrayList<Class<?>>();
		for(Map.Entry<Errno, Class<?>> entry : exceptClassMap.entrySet()) {
			classList.add(entry.getValue());
		}
		return classList;
	}

	// base class
	public static class DerivedFromErrnoException extends DShellException {
		private static final long serialVersionUID = -2322285443658141171L;

		private String syscallName;
		private String param;
		private String errno;

		public DerivedFromErrnoException() {
			this("");
		}

		public DerivedFromErrnoException(String message) {
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

	// temporary class. future may be removed
	public static class UnimplementedErrnoException extends DerivedFromErrnoException {
		private static final long serialVersionUID = 3957045480645559921L;

		public UnimplementedErrnoException(String message) {
			super(message);
		}
		
		@Override
		public String toString() {
			return super.toString() + " :" + this.getErrno() + " has not supported yet";
		}
	}

	// Derived from Errno
	@DerivedFromErrno(value = Errno.EDOM)
	public static class ArgumentOutOfDomainException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -2231772718959423178L;

		public ArgumentOutOfDomainException(String message) {
			super(message);
		}
		public ArgumentOutOfDomainException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EFAULT)
	public static class BadAddressException extends DerivedFromErrnoException {
		private static final long serialVersionUID = 5757398124745842354L;

		public BadAddressException(String message) {
			super(message);
		}
		public BadAddressException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EBADF)
	public static class BadFileDescriptorException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -7496151185611802517L;

		public BadFileDescriptorException(String message) {
			super(message);
		}
		public BadFileDescriptorException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EBADMSG)
	public static class BadMessageException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -844920617505654673L;

		public BadMessageException(String message) {
			super(message);
		}
		public BadMessageException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EBADFD)
	public static class BadStateFileDescriptorException extends DerivedFromErrnoException {
		private static final long serialVersionUID = 1323523835328835128L;

		public BadStateFileDescriptorException(String message) {
			super(message);
		}
		public BadStateFileDescriptorException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EPIPE)
	public static class BrokenPipeException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -5163673987337012958L;

		public BrokenPipeException(String message) {
			super(message);
		}
		public BrokenPipeException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EBUSY)
	public static class BusyResourceException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -398174827082926739L;

		public BusyResourceException(String message) {
			super(message);
		}
		public BusyResourceException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ETXTBSY)
	public static class BusyTextFileException extends DerivedFromErrnoException {
		private static final long serialVersionUID = 3984628712073903229L;

		public BusyTextFileException(String message) {
			super(message);
		}
		public BusyTextFileException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ECONNREFUSED)
	public static class ConnectionRefusedException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -5631944596016935849L;

		public ConnectionRefusedException(String message) {
			super(message);
		}
		public ConnectionRefusedException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ETIMEDOUT)
	public static class ConnectionTimeoutException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -783718463247817643L;

		public ConnectionTimeoutException(String message) {
			super(message);
		}
		public ConnectionTimeoutException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EXDEV)
	public static class CrossDeviceLinkException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -2542368498065138082L;

		public CrossDeviceLinkException(String message) {
			super(message);
		}
		public CrossDeviceLinkException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EDEADLK)
	public static class DeadLockException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -2664225849449332494L;

		public DeadLockException(String message) {
			super(message);
		}
		public DeadLockException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ENODEV)
	public static class DeviceNotFoundException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -2089536947016513934L;

		public DeviceNotFoundException(String message) {
			super(message);
		}
		public DeviceNotFoundException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ENOEXEC)
	public static class ExecutionFormatException extends DerivedFromErrnoException {
		private static final long serialVersionUID = 6039887131382912453L;

		public ExecutionFormatException(String message) {
			super(message);
		}
		public ExecutionFormatException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EEXIST)
	public static class FileExistException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -1147889522086285307L;

		public FileExistException(String message) {
			super(message);
		}
		public FileExistException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ENOENT)
	public static class FileNotFoundException extends DerivedFromErrnoException {
		private static final long serialVersionUID = 6142467605943464028L;

		public FileNotFoundException(String message) {
			super(message);
		}
		public FileNotFoundException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ENFILE)
	public static class FileTableOverflowException extends DerivedFromErrnoException {
		private static final long serialVersionUID = 3318452069127891085L;

		public FileTableOverflowException(String message) {
			super(message);
		}
		public FileTableOverflowException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EIO)
	public static class IOException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -2925711332327816255L;

		public IOException(String message) {
			super(message);
		}
		public IOException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ESPIPE)
	public static class IllegalSeekException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -5170190658505999982L;

		public IllegalSeekException(String message) {
			super(message);
		}
		public IllegalSeekException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ENOTTY)
	public static class InappropriateOperateException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -9048472347245941031L;

		public InappropriateOperateException(String message) {
			super(message);
		}
		public InappropriateOperateException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EINTR)
	public static class InterruptedBySignalException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -2966050458509865009L;

		public InterruptedBySignalException(String message) {
			super(message);
		}
		public InterruptedBySignalException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EINVAL)
	public static class InvalidArgumentException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -8436987830377221886L;

		public InvalidArgumentException(String message) {
			super(message);
		}
		public InvalidArgumentException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EISDIR)
	public static class IsDirectoryException extends DerivedFromErrnoException {
		private static final long serialVersionUID = 162687839416126743L;

		public IsDirectoryException(String message) {
			super(message);
		}
		public IsDirectoryException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ENOBUFS)
	public static class NoBufferSpaceException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -5330482173311853395L;

		public NoBufferSpaceException(String message) {
			super(message);
		}
		public NoBufferSpaceException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ECHILD)
	public static class NoChildException extends DerivedFromErrnoException {
		private static final long serialVersionUID = 7148181168710617339L;

		public NoChildException(String message) {
			super(message);
		}
		public NoChildException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ENOMEM)
	public static class NoFreeMemoryException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -195959244637562160L;

		public NoFreeMemoryException(String message) {
			super(message);
		}
		public NoFreeMemoryException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ENOSPC)
	public static class NoFreeSpaceException extends DerivedFromErrnoException {
		private static final long serialVersionUID = 8768541609625410569L;

		public NoFreeSpaceException(String message) {
			super(message);
		}
		public NoFreeSpaceException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ENOTBLK)
	public static class NotBlockDeviceException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -1664763329262707450L;

		public NotBlockDeviceException(String message) {
			super(message);
		}
		public NotBlockDeviceException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ENOTDIR)
	public static class NotDirectoryException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -8787884746109654469L;

		public NotDirectoryException(String message) {
			super(message);
		}
		public NotDirectoryException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ENOTEMPTY)
	public static class NotEmptyDirectoryException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -8881686779135583683L;

		public NotEmptyDirectoryException(String message) {
			super(message);
		}
		public NotEmptyDirectoryException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ENXIO)
	public static class NotExistIOException extends DerivedFromErrnoException {
		private static final long serialVersionUID = 1645567429022447252L;

		public NotExistIOException(String message) {
			super(message);
		}
	}


	@DerivedFromErrno(value = Errno.EACCES)
	public static class NotPermittedException extends DerivedFromErrnoException {
		private static final long serialVersionUID = 1796678881928391383L;

		public NotPermittedException(String message) {
			super(message);
		}
		public NotPermittedException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EPERM)
	public static class NotPermittedOperateException extends DerivedFromErrnoException {
		private static final long serialVersionUID = 2627959641734763573L;

		public NotPermittedOperateException(String message) {
			super(message);
		}
		public NotPermittedOperateException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ENOTSOCK)
	public static class NotSocketException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -3934984088546370161L;

		public NotSocketException(String message) {
			super(message);
		}
		public NotSocketException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ESRCH)
	public static class ProcessNotFoundException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -3992933727935155963L;

		public ProcessNotFoundException(String message) {
			super(message);
		}
		public ProcessNotFoundException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EROFS)
	public static class ReadOnlyException extends DerivedFromErrnoException {
		private static final long serialVersionUID = 8332949929528954627L;

		public ReadOnlyException(String message) {
			super(message);
		}
		public ReadOnlyException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EREMOTEIO)
	public static class RemoteIOException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -539582706711746246L;

		public RemoteIOException(String message) {
			super(message);
		}
		public RemoteIOException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EAGAIN)
	public static class TemporaryUnavailableException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -260611456289777692L;

		public TemporaryUnavailableException(String message) {
			super(message);
		}
		public TemporaryUnavailableException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EFBIG)
	public static class TooLargeFileException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -7966116738015921365L;

		public TooLargeFileException(String message) {
			super(message);
		}
		public TooLargeFileException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EMSGSIZE)
	public static class TooLongMessageException extends DerivedFromErrnoException {
		private static final long serialVersionUID = 8529989415091772526L;

		public TooLongMessageException(String message) {
			super(message);
		}
		public TooLongMessageException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ENAMETOOLONG)
	public static class TooLongNameException extends DerivedFromErrnoException {
		private static final long serialVersionUID = 2424365446992735197L;

		public TooLongNameException(String message) {
			super(message);
		}
		public TooLongNameException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.E2BIG)
	public static class TooManyArgsException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -9207906526678278894L;

		public TooManyArgsException(String message) {
			super(message);
		}
		public TooManyArgsException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ELOOP)
	public static class TooManyEncounteredLinkException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -5707416057493927952L;

		public TooManyEncounteredLinkException(String message) {
			super(message);
		}
		public TooManyEncounteredLinkException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EMFILE)
	public static class TooManyFileOpenException extends DerivedFromErrnoException {
		private static final long serialVersionUID = 9282872870301020L;

		public TooManyFileOpenException(String message) {
			super(message);
		}
		public TooManyFileOpenException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EMLINK)
	public static class TooManyLinkException extends DerivedFromErrnoException {
		private static final long serialVersionUID = 8752586840100868249L;

		public TooManyLinkException(String message) {
			super(message);
		}
		public TooManyLinkException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EUSERS)
	public static class TooManyUsersException extends DerivedFromErrnoException {
		private static final long serialVersionUID = -3444647688137490451L;

		public TooManyUsersException(String message) {
			super(message);
		}
		public TooManyUsersException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ENOLCK)
	public static class UnavailableLockException extends DerivedFromErrnoException {
		private static final long serialVersionUID = 8589971637706803734L;

		public UnavailableLockException(String message) {
			super(message);
		}
		public UnavailableLockException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.EHOSTUNREACH)
	public static class UnreachableHostException extends DerivedFromErrnoException {
		private static final long serialVersionUID = 1597304418977336685L;

		public UnreachableHostException(String message) {
			super(message);
		}
		public UnreachableHostException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ENETUNREACH)
	public static class UnreachableNetworkException extends DerivedFromErrnoException {
		private static final long serialVersionUID = 7582354315727320975L;

		public UnreachableNetworkException(String message) {
			super(message);
		}
		public UnreachableNetworkException() {
			super();
		}
	}


	@DerivedFromErrno(value = Errno.ERANGE)
	public static class UnrepresentableResultException extends DerivedFromErrnoException {
		private static final long serialVersionUID = 8753352643952859835L;

		public UnrepresentableResultException(String message) {
			super(message);
		}
		public UnrepresentableResultException() {
			super();
		}
	}
}
