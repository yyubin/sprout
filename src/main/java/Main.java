
import sprout.beans.annotation.ComponentScan;
import sprout.boot.SproutApplication;

@ComponentScan(basePackages = {"sprout", "app"})
public class Main {

    public static void main(String[] args) throws Exception {
        SproutApplication.run(Main.class);
    }

}
