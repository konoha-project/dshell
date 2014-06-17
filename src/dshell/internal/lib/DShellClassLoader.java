package dshell.internal.lib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * used for user defined class loading.
 * @author skgchxngsxyz-osx
 *
 */
public class DShellClassLoader extends ClassLoader {
	/**
	 * if true, dump byte code.
	 */
	private static boolean enableDump = false;
	
	
	/**
	 * fully qualified class name.
	 */
	private String className;

	/**
	 * require java class specification.
	 */
	private byte[] byteCode;
	private boolean initialized = false;

	public DShellClassLoader() {
		super();
	}

	/**
	 * used for child class loader creation.
	 * @param classLoader
	 */
	protected DShellClassLoader(DShellClassLoader classLoader) {
		super(classLoader);
	}

	@Override protected Class<?> findClass(String name) {
		if(!this.initialized) {
			return null;
		}
		this.initialized = false;
		Class<?>  generetedClass = this.defineClass(this.className, this.byteCode, 0, this.byteCode.length);
		this.byteCode = null;
		return generetedClass;
	}

	/**
	 * set byte code and class name.
	 * before class loading, must call it.
	 * @param className
	 * - must be fully qualified class name.
	 * @param byteCode
	 */
	public void setByteCode(String className, byte[] byteCode) {
		this.initialized = true;
		this.className = className;
		this.byteCode = byteCode.clone();
	}

	/**
	 * 
	 * @param className
	 * - must be fully qualified class name.
	 * @param byteCode
	 * @return
	 * - if class loading failed, call System.exit(1).
	 */
	public Class<?> definedAndLoadClass(String className, byte[] byteCode) {
		String binaryName = className;
		if(className.indexOf(".") == -1) {
			binaryName = className.replace('/', '.');
		}
		this.setByteCode(binaryName, byteCode);
		this.dump();
		try {
			return this.loadClass(binaryName);
		} catch (Throwable e) {
			e.printStackTrace();
			Utils.fatal(1, "class loading failed: " + binaryName);
		}
		return null;
	}

	/**
	 * create child class loader.
	 * @return
	 */
	public DShellClassLoader createChild() {
		return new DShellClassLoader(this);
	}

	/**
	 * for debug purpose.
	 */
	private void dump() {
		if(!enableDump) {
			return;
		}
		int index = this.className.lastIndexOf('.');
		String classFileName = this.className.substring(index + 1) + ".class";
		System.err.println("@@@@ Dump ByteCode: " + classFileName + " @@@@");
		FileOutputStream fileOutputStream;
		try {
			fileOutputStream = new FileOutputStream(new File(classFileName));
			fileOutputStream.write(this.byteCode);
			fileOutputStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setDump(boolean enableByteCodeDump) {
		enableDump = enableByteCodeDump;
	}
}
