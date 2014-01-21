package dshell.lib;

import java.util.EnumMap;

import dshell.exception.BadFileDescriptorException;
import dshell.exception.BadMessageException;
import dshell.exception.BadStateFileDescriptorException;
import dshell.exception.BrokenPipeException;
import dshell.exception.ConnectionTimeoutException;
import dshell.exception.DeviceNotFoundException;
import dshell.exception.FileExistException;
import dshell.exception.FileTableOverflowException;
import dshell.exception.IllegalSeekException;
import dshell.exception.InappropriateOperateException;
import dshell.exception.InterruptedBySignalException;
import dshell.exception.InvalidArgumentException;
import dshell.exception.IsDirectoryException;
import dshell.exception.NoBufferSpaceException;
import dshell.exception.NoChildException;
import dshell.exception.NoFreeMemoryException;
import dshell.exception.NoFreeSpaceException;
import dshell.exception.NotDirectoryException;
import dshell.exception.NotEmptyDirectoryException;
import dshell.exception.NotPermittedException;
import dshell.exception.NotPermittedOperateException;
import dshell.exception.NotSocketException;
import dshell.exception.ReadOnlyException;
import dshell.exception.RemoteIOException;
import dshell.exception.TemporaryUnavailableException;
import dshell.exception.TooLargeFileException;
import dshell.exception.TooLongMessageException;
import dshell.exception.TooLongNameException;
import dshell.exception.TooManyArgsException;
import dshell.exception.TooManyFileOpenException;
import dshell.exception.TooManyLinkException;
import dshell.exception.TooManyUsersException;
import dshell.exception.UnreachableHostException;
import dshell.exception.UnreachableNetworkException;


public class ExceptionMap {
	private EnumMap<Errno, Class<?>> exceptMap;

