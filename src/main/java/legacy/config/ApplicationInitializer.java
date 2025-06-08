package legacy.config;

import com.sun.tools.javac.Main;
import legacy.http.request.RequestHandler;
import org.yaml.snakeyaml.Yaml;
import server.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ApplicationInitializer {

    public static void initialize() throws Exception {

        Yaml yaml = new Yaml();
        List<String> packageToScan;

        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("config.yml")) {
            Map<String, List<String>> data = yaml.load(input);
            packageToScan = data.get("packages");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        for (String packageName : packageToScan) {
            Container.getInstance().scan(packageName.trim());
        }

        Collection<Object> components = Container.getInstance().getComponents();
        for (Object component : components) {
            System.out.println(component.getClass().getName());
        }

        RequestHandler handler = Container.getInstance().get(RequestHandler.class);
        handler.setControllers(Container.getInstance().scanControllers());

//        InputHandler inputHandler = Container.getInstance().get(InputHandler.class);
//        inputHandler.startInputLoop();

        HttpServer server = Container.getInstance().get(HttpServer.class);
        server.serverStart(8080);
    }
}
