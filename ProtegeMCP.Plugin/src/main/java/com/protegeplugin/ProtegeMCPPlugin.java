package com.protegeplugin;

import java.awt.event.ActionEvent;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
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
                    if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        OWLOntology activeOntology = modelManager.getActiveOntology();
                        OWLDataFactory dataFactory = modelManager.getOWLDataFactory();

                        Gson gson = new Gson();
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
                            RenameConceptRequest request = gson.fromJson(reader, RenameConceptRequest.class);

                            // Build new IRI
                            IRI oldIRI = IRI.create(request.oldUri);
                            IRI newIRI = IRI.create(request.newUri);

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
                        }
                        catch (Exception e) {
                            sendResponse(exchange, "Error: " + e.getMessage());
                        }


                    } else {
                        exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                    }
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
                    if ("POST".equalsIgnoreCase(exchange.getRequestMethod()))
                    {
                        OWLModelManager modelManager = getOWLModelManager();
                        OWLOntologyManager ontologyManager = modelManager.getOWLOntologyManager();

                        Gson gson = new Gson();
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
                            OpenOntologyRequest request = gson.fromJson(reader, OpenOntologyRequest.class);
                            File file = new File(request.path);
                            OWLOntology loadedOntology;
                            loadedOntology = ontologyManager.loadOntologyFromOntologyDocument(file);
                            modelManager.setActiveOntology(loadedOntology);
                            modelManager.fireEvent(EventType.ACTIVE_ONTOLOGY_CHANGED);
                            sendResponse(exchange, "Ontology opened successfully");
                        }
                        catch (Exception e) {
                            sendResponse(exchange, "Error: " + e.getMessage());
                        }
                    }
                    else {
                        exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                    }
                });

                server.createContext("/save", exchange -> {
                    if ("POST".equalsIgnoreCase(exchange.getRequestMethod()))
                    {
                        Gson gson = new Gson();
                        modelManager = getOWLModelManager();
                        OWLOntology activeOntology = modelManager.getActiveOntology();
                        OWLOntologyManager ontologyManager = modelManager.getOWLOntologyManager();

                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
                            SaveOntologyRequest request = gson.fromJson(reader, SaveOntologyRequest.class);
                            if (request.path == null || request.path.trim().isEmpty())
                            {
                                modelManager.save();
                            } else {
                                File file = new File(request.path);
                                IRI documentIRI = IRI.create(file.toURI());
                                ontologyManager.saveOntology(activeOntology, documentIRI);
                            }

                            sendResponse(exchange, "Ontology saved successfully");
                        } catch (OWLOntologyStorageException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    else {
                        exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                    }
                });

                server.createContext("/subclass", exchange -> {
                    if ("POST".equalsIgnoreCase(exchange.getRequestMethod()))
                    {
                        modelManager = getOWLModelManager();
                        OWLOntology activeOntology = modelManager.getActiveOntology();
                        OWLDataFactory factory = modelManager.getOWLDataFactory();
                        Gson gson = new Gson();

                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
                            SubclassConceptRequest concept = gson.fromJson(reader, SubclassConceptRequest.class);

                            OWLClass childClass = factory.getOWLClass(IRI.create(concept.childUri));
                            OWLClass parentClass = factory.getOWLThing();
                            if(concept.parentUri != null && !concept.parentUri.trim().isEmpty()){
                                parentClass = factory.getOWLClass(IRI.create(concept.parentUri));
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
                        } catch (Exception e) {
                            sendResponse(exchange, "Error: " + e.getMessage());
                        }

                    } else {
                        exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                    }
                });

                server.createContext("/delete-concept", exchange -> {
                    if("POST".equalsIgnoreCase(exchange.getRequestMethod()))
                    {
                        modelManager = getOWLModelManager();
                        OWLOntology activeOntology = modelManager.getActiveOntology();
                        OWLDataFactory factory = modelManager.getOWLDataFactory();
                        Gson gson = new Gson();
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
                            DeleteConceptRequest concept = gson.fromJson(reader, DeleteConceptRequest.class);

                            OWLClass childClass = factory.getOWLClass(IRI.create(concept.uri));
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
                        } catch (Exception e) {
                            sendResponse(exchange, "Error: " + e.getMessage());
                        }
                    }
                    else
                    {
                        exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                    }
                });

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
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
                            CreateNewConceptRequest concept = gson.fromJson(reader, CreateNewConceptRequest.class);

                            OWLClass childClass = factory.getOWLClass(IRI.create(concept.uri));
                            OWLDeclarationAxiom declaration = factory.getOWLDeclarationAxiom(childClass);

                            SwingUtilities.invokeLater(() -> {
                                modelManager.applyChange(new AddAxiom(activeOntology, declaration));
                                try {
                                    sendResponse(exchange, "Success");
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                        } catch (Exception e) {
                            sendResponse(exchange, "Error: " + e.getMessage());
                        }
                    }
                    else
                    {
                        exchange.sendResponseHeaders(405, -1); // Method Not Allowed
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
}