public class PositionPacket {
    public short x;
    public short y;
    public short z;

    public PositionPacket(short x, short y, short z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static long encode(double val) {
        return MathHelper.lfloor(val * getConversionFactor());
    }

    public static double decode(long val) {
        return (double) val / getConversionFactor();
    }

    public static double getConversionFactor() {
        return Main.VERSION == Version.v1_8Minus ? 32.0 : 4096.0;
    }
}