	public ExceptionMap() {
		this.exceptMap = new EnumMap<Errno, Class<?>>(Errno.class);
		// put element
		this.addToMap(Errno.SUCCESS			, null);
		this.addToMap(Errno.EPERM			, NotPermittedOperateException.class);
		this.addToMap(Errno.ENOENT			, dshell.exception.FileNotFoundException.class);
		this.addToMap(Errno.ESRCH			, null);
		this.addToMap(Errno.EINTR			, InterruptedBySignalException.class);
		this.addToMap(Errno.EIO				, dshell.exception.IOException.class);
		this.addToMap(Errno.ENXIO			, null);
		this.addToMap(Errno.E2BIG			, TooManyArgsException.class);
		this.addToMap(Errno.ENOEXEC			, null);
		this.addToMap(Errno.EBADF			, BadFileDescriptorException.class);
		this.addToMap(Errno.ECHILD			, NoChildException.class);
		this.addToMap(Errno.EAGAIN			, TemporaryUnavailableException.class);
		this.addToMap(Errno.ENOMEM			, NoFreeMemoryException.class);
		this.addToMap(Errno.EACCES			, NotPermittedException.class);
		this.addToMap(Errno.EFAULT			, null);
		this.addToMap(Errno.ENOTBLK			, null);
		this.addToMap(Errno.EBUSY			, null);
		this.addToMap(Errno.EEXIST			, FileExistException.class);
		this.addToMap(Errno.EXDEV			, null);
		this.addToMap(Errno.ENODEV			, DeviceNotFoundException.class);
		this.addToMap(Errno.ENOTDIR			, NotDirectoryException.class);
		this.addToMap(Errno.EISDIR			, IsDirectoryException.class);
		this.addToMap(Errno.EINVAL			, InvalidArgumentException.class);
		this.addToMap(Errno.ENFILE			, FileTableOverflowException.class);
		this.addToMap(Errno.EMFILE			, TooManyFileOpenException.class);
		this.addToMap(Errno.ENOTTY			, InappropriateOperateException.class);
		this.addToMap(Errno.ETXTBSY			, null);
		this.addToMap(Errno.EFBIG			, TooLargeFileException.class);
		this.addToMap(Errno.ENOSPC			, NoFreeSpaceException.class);
		this.addToMap(Errno.ESPIPE			, IllegalSeekException.class);
		this.addToMap(Errno.EROFS			, ReadOnlyException.class);
		this.addToMap(Errno.EMLINK			, null);
		this.addToMap(Errno.EPIPE			, BrokenPipeException.class);
		this.addToMap(Errno.EDOM			, null);
		this.addToMap(Errno.ERANGE			, null);
		this.addToMap(Errno.EDEADLK			, null);
		this.addToMap(Errno.ENAMETOOLONG	, TooLongNameException.class);
		this.addToMap(Errno.ENOLCK			, null);
		this.addToMap(Errno.ENOSYS			, null);
		this.addToMap(Errno.ENOTEMPTY		, NotEmptyDirectoryException.class);
		this.addToMap(Errno.ELOOP			, TooManyLinkException.class);
		this.addToMap(Errno.EWOULDBLOCK		, this.getExceptionClass(Errno.EAGAIN));//EAGAIN
		this.addToMap(Errno.ENOMSG			, null);
		this.addToMap(Errno.EIDRM			, null);
		this.addToMap(Errno.ECHRNG			, null);
		this.addToMap(Errno.EL2NSYNC		, null);
		this.addToMap(Errno.EL3HLT			, null);
		this.addToMap(Errno.EL3RST			, null);
		this.addToMap(Errno.ELNRNG			, null);
		this.addToMap(Errno.EUNATCH			, null);
		this.addToMap(Errno.ENOCSI			, null);
		this.addToMap(Errno.EL2HLT			, null);
		this.addToMap(Errno.EBADE			, null);
		this.addToMap(Errno.EBADR			, null);
		this.addToMap(Errno.EXFULL			, null);
		this.addToMap(Errno.ENOANO			, null);
		this.addToMap(Errno.EBADRQC			, null);
		this.addToMap(Errno.EBADSLT			, null);
		this.addToMap(Errno.EDEADLOCK		, this.getExceptionClass(Errno.EDEADLK));//EDEADLK
		this.addToMap(Errno.EBFONT			, null);
		this.addToMap(Errno.ENOSTR			, null);
		this.addToMap(Errno.ENODATA			, null);
		this.addToMap(Errno.ETIME			, null);
		this.addToMap(Errno.ENOSR			, null);
		this.addToMap(Errno.ENONET			, null);
		this.addToMap(Errno.ENOPKG			, null);
		this.addToMap(Errno.EREMOTE			, null);
		this.addToMap(Errno.ENOLINK			, null);
		this.addToMap(Errno.EADV			, null);
		this.addToMap(Errno.ESRMNT			, null);
		this.addToMap(Errno.ECOMM			, null);
		this.addToMap(Errno.EPROTO			, null);
		this.addToMap(Errno.EMULTIHOP		, null);
		this.addToMap(Errno.EDOTDOT			, null);
		this.addToMap(Errno.EBADMSG			, BadMessageException.class);
		this.addToMap(Errno.EOVERFLOW		, null);
		this.addToMap(Errno.ENOTUNIQ		, null);
		this.addToMap(Errno.EBADFD			, BadStateFileDescriptorException.class);
		this.addToMap(Errno.EREMCHG			, null);
		this.addToMap(Errno.ELIBACC			, null);
		this.addToMap(Errno.ELIBBAD			, null);
		this.addToMap(Errno.ELIBSCN			, null);
		this.addToMap(Errno.ELIBMAX			, null);
		this.addToMap(Errno.ELIBEXEC		, null);
		this.addToMap(Errno.EILSEQ			, null);
		this.addToMap(Errno.ERESTART		, null);
		this.addToMap(Errno.ESTRPIPE		, null);
		this.addToMap(Errno.EUSERS			, TooManyUsersException.class);
		this.addToMap(Errno.ENOTSOCK		, NotSocketException.class);
		this.addToMap(Errno.EDESTADDRREQ	, null);
		this.addToMap(Errno.EMSGSIZE		, TooLongMessageException.class);
		this.addToMap(Errno.EPROTOTYPE		, null);
		this.addToMap(Errno.ENOPROTOOPT		, null);
		this.addToMap(Errno.EPROTONOSUPPORT	, null);
		this.addToMap(Errno.ESOCKTNOSUPPORT	, null);
		this.addToMap(Errno.EOPNOTSUPP		, null);
		this.addToMap(Errno.EPFNOSUPPORT	, null);
		this.addToMap(Errno.EAFNOSUPPORT	, null);
		this.addToMap(Errno.EADDRINUSE		, null);
		this.addToMap(Errno.EADDRNOTAVAIL	, null);
		this.addToMap(Errno.ENETDOWN		, null);
		this.addToMap(Errno.ENETUNREACH		, UnreachableNetworkException.class);
		this.addToMap(Errno.ENETRESET		, null);
		this.addToMap(Errno.ECONNABORTED	, null);
		this.addToMap(Errno.ECONNRESET		, null);
		this.addToMap(Errno.ENOBUFS			, NoBufferSpaceException.class);
		this.addToMap(Errno.EISCONN			, null);
		this.addToMap(Errno.ENOTCONN		, null);
		this.addToMap(Errno.ESHUTDOWN		, null);
		this.addToMap(Errno.ETOOMANYREFS	, null);
		this.addToMap(Errno.ETIMEDOUT		, ConnectionTimeoutException.class);
		this.addToMap(Errno.ECONNREFUSED	, null);
		this.addToMap(Errno.EHOSTDOWN		, null);
		this.addToMap(Errno.EHOSTUNREACH	, UnreachableHostException.class);
		this.addToMap(Errno.EALREADY		, null);
		this.addToMap(Errno.EINPROGRESS		, null);
		this.addToMap(Errno.ESTALE			, null);
		this.addToMap(Errno.EUCLEAN			, null);
		this.addToMap(Errno.ENOTNAM			, null);
		this.addToMap(Errno.ENAVAIL			, null);
		this.addToMap(Errno.EISNAM			, null);
		this.addToMap(Errno.EREMOTEIO		, RemoteIOException.class);
		this.addToMap(Errno.EDQUOT			, null);
		this.addToMap(Errno.ENOMEDIUM		, null);
		this.addToMap(Errno.EMEDIUMTYPE		, null);
		this.addToMap(Errno.ECANCELED		, null);
		this.addToMap(Errno.ENOKEY			, null);
		this.addToMap(Errno.EKEYEXPIRED		, null);
		this.addToMap(Errno.EKEYREVOKED		, null);
		this.addToMap(Errno.EKEYREJECTED	, null);
	}

	private void addToMap(Errno key, Class<?> value) {
		this.exceptMap.put(key, value);
	}

	public Class<?> getExceptionClass(int errno) {
		return this.exceptMap.get(Errno.values()[errno]);
	}

	public Class<?> getExceptionClass(String errnoString) {
		return this.exceptMap.get(Errno.valueOf(errnoString));
	}

	public Class<?> getExceptionClass(Errno key) {
		return this.exceptMap.get(key);
	}

	public String getErrnoString(int errno) {
		return Errno.values()[errno].toString();
	}
}

enum Errno {
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
	EKEYREJECTED	;
}
