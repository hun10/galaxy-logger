public class NormalBrain {
    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 60; i++) {
            long delay = (long) (1000 + Math.random() * 2000);

            System.out.println("Sleeping for " + delay);

            Thread.sleep(delay);
        }
    }
}
