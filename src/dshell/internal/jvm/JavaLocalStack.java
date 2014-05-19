package dshell.internal.jvm;

import org.objectweb.asm.Type;

final class JavaLocalStack {
	public final String   Name;
	public final Class<?> JavaType;
	public final Type     AsmType;
	public final int      Index;
	public JavaLocalStack(int Index, Class<?> JavaType, Type TypeInfo, String Name) {
		this.Index    = Index;
		//System.out.println("** debug add local " + Name + ", " + JavaType);
		this.JavaType = JavaType;
		this.AsmType  = TypeInfo;
		this.Name     = Name;
	}
}