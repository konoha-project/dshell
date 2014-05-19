
package dshell.internal.jvm;

public class JavaCastApi {

	public static Object toObject(boolean x) { return x; }
	public static Object toObject(byte x)    { return x; }
	public static Object toObject(short x)   { return x; }
	public static Object toObject(int x)     { return x; }
	public static Object toObject(long x)    { return x; }
	public static Object toObject(float x)   { return x; }
	public static Object toObject(double x)  { return x; }

	public static Object toObject(Boolean x) { return x; }
	public static Object toObject(Byte x)    { return x; }
	public static Object toObject(Short x)   { return x; }
	public static Object toObject(Integer x) { return x; }
	public static Object toObject(Long x)    { return x; }
	public static Object toObject(Float x)   { return x; }
	public static Object toObject(Double x)  { return x; }

	public static boolean toboolean(Object x) { return x == null ? false:(Boolean)x; }
	public static byte    tobyte(Object x)    { return x == null ? 0:(Byte)x; }
	public static short   toshort(Object x)   { return x == null ? 0:(Short)x; }
	public static int     toint(Object x)     { return x == null ? 0:(Integer)x; }
	public static long    tolong(Object x)    { return x == null ? 0:(Long)x; }
	public static float   tofloat(Object x)   { return x == null ? 0:(Float)x; }
	public static double  todouble(Object x)  { return x == null ? 0.0:(Double)x; }

	public static Boolean toBoolean(Object x) { return (Boolean)x; }
	public static Byte    toByte(Object x)    { return (Byte)x; }
	public static Short   toShort(Object x)   { return (Short)x; }
	public static Integer toInteger(Object x) { return (Integer)x; }
	public static Long    toLong(Object x)    { return (Long)x; }
	public static Float   toFloat(Object x)   { return (Float)x; }
	public static Double  toDouble(Object x)  { return (Double)x; }

	public static Boolean toBoolean(boolean x) { return x; }
	public static boolean toboolean(Boolean x) { return x; }

	public static long tolong(byte x)   { return x; }
	public static long tolong(short x)  { return x; }
	public static long tolong(int x)    { return x; }
	public static long tolong(float x)  { return (long)x; }
	public static long tolong(double x) { return (long)x; }

	public static long tolong(Byte x)    { return x.longValue(); }
	public static long tolong(Short x)   { return x.longValue(); }
	public static long tolong(Integer x) { return x.longValue(); }
	public static long tolong(Long x)    { return x.longValue(); }
	public static long tolong(Float x)   { return x.longValue(); }
	public static long tolong(Double x)  { return x.longValue(); }

	public static Long toLong(byte x)  { return (long)x; }
	public static Long toLong(short x) { return (long)x; }
	public static Long toLong(int x)   { return (long)x; }

	public static byte   tobyte(long x)   { return (byte)x; }
	public static short  toshort(long x)  { return (short)x; }
	public static int    toint(long x)    { return (int)x; }
	public static float  tofloat(long x)  { return x; }
	public static double todouble(long x) { return x; }

	public static Byte    toByte(long x)    { return (byte)x; }
	public static Short   toShort(long x)   { return (short)x; }
	public static Integer toInteger(long x) { return (int)x; }
	public static Long    toLong(long x)    { return (long)x; }
	public static Float   toFloat(long x)   { return (float)x; }
	public static Double  toDouble(long x)  { return (double)x; }

	public static double todouble(byte x)  { return x; }
	public static double todouble(short x) { return x; }
	public static double todouble(int x)   { return x; }
	//	public static double todouble(long x) { return (double)x; }
	public static double todouble(float x) { return x; }

	public static double todouble(Byte x)    { return x.doubleValue(); }
	public static double todouble(Short x)   { return x.doubleValue(); }
	public static double todouble(Integer x) { return x.doubleValue(); }
	public static double todouble(Long x)    { return x.doubleValue(); }
	public static double todouble(Float x)   { return x.doubleValue(); }
	public static double todouble(Double x)  { return x.doubleValue(); }

	public static byte  tobyte(double x)  { return (byte)x; }
	public static short toshort(double x) { return (short)x; }
	public static int   toint(double x)   { return (int)x; }
	//	public static double tolong(double x) { return (long)x; }
	public static float tofloat(double x) { return (float)x; }

	public static Byte    toByte(double x)    { return (byte)x; }
	public static Short   toShort(double x)   { return (short)x; }
	public static Integer toInteger(double x) { return (int)x; }
	public static Long    toLong(double x)    { return (long)x; }
	public static Float   toFloat(double x)   { return (float)x; }
	public static Double  toDouble(double x)  { return (double)x; }

	public static String toString(char x)      { return String.valueOf(x); }
	public static String toString(Character x) { return x.toString(); }

	public static String toString(boolean x) { return Boolean.toString(x); }
	//public static String toString(byte x)    { return Byte.toString(x); }
	//public static String toString(short x)   { return Short.toString(x); }
	//public static String toString(int x)     { return Integer.toString(x); }
	public static String toString(long x)    { return Long.toString(x); }
	//public static String toString(float x)   { return Float.toString(x); }
	public static String toString(double x)  { return Double.toString(x); }

	public static String toString(Boolean x) { return x.toString(); }
	//public static String toString(Byte x)    { return x.toString(); }
	//public static String toString(Short x)   { return x.toString(); }
	//public static String toString(Integer x) { return x.toString(); }
	public static String toString(Long x)    { return x.toString(); }
	//public static String toString(Float x)   { return x.toString(); }
	public static String toString(Double x)  { return x.toString(); }

	public static char      tochar(String x)      { return x.charAt(0); }
	public static Character toCharacter(String x) { return x.charAt(0); }

}
