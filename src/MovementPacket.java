public class MovementPacket {
    public int x;
    public int y;
    public int z;

    public MovementPacket(double x, double y, double z) {
        this.x = (int) (x * 8000.0D);
        this.y = (int) (y * 8000.0D);
        this.z = (int) (z * 8000.0D);
    }

    public double getX() {
        return (double) this.x / 8000.0D;
    }

    public double getY() {
        return (double) this.y / 8000.0D;
    }

    public double getZ() {
        return (double) this.z / 8000.0D;
    }
}
