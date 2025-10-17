package com.protegeplugin;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;
import org.semanticweb.owlapi.model.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.swing.*;

public class ProtegeMCPPlugin extends ProtegeOWLAction {
    private OWLModelManager modelManager;

    public void initialise() {
        Thread serverThread = new Thread(() -> {
            try {
                int port = 8080;
                HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
                server.createContext("/hello", exchange -> {
                    System.out.println("HIT! Path: " + exchange.getRequestURI().getPath());
                    modelManager = getOWLModelManager();
                    OWLOntology owlOntology = modelManager.getActiveOntology();
                    OWLDataFactory factory = modelManager.getOWLDataFactory();
                    OWLClass newClass = factory.getOWLClass(IRI.create("http://example.com#MyNewConcept" + new Random().nextInt()));
                    OWLDeclarationAxiom declaration = factory.getOWLDeclarationAxiom(newClass);
                    AddAxiom addAxiom = new AddAxiom(owlOntology, declaration);

                    // Schedule on UI thread
                    SwingUtilities.invokeLater(() -> {
                        modelManager.applyChange(addAxiom);
                        System.out.println("6");
                        System.out.println("DONE!");
                    });

                    sendResponse(exchange, "Hello world");
                });

                server.setExecutor(null); // default executor
                server.start();
                System.out.println("✅ Server running at http://localhost:" + port);

                // Keep a reference to stop later
                Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // Mark as daemon, so it doesn’t prevent Protege shutdown
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void sendResponse(HttpExchange exchange, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public void dispose() {}

    public void actionPerformed(ActionEvent event) {}
}