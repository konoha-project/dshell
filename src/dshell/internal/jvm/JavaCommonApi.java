package dshell.internal.jvm;

import libbun.util.BArray;
import libbun.util.BunMap;
import libbun.util.SoftwareFault;
import libbun.util.BBooleanArray;
import libbun.util.BFloatArray;
import libbun.util.BIntArray;

public class JavaCommonApi {

	public final static void Assert(boolean x, String Location) {
		if(!x) {
			Exception e = new SoftwareFault("failed: " + Location);
			e.printStackTrace();
			System.err.println("REC assert 0 @" + Location);
			System.exit(1);
		}
		//		else {
		//			System.err.println("REC assert 1 @" + Location);
		//		}
	}

	public final static void Print(String o) {
		System.out.print(o);
	}

	public final static void PrintLine(String o) {
		System.out.println(o);
	}

	//	// converter
	//	asm macro _ "zen/codegen/jvm/JavaCommonApi.IntToFloat($[0])" : Func<int,float>;
	//	asm macro _ "zen/codegen/jvm/JavaCommonApi.FloatToInt($[0])" : Func<float,int>;
	//	asm macro _ "zen/codegen/jvm/JavaCommonApi.ToString($[0])" : Func<boolean,String>;
	//	asm macro _ "zen/codegen/jvm/JavaCommonApi.ToString($[0])" : Func<int,String>;
	//	asm macro _ "zen/codegen/jvm/JavaCommonApi.ToString($[0])" : Func<float,String>;
	//

	public final static long FloatToInt(double x) {
		return Math.round(x);
	}

	public final static double IntToFloat(long x) {
		return x;
	}

	public final static String ToString(Object x) {
		if(x == null) {
			return "null";
		}
		return x.toString();
	}

	public final static String ToString(boolean x) {
		return String.valueOf(x);
	}

	public final static String ToString(long x) {
		return String.valueOf(x);
	}

	public final static String ToString(double x) {
		return String.valueOf(x);
	}

	//	// Array
	//	asm macro size "zen/codegen/jvm/JavaCommonApi.ArraySize($[0])" : Func<??[],int>;
	//	asm macro clear "zen/codegen/jvm/JavaCommonApi.ArrayClear($[0],$[1])" : Func<??[],int,void>;
	//	asm macro add "zen/codegen/jvm/JavaCommonApi.ArrayAdd($[0],$[1])" : Func<??[],??,void>;
	//	asm macro add "zen/codegen/jvm/JavaCommonApi.ArrayInsert($[0],$[1],$[2])" : Func<??[],int,??,void>;


	public final static long StringSize(String x) {
		return x.length();
	}

	public final static String SubString(String x, long y) {
		return x.substring((int)y);
	}

	public final static String SubString(String x, long y, long z) {
		return x.substring((int)y, (int)z);
	}

	public final static long IndexOf(String x, String y) {
		return x.indexOf(y);
	}

	public final static long IndexOf(String x, String y, long z) {
		return x.indexOf(y, (int)z);
	}

	public final static boolean Equals(String x, String z) {
		return x.equals(z);
	}

	public final static boolean StartsWith(String x, String z) {
		return x.startsWith(z);
	}

	public final static boolean EndsWith(String x, String z) {
		return x.endsWith(z);
	}

	// ObjectArray
	public final static <T> long ObjectArraySize(BArray<T> x) {
		return x.size();
	}

	public final static <T> void ObjectArrayClear(BArray<T> x, long y) {
		x.clear((int) y);
	}

	public final static <T> void ObjectArrayAdd(BArray<T> x, T y) {
		x.add(y);
	}

	public final static <T> void ObjectArrayInsert(BArray<T> x, long y, T z) {
		x.add((int) y, z);
	}

	public final static <T> String ObjectArrayToString(BArray<T> x) {
		return x.toString();
	}
	// BooleanArray
	public final static long BooleanArraySize(BBooleanArray x) {
		return x.Size();
	}

