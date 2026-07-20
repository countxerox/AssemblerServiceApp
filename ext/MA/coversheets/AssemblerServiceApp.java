package ext.MA.coversheets;


import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public class AssemblerServiceApp {

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/soap/services/AssemblerService",
                new RequestHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("AssemblerServiceApp started on port 8080");
    }
}
