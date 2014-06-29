package dshell.internal.type;

import java.util.Collections;
import java.util.List;

/**
 * Represents generic type (array type or map type).
 * It contains elements type.
 * @author skgchxngsxyz-osx
 *
 */
public class ReifiedType extends ClassType implements GenericType {
	private final List<DSType> elementTypeList;

	ReifiedType(String typeName, GenericBaseType baseType, List<DSType> elementTypeList) {
		super(typeName, baseType.getInternalName(), baseType.superType, false);	//FIXME: super type
		this.elementTypeList = Collections.unmodifiableList(elementTypeList);
	}

	/**
	 * used for primitive array type.
	 * @param typeName
	 * @param internalName
	 * @param superType
	 * @param elementTypeList
	 */
	protected ReifiedType(String typeName, String internalName, DSType superType, List<DSType> elementTypeList) {
		super(typeName, internalName, superType, false);
		this.elementTypeList = Collections.unmodifiableList(elementTypeList);
	}

	@Override
	public List<DSType> getElementTypeList() {
		return this.elementTypeList;
	}

	/**
	 * Represent base type of generic type.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class GenericBaseType extends ClassType {
		GenericBaseType(String className, String internalName, DSType superType, boolean allowExtends) {
			super(className, internalName, superType, allowExtends);
		}
	}

//	public final static class PrimitiveArrayType extends ReifiedType {
//		private final TypePool pool;
//		private String[][] constructorElements;
//		private String[][] methodElements;
//		boolean initConstructor = false;
//
//		private final List<ConstructorHandle> constructorHandleList;
//		private final Map<String, HandleEntry<MethodHandle>> methodEntryMap;
//
//		PrimitiveArrayType(TypePool pool, String internalName, DSType superType, PrimitiveType elementType,
//				String[][] constructorElements,
//				String[][] fieldElements,
//				String[][] methodElements) {
//			super("Array<" + elementType.getTypeName() + ">", internalName, superType, toTypeList(elementType));
//			this.pool = pool;
//			this.constructorElements = constructorElements;
//			this.methodElements = methodElements;
//			this.constructorHandleList = new ArrayList<>(constructorElements.length);
//			this.methodEntryMap = new HashMap<>();
//		}
//
//		private static List<DSType> toTypeList(DSType type) {
//			List<DSType> typeList = new ArrayList<>(1);
//			typeList.add(type);
//			return typeList;
//		}
//
//		protected ConstructorHandle toConstructorHandle(String[] paramTypeNames) {
//			List<DSType> paramTypeList = new ArrayList<>(paramTypeNames.length);
//			for(String typeName : paramTypeNames) {
//				paramTypeList.add(this.pool.parseTypeName(typeName));
//			}
//			return new ConstructorHandle(this, paramTypeList);
//		}
//
//		@Override
//		public ConstructorHandle lookupConstructorHandle(List<DSType> paramTypeList) {
//			if(!this.initConstructor) {
//				this.initConstructor = true;
//				for(String[] e : this.constructorElements) {
//					this.constructorHandleList.add(this.toConstructorHandle(e));
//				}
//				this.constructorElements = null;
//			}
//			for(ConstructorHandle handle : this.constructorHandleList) {
//				final int size = handle.getParamTypeList().size();
//				if(size != paramTypeList.size()) {
//					continue;
//				}
//				int matchCount = 0;
//				for(int i = 0; i < size; i++) {
//					if(!handle.getParamTypeList().get(i).isAssignableFrom(paramTypeList.get(i))) {
//						break;
//					}
//					matchCount++;
//				}
//				if(matchCount == size) {
//					return handle;
//				}
//			}
//			return null;
//		}
//
//		protected MethodHandle toMethodHandle(String[] symbols) {
//			DSType returnType = this.pool.parseTypeName(symbols[1]);
//			int paramSize = symbols.length - 2;
//			List<DSType> paramTypeList = new ArrayList<>(paramSize);
//			for(int i = 0; i < paramSize; i++) {
//				paramTypeList.add(this.pool.parseTypeName(symbols[i + 2]));
//			}
//			return new MethodHandle(symbols[0], this, returnType, paramTypeList);
//		}
//
//		@Override
//		public MethodHandle lookupMethodHandle(String methodName) {
//			HandleEntry<MethodHandle> handleEntry = this.methodEntryMap.get(methodName);
//			if(handleEntry != null) {
//				MethodHandle handle = handleEntry.handle;
//				if(handle == null) {
//					handle = this.toMethodHandle(this.methodElements[handleEntry.index]);
//					handleEntry.handle = handle;
//				}
//				return handle;
//			}
//			return null;
//		}
//	}
}