	public final static void BooleanArrayClear(BBooleanArray x, long y) {
		x.Clear(y);
	}

	public final static <T> void BooleanArrayAdd(BBooleanArray x, boolean y) {
		x.Add(y);
	}

	public final static <T> void BooleanArrayInsert(BBooleanArray x, long y, boolean z) {
		x.Insert(y, z);
	}

	public final static String BooleanArrayToString(BBooleanArray x) {
		return x.toString();
	}
	// IntArray
	public final static long IntArraySize(BIntArray x) {
		return x.Size();
	}

	public final static void IntArrayClear(BIntArray x, long y) {
		x.Clear(y);
	}

	public final static <T> void IntArrayAdd(BIntArray x, long y) {
		x.Add(y);
	}

	public final static <T> void IntArrayInsert(BIntArray x, long y, long z) {
		x.Insert(y, z);
	}

	public final static String IntArrayToString(BIntArray x) {
		return x.toString();
	}
	// FloatArray
	public final static long FloatArraySize(BFloatArray x) {
		return x.Size();
	}

	public final static void FloatArrayClear(BFloatArray x, long y) {
		x.Clear(y);
	}

	public final static <T> void FloatArrayAdd(BFloatArray x, double y) {
		x.Add(y);
	}

	public final static <T> void FloatArrayInsert(BFloatArray x, long y, double z) {
		x.Insert(y, z);
	}

	public final static String FloatArrayToString(BFloatArray x) {
		return x.toString();
	}

	// Map
	public final static <T> boolean HasKey(BunMap<T> x, String y) {
		return x.HasKey(y);
	}

	public final static <T> BArray<String> Keys(BunMap<T> x) {
		return x.keys();
	}

	public final static <T> String MapToString(BunMap<T> x) {
		return x.toString();
	}

	// Object
	public final static String ObjectToString(Object x) {
		return x.toString();
	}

	public static SoftwareFault ToFault(Throwable t) {
		if(t instanceof SoftwareFault) {
			return (SoftwareFault) t;
		}
		return new SoftwareFault(t);
	}

	//	public final static JavaStaticFunc ConvertToNativeFunc(Method jMethod) {
	//		@Var ZFuncType FuncType = JavaTypeTable.ConvertToFuncType(jMethod);
	//		return new JavaStaticFunc(jMethod.getName(), FuncType, jMethod);
	//	}
	//
	//	static ZFunc LoadFunc(String Name, Class<?> ... classes) {
	//		try {
	//			return ConvertToNativeFunc(JavaCommonApi.class.getMethod(Name, classes));
	//		} catch (Exception e) {
	//			LibZen._Exit(1, "FIXME: " + e);
	//		}
	//		return null;
	//	}

	//	static void LoadCommonApi(ZGenerator Generator) {
	//		Generator.SetDefinedFunc(LoadFunc("Assert", boolean.class));
	//		Generator.SetDefinedFunc(LoadFunc("Assert", boolean.class, String.class));
	//		Generator.SetConverterFunc(ZType.FloatType, ZType.IntType, LoadFunc("FloatToInt", double.class));
	//		Generator.SetConverterFunc(ZType.IntType, ZType.FloatType, LoadFunc("IntToFloat", long.class));
	//		Generator.SetConverterFunc(ZType.VarType, ZType.StringType, LoadFunc("ToString", Object.class));
	//		Generator.SetConverterFunc(ZType.BooleanType, ZType.StringType, LoadFunc("ToString", boolean.class));
	//		Generator.SetConverterFunc(ZType.IntType, ZType.StringType, LoadFunc("ToString", long.class));
	//		Generator.SetConverterFunc(ZType.FloatType, ZType.StringType, LoadFunc("ToString", double.class));
	//		Generator.SetDefinedFunc(LoadFunc("Size", String.class));
	//	}
}
