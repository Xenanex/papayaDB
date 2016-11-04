package papayaDB.rest;

import java.util.function.Consumer;

import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import papayaDB.api.QueryAnswer;
import papayaDB.api.chainable.AbstractChainableQueryInterface;
import papayaDB.client.Connection;

/**
 * Représente une interface web d'accès à une base de données, par l'intermédiaire de routes
 * suivant le REST. 
 *
 */
public class RESTQueryInterface extends AbstractChainableQueryInterface {
	private final HttpServer listeningServer;
	private final NetClient tcpClient;
	private final String host;
	private final int port;
	private final Router router;


	public RESTQueryInterface(String host, int port) {
		NetClientOptions options = new NetClientOptions();
		tcpClient = getVertx().createNetClient(options);
		this.host = host;
		this.port = port;

		router = Router.router(getVertx());
		router.get("/*").handler(this::onRESTQuery);
		
		listeningServer = getVertx().createHttpServer();
	}
	
	public void listen() {
		listeningServer.requestHandler(router::accept).listen(8080);
		System.out.println("Now listening for HTTP REST queries...");
	}
	
	@Override
	public void close() {
		listeningServer.close();
		tcpClient.close();
		super.close();
	}

	@Override
	public void processQuery(String query, Consumer<QueryAnswer> callback) {
		JsonObject answer = new JsonObject();
		answer.put("test", "test");
		callback.accept(new QueryAnswer(answer));
		/*
		// VRAI CODE EN SUPPOSANT QU'UNE DB EXISTE DE L'AUTRE COTE DE LA CONNEXION
		tcpClient.connect(port, host, connectHandler -> {
			if (connectHandler.succeeded()) {
				System.out.println("Connection established for query");
				NetSocket socket = connectHandler.result();
				
				// Définir quoi faire avec la réponse
				socket.handler(buffer -> {
					JsonObject answer = buffer.toJsonObject();
					callback.accept(new QueryAnswer(answer));
				});
				
				// Envoyer la demande
				socket.write(query);

			} else {
				System.out.println("Failed to connect: " + connectHandler.cause().getMessage());
			}
		});
		*/
	}

	public void onRESTQuery(RoutingContext routingContext) {
		HttpServerResponse response = routingContext.response();
		HttpServerRequest request = routingContext.request();
		String path = request.path();

		System.out.println("Received query "+path);
		
		processQuery(path, answer -> {
			response.putHeader("content-type", "application/json")
			.end(Json.encodePrettily(answer.getData()));
		});
	}

	
	public static void main(String[] args) {
		RESTQueryInterface RESTInterface = new RESTQueryInterface("localhost", 200);
		RESTInterface.listen();
	}
}
