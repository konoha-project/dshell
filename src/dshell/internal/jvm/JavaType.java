package dshell.internal.jvm;

import libbun.type.BType;

public class JavaType extends BType {
	private final JavaTypeTable typeTable;
	private Class<?> javaClass;

	public JavaType(JavaTypeTable typeTable, Class<?> JType) {
		super(BType.UniqueTypeFlag, JType.getSimpleName(), null);
		this.typeTable = typeTable;
		this.javaClass = JType;
	}

	@Override
	public BType GetSuperType() {
		return this.typeTable.GetBunType(this.javaClass.getSuperclass());
	}
}
