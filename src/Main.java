import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

public class Main {
    public static final Version VERSION = Version.v1_14Plus;

    public static void main(String[] args) throws IOException {
        final String outputFileName = System.getProperty("user.home") + "\\OneDrive\\Desktop\\eyeSim.csv";
        PrintWriter out = new PrintWriter(outputFileName);
        Random rand = new Random();

        int strongholdX = 0;
        int strongholdZ = 0;
        double[] errorByTick = new double[80];

        final int NUMSIMS = 1_000_000;
//        double[] errors = new double[NUMSIMS];

        for (int count = 0; count < NUMSIMS; ++count) {
            double yaw = rand.nextDouble(-Math.PI, Math.PI);
            double distance = rand.nextDouble(100, 2000);

            double playerX = strongholdX + distance * Math.sin(yaw);
            double playerZ = strongholdZ - distance * Math.cos(yaw);
            double playerY = 64;

            // Assuming corner jam for no position imprecision
            playerX = Math.round(100 * playerX) / 100.0 + ((Math.abs(playerX % 1) < 0.5) ? 0.3 : -0.3);
            playerZ = Math.round(100 * playerZ) / 100.0 + ((Math.abs(playerZ % 1) < 0.5) ? 0.3 : -0.3);

            final double roundedPlayerX = Math.round(100 * playerX) / 100.0;
            final double roundedPlayerZ = Math.round(100 * playerZ) / 100.0;

            yaw = Math.atan2(roundedPlayerX - strongholdX, strongholdZ - roundedPlayerZ) * 180.0 / Math.PI;

            EyeOfEnder eye = new EyeOfEnder(playerX, playerY + 0.9, playerZ);
            eye.moveTowards(strongholdX, strongholdZ);
//            System.out.println("Player: " + playerX + " " + playerY + " " + playerZ);

            for (int tick = 0; tick < 80; ++tick) {
                // Assuming server and client ticks alternate
                eye.serverTick();
                eye.clientTick();

                double serverError = Math.atan2(playerX - eye.serverX, eye.serverZ - playerZ) * 180.0 / Math.PI - yaw;
                if (serverError < -180) serverError += 360;
                if (serverError > 180) serverError -= 360;

                double clientError = Math.atan2(playerX - eye.clientX, eye.clientZ - playerZ) * 180.0 / Math.PI - yaw;
                if (clientError < -180) clientError += 360;
                if (clientError > 180) clientError -= 360;

                double error = clientError - yawBasedError(yaw);
                errorByTick[tick] += Math.abs(error);
//                errors[count] = error;
            }
        }

        for (int i = 0; i < 80; ++i) {
            out.println(errorByTick[i] / NUMSIMS);
        }

//        for (int i = 0; i < NUMSIMS; ++i) {
//            System.out.println(errors[i]);
//        }
        out.close();
    }

    public static double yawBasedError(double yaw) {
        final double approximateErrorMag = 1 / (PositionPacket.getConversionFactor() * 12 * Math.sqrt(2)) * 180 / Math.PI;
        return approximateErrorMag * Math.sin((yaw + 45) * Math.PI / 180.0);
    }
}
