package com.protegeplugin;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import com.google.gson.Gson;
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

                server.createContext("/concepts", exchange -> {
                    modelManager = getOWLModelManager();
                    OWLOntology activeOntology = modelManager.getActiveOntology();
                    OWLDataFactory factory = modelManager.getOWLDataFactory();
                    Gson gson = new Gson();

                    if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                         Set<OWLClass> presentConcepts = activeOntology.getClassesInSignature();
                         String response = gson.toJson(presentConcepts);
                         sendResponse(exchange, response);
                    }
                    else if ("POST".equalsIgnoreCase(exchange.getRequestMethod()))
                    {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
                        CreateNewConceptRequest concept = gson.fromJson(reader, CreateNewConceptRequest.class);

                        OWLClass newClass = factory.getOWLClass(IRI.create(concept.Uri));
                        OWLDeclarationAxiom declaration = factory.getOWLDeclarationAxiom(newClass);
                        AddAxiom addAxiom = new AddAxiom(activeOntology, declaration);

                        // Schedule on UI thread
                        SwingUtilities.invokeLater(() -> {
                            modelManager.applyChange(addAxiom);
                        });

                        sendResponse(exchange, "Success");
                    } else
                    {
                        exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                    }
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