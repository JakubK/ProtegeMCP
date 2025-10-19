package com.protegeplugin;

import java.awt.event.ActionEvent;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;
import org.semanticweb.owlapi.model.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

import javax.swing.*;

public class ProtegeMCPPlugin extends ProtegeOWLAction {
    private OWLModelManager modelManager;

    public void initialise() {
        Thread serverThread = new Thread(() -> {
            try {
                int port = 8080;
                try (ServerSocket serverSocket = new ServerSocket(port)) {
                    serverSocket.setReuseAddress(true);
                } catch (IOException e) {
                    return; // Port is already in use
                }
                HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

                server.createContext("/rename-concept", exchange -> {
                    Map<String, String> qparams = parseQueryParams(exchange);

                    // Build new IRI
                    IRI oldIRI = IRI.create(qparams.get("oldUri"));
                    IRI newIRI = IRI.create(qparams.get("newUri"));

                    // Create renamer
                    OWLEntityRenamer renamer = new OWLEntityRenamer(
                        modelManager.getOWLOntologyManager(),
                        modelManager.getOntologies()
                    );
                    List<OWLOntologyChange> changes = renamer.changeIRI(oldIRI, newIRI);

                    SwingUtilities.invokeLater(() -> {
                        for(OWLOntologyChange change : changes)
                        {
                            modelManager.applyChange(change);
                        }
                        try {
                            sendResponse(exchange, "Success");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                server.createContext("/new", exchange -> {
                    if ("POST".equalsIgnoreCase(exchange.getRequestMethod()))
                    {
                        OWLModelManager modelManager = getOWLModelManager();
                        OWLOntologyManager ontologyManager = modelManager.getOWLOntologyManager();

                        IRI newOntologyIRI = IRI.create("http://example.org/newOntology");
                        try {
                            OWLOntology newOntology = ontologyManager.createOntology(newOntologyIRI);
                            modelManager.setActiveOntology(newOntology);
                            modelManager.fireEvent(EventType.ACTIVE_ONTOLOGY_CHANGED);
                            sendResponse(exchange, "New ontology created successfully");
                        } catch (OWLOntologyCreationException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    else {
                        exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                    }
                });

                server.createContext("/open", exchange -> {
                    OWLModelManager modelManager = getOWLModelManager();
                    OWLOntologyManager ontologyManager = modelManager.getOWLOntologyManager();
                    Map<String, String> qparams = parseQueryParams(exchange);

                    String path = qparams.get("path");
                    File file = new File(path);
                    OWLOntology loadedOntology;

                    try {
                        loadedOntology = ontologyManager.loadOntologyFromOntologyDocument(file);
                        modelManager.setActiveOntology(loadedOntology);
                        modelManager.fireEvent(EventType.ACTIVE_ONTOLOGY_CHANGED);
                        sendResponse(exchange, "Ontology opened successfully");
                    } catch (OWLOntologyCreationException e) {
                        sendResponse(exchange, "Error occured while Opening " + path
                                + " ontology: " + e.getMessage());
                    }
                });

                server.createContext("/save", exchange -> {
                        modelManager = getOWLModelManager();
                        OWLOntology activeOntology = modelManager.getActiveOntology();
                        OWLOntologyManager ontologyManager = modelManager.getOWLOntologyManager();

                        Map<String, String> qparams = parseQueryParams(exchange);
                        String path = qparams.get("path");

                        if (path == null || path.trim().isEmpty())
                        {
                            try {
                                modelManager.save();
                                sendResponse(exchange, "Ontology saved successfully");
                            } catch (OWLOntologyStorageException e) {
                                sendResponse(exchange, "Error occured while saving at" + path
                                        + " error:" + e.getMessage());
                            }
                        } else {
                            File file = new File(path);
                            IRI documentIRI = IRI.create(file.toURI());
                            try {
                                ontologyManager.saveOntology(activeOntology, documentIRI);
                                sendResponse(exchange, "Ontology saved successfully");
                            } catch (OWLOntologyStorageException e) {
                                sendResponse(exchange, "Error occured while saving at" + path
                                        + " error:" + e.getMessage());
                            }
                        }
                });

                server.createContext("/subclass-concept", exchange -> {
                        modelManager = getOWLModelManager();
                        OWLOntology activeOntology = modelManager.getActiveOntology();
                        OWLDataFactory factory = modelManager.getOWLDataFactory();

                        Map<String, String> qparams = parseQueryParams(exchange);
                        OWLClass childClass = factory.getOWLClass(IRI.create(qparams.get("childUri")));
                        OWLClass parentClass = factory.getOWLThing();

                        String parentUri = qparams.get("parentUri");

                        if(parentUri != null && !parentUri.trim().isEmpty()){
                            parentClass = factory.getOWLClass(IRI.create(parentUri));
                        }

                        OWLSubClassOfAxiom subClassAxiom = factory.getOWLSubClassOfAxiom(childClass, parentClass);
                        SwingUtilities.invokeLater(() -> {
                            modelManager.applyChange(new AddAxiom(activeOntology, subClassAxiom));

                            try {
                                sendResponse(exchange, "Success");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                });

                server.createContext("/delete-concept", exchange -> {
                    modelManager = getOWLModelManager();
                    OWLOntology activeOntology = modelManager.getActiveOntology();
                    OWLDataFactory factory = modelManager.getOWLDataFactory();

                    Map<String, String> qparams = parseQueryParams(exchange);
                    OWLClass childClass = factory.getOWLClass(IRI.create(qparams.get("conceptUri")));
                    Set<OWLAxiom> referencingAxioms = activeOntology.getReferencingAxioms(childClass);

                    SwingUtilities.invokeLater(() -> {

                        for(OWLAxiom x : referencingAxioms)
                        {
                            modelManager.applyChange(new RemoveAxiom(activeOntology, x));
                        }
                        try {
                            sendResponse(exchange, "Success");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                server.createContext("/concepts", exchange -> {
                    modelManager = getOWLModelManager();
                    OWLOntology activeOntology = modelManager.getActiveOntology();
                    OWLDataFactory factory = modelManager.getOWLDataFactory();

                    if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                         Set<OWLClass> presentConcepts = activeOntology.getClassesInSignature();
                         String response = presentConcepts.toString();
                         sendResponse(exchange, response);
                    }
                    else if ("POST".equalsIgnoreCase(exchange.getRequestMethod()))
                    {
                        Map<String, String> qparams = parseQueryParams(exchange);
                        OWLClass childClass = factory.getOWLClass(IRI.create(qparams.get("uri")));
                        OWLDeclarationAxiom declaration = factory.getOWLDeclarationAxiom(childClass);

                        SwingUtilities.invokeLater(() -> {
                            modelManager.applyChange(new AddAxiom(activeOntology, declaration));
                            try {
                                sendResponse(exchange, "Success");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                });

                server.setExecutor(null);
                server.start();
                System.out.println("✅ Server running at http://localhost:" + port);

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

    private Map<String, String> parseQueryParams(HttpExchange exchange) {
        Map<String, String> result = new HashMap<>();
        String query = exchange.getRequestURI().getQuery(); // e.g. offset=10&limit=100
        if (query != null) {
            String[] pairs = query.split("&");
            for (String p : pairs) {
                String[] kv = p.split("=");
                if (kv.length == 2) {
                    // URL decode parameter values
                    try {
                        String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                        String value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                        result.put(key, value);
                    } catch (Exception e) {
                        System.out.println( "Error decoding URL parameter" + e);
                    }
                }
            }
        }
        return result;
    }
}