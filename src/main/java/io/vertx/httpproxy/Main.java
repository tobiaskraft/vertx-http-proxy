package io.vertx.httpproxy;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */

public class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);
  
  @Parameter(names = "--port")
  public int port = 8080;

  @Parameter(names = "--address")
  public String address = "0.0.0.0";

  @Parameter(names = "--destinationAddress")
  public String destinationAddress = "destserver";

  @Parameter(names = "--destinationPort")
  public int destinationPort = 443;

  public static void main(String[] args) {
    Main main = new Main();
    JCommander jc = new JCommander(main);
    jc.parse(args);
    main.run();
  }

  public void run() {
    InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);

    Vertx vertx = Vertx.vertx();
    HttpClient client = vertx.createHttpClient(new HttpClientOptions()
        .setMaxInitialLineLength(10000)
        .setSsl(true).setTrustAll(true)
        .setLogActivity(true));

    HttpProxy proxy = HttpProxy
        .reverseProxy(client)
        .target(destinationPort, destinationAddress);

    HttpServer proxyServer = vertx.createHttpServer(new HttpServerOptions()
        .setPort(port)
        .setMaxInitialLineLength(10000)
        .setLogActivity(true))
        .requestHandler(req -> {
          log.info("path: "+ req.path()+" params: "+req.params());
          proxy.handle(req);
        });

    proxyServer.listen(ar -> {
      if (ar.succeeded()) {
        log.info("Proxy server started on " + port);
        log.info("destination address " + destinationAddress);
        log.info("destination port " + destinationPort);
      } else {
        ar.cause().printStackTrace();
      }
    });
  }
}
