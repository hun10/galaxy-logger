import java.util.logging.Logger;

public class SmallBrain {
    private static final Logger LOG = Logger.getLogger(SmallBrain.class.getName());

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 60; i++) {
            long delay = (long) (1000 + Math.random() * 2000);

            LOG.info("Sleeping for " + delay);

            Thread.sleep(delay);
        }
    }
}
