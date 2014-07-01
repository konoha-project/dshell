package dshell.internal.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * represent type parameter of generic type.
 * @author skgchxngsxyz-osx
 *
 */
public class ParametricType extends DSType {
	public ParametricType(String symbol) {
		super(symbol, "java/lang/Object", false);
	}

	@Override
	public boolean isAssignableFrom(DSType targetType) {
		if(!(targetType instanceof ParametricType)) {
			return false;
		}
		ParametricType parametricType = (ParametricType) targetType;
		return this.getTypeName().equals(parametricType.getTypeName());
	}

	public static class ParametricGenericType extends DSType {
		private final String baseTypeName;
		private final List<DSType> parametricTypeList;

		public ParametricGenericType(String baseTypeName, List<DSType> parametricTypeList) {
			super("$ParametricGeneric$", "java/lang/Object", false);
			this.baseTypeName = baseTypeName;
			this.parametricTypeList = parametricTypeList;
		}

		public DSType reifyType(TypePool pool, Map<String, Integer> typeMap, List<DSType> elemenTypeList) {
			List<DSType> typeList = new ArrayList<>(this.parametricTypeList.size());
			for(DSType parametricType : this.parametricTypeList) {
				if(parametricType instanceof ParametricType) {
					typeList.add(elemenTypeList.get(typeMap.get(parametricType.getTypeName())));
				} else if(parametricType instanceof ParametricGenericType) {
					typeList.add(((ParametricGenericType)parametricType).reifyType(pool, typeMap, elemenTypeList));
				} else {
					typeList.add(parametricType);
				}
			}
			return pool.createAndGetReifiedTypeIfUndefined(this.baseTypeName, typeList);
		}
	}
}
