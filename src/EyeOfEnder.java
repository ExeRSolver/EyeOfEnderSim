public class EyeOfEnder {
    public double serverX;
    public double serverY;
    public double serverZ;
    public double serverMotionX = 0;
    public double serverMotionY = 0;
    public double serverMotionZ = 0;

    public double clientX;
    public double clientY;
    public double clientZ;
    public double clientMotionX = 0;
    public double clientMotionY = 0;
    public double clientMotionZ = 0;

    public double targetX;
    public double targetY;
    public double targetZ;
    public int despawnTimer;

    public PositionPacket positionPacket = null;
    public MovementPacket movementPacket = null;
    private long encodedServerX;
    private long encodedServerY;
    private long encodedServerZ;
    private double trackedMotionX = 0;
    private double trackedMotionY = 0;
    private double trackedMotionZ = 0;
    private long encodedClientX;
    private long encodedClientY;
    private long encodedClientZ;

    public EyeOfEnder(double x, double y, double z) {
        serverX = x;
        serverY = y;
        serverZ = z;

        clientX = x;
        clientY = y;
        clientZ = z;

        encodedServerX = PositionPacket.encode(x);
        encodedServerY = PositionPacket.encode(y);
        encodedServerZ = PositionPacket.encode(z);

        encodedClientX = PositionPacket.encode(x);
        encodedClientY = PositionPacket.encode(y);
        encodedClientZ = PositionPacket.encode(z);
    }

    public void moveTowards(int x, int z) {
        double dx = x - serverX;
        double dz = z - serverZ;
        float distance = MathHelper.sqrt(dx * dx + dz * dz);

        // Assuming the stronghold is over 12 blocks away
        this.targetX = serverX + dx / (double)distance * 12.0D;
        this.targetZ = serverZ + dz / (double)distance * 12.0D;
        this.targetY = serverY + 8.0D;

        this.despawnTimer = 0;
    }

    public void serverTick() {
        serverX += serverMotionX;
        serverY += serverMotionY;
        serverZ += serverMotionZ;
        float horizontalMag = MathHelper.sqrt(serverMotionX * serverMotionX + serverMotionZ * serverMotionZ);

        double distXToTarget = this.targetX - serverX;
        double distZToTarget = this.targetZ - serverZ;
        float horDistToTarget = (float) Math.sqrt(distXToTarget * distXToTarget + distZToTarget * distZToTarget);
        float yawToTarget = (float) MathHelper.atan2(distZToTarget, distXToTarget);
        double nextHorizontalMag = MathHelper.lerp(0.0025D, (double)horizontalMag, (double)horDistToTarget);
        double nextVerticalMag = serverMotionY;
        if (horDistToTarget < 1.0F) {
            nextHorizontalMag *= 0.8D;
            nextVerticalMag *= 0.8D;
        }

        int j = serverY < this.targetY ? 1 : -1;
        serverMotionX = Math.cos(yawToTarget) * nextHorizontalMag;
        serverMotionY = nextVerticalMag + ((double) j - nextVerticalMag) * (double) 0.015F;
        serverMotionZ = Math.sin(yawToTarget) * nextHorizontalMag;

        this.sendPackets();
        ++despawnTimer;
    }

    private void sendPackets() {
        if (despawnTimer % 4 != 0)
            return;

        long encodedDeltaX;
        long encodedDeltaY;
        long encodedDeltaZ;
        boolean positionChanged;
        if (Main.VERSION == Version.v1_14Plus) {
            double deltaX = serverX - PositionPacket.decode(encodedServerX);
            double deltaY = serverY - PositionPacket.decode(encodedServerY);
            double deltaZ = serverZ - PositionPacket.decode(encodedServerZ);
            positionChanged = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ >= 7.62939453125E-6D;

            encodedDeltaX = PositionPacket.encode(deltaX);
            encodedDeltaY = PositionPacket.encode(deltaY);
            encodedDeltaZ = PositionPacket.encode(deltaZ);
        }
        else {
            encodedDeltaX = PositionPacket.encode(serverX) - encodedServerX;
            encodedDeltaY = PositionPacket.encode(serverY) - encodedServerY;
            encodedDeltaZ = PositionPacket.encode(serverZ) - encodedServerZ;
            if (Main.VERSION == Version.v1_8Minus)
                positionChanged = Math.abs(encodedDeltaX) >= 4 || Math.abs(encodedDeltaY) >= 4 || Math.abs(encodedDeltaZ) >= 4;
            else
                positionChanged = encodedDeltaX * encodedDeltaX + encodedDeltaY * encodedDeltaY + encodedDeltaZ * encodedDeltaZ >= 128;
        }
        boolean shouldUpdatePosition = positionChanged || despawnTimer % 60 == 0;

        if (shouldUpdatePosition) {
            encodedServerX = PositionPacket.encode(serverX);
            encodedServerY = PositionPacket.encode(serverY);
            encodedServerZ = PositionPacket.encode(serverZ);
            if (despawnTimer > 0)
                positionPacket = new PositionPacket((short)((int)encodedDeltaX), (short)((int)encodedDeltaY), (short)((int)encodedDeltaZ));
        }

        if ((Main.VERSION == Version.v1_11To1_13 || Main.VERSION == Version.v1_14Plus) && despawnTimer == 0)
            return;

        double motionDeltaX = serverMotionX - trackedMotionX;
        double motionDeltaY = serverMotionY - trackedMotionY;
        double motionDeltaZ = serverMotionZ - trackedMotionZ;
        double MotionDeltaSqr = motionDeltaX * motionDeltaX + motionDeltaY * motionDeltaY + motionDeltaZ * motionDeltaZ;
        if (MotionDeltaSqr > ((Main.VERSION == Version.v1_14Plus) ? 1.0E-7D : 0.0004) || MotionDeltaSqr > 0 && serverMotionX == 0 && serverMotionY == 0 && serverMotionZ == 0) {
            trackedMotionX = serverMotionX;
            trackedMotionY = serverMotionY;
            trackedMotionZ = serverMotionZ;
            movementPacket = new MovementPacket(trackedMotionX, trackedMotionY, trackedMotionZ);
        }
    }

    public void clientTick() {
        if (positionPacket != null) {
            encodedClientX += positionPacket.x;
            encodedClientY += positionPacket.y;
            encodedClientZ += positionPacket.z;

            clientX = PositionPacket.decode(encodedClientX);
            clientY = PositionPacket.decode(encodedClientY);
            clientZ = PositionPacket.decode(encodedClientZ);

            positionPacket = null;
        }

        if (movementPacket != null) {
            clientMotionX = movementPacket.getX();
            clientMotionY = movementPacket.getY();
            clientMotionZ = movementPacket.getZ();

            movementPacket = null;
        }

        clientX += clientMotionX;
        clientY += clientMotionY;
        clientZ += clientMotionZ;
    }
}
