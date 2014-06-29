package dshell.internal.type;

/**
 * Represents class type.
 * Class name must be upper case.
 * It contains super class type.
 * @author skgchxngsxyz-osx
 *
 */
public abstract class ClassType extends DSType {
	protected final DSType superType;

	protected ClassType(String className, String internalName, DSType superType, boolean allowExtends) {
		super(className, internalName, allowExtends);
		this.superType = superType;
	}

	public DSType getSuperType() {
		return this.superType;
	}

	@Override
	public boolean isAssignableFrom(DSType targetType) {
		if(!(targetType instanceof ClassType)) {
			return false;
		}
		if(this.getTypeName().equals(targetType.getTypeName())) {
			return true;
		}
		DSType superType = ((ClassType)targetType).getSuperType();
		if(!(superType instanceof RootClassType)) {
			return this.isAssignableFrom(superType);
		}
		return false;
	}
}
