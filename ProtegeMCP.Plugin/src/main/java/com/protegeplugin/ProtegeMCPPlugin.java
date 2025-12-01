package com.protegeplugin;

import java.awt.event.ActionEvent;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Supplier;

import com.google.gson.Gson;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxParserImpl;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.semanticweb.owlapi.util.OWLEntityRenamer;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;

import javax.swing.*;

public class ProtegeMCPPlugin extends ProtegeOWLAction {
    private OWLModelManager modelManager;

    public void initialise() {

        Thread mcpThread = new Thread(() -> {
            try {
                InputStream in = ProtegeMCPPlugin.class.getResourceAsStream("/ProtegeMCP.Server");

                String tmpDir = System.getProperty("java.io.tmpdir"); // OS-independent
                Path temp = Files.createTempFile(Paths.get(tmpDir), "ProtegeMCP.Server", ".dll");

                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);

                temp.toFile().setExecutable(true);
                temp.toFile().deleteOnExit();
                new ProcessBuilder(temp.toString()).start();
            } catch (Exception e) {
                System.out.println("Error when starting MCP Server process" + e.getMessage());
            }
        });

        Thread serverThread = new Thread(() -> {
            try {
                int port = 8080;
                try (ServerSocket serverSocket = new ServerSocket(port)) {
                    serverSocket.setReuseAddress(true);
                } catch (IOException e) {
                    return; // Port is already in use
                }
                HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

                server.createContext("/get-current-ontology", exchange -> {
                    modelManager = getOWLModelManager();
                    OWLOntology activeOntology = modelManager.getActiveOntology();
                    OWLOntologyManager ontologyManager = modelManager.getOWLOntologyManager();
                    var iri = ontologyManager.getOntologyDocumentIRI(activeOntology).toString();

                    sendResponse(exchange, iri);
                });

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
                        for (OWLOntologyChange change : changes) {
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
                    if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
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
                    } else {
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

                    if (path == null || path.trim().isEmpty()) {
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

                server.createContext("/delete-concept", exchange -> {
                    modelManager = getOWLModelManager();
                    OWLOntology activeOntology = modelManager.getActiveOntology();
                    OWLDataFactory factory = modelManager.getOWLDataFactory();

                    Map<String, String> qparams = parseQueryParams(exchange);
                    OWLClass childClass = factory.getOWLClass(IRI.create(qparams.get("conceptUri")));
                    Set<OWLAxiom> referencingAxioms = activeOntology.getReferencingAxioms(childClass);

                    SwingUtilities.invokeLater(() -> {

                        for (OWLAxiom x : referencingAxioms) {
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
                    } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
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

                server.createContext("/remove-concept-axiom", exchange -> {
                    try {

                        modelManager = getOWLModelManager();
                        OWLOntology activeOntology = modelManager.getActiveOntology();
                        OWLDataFactory factory = modelManager.getOWLDataFactory();

                        Map<String, String> qparams = parseQueryParams(exchange);
                        var uri = qparams.get("uri");
                        var axiomKind = qparams.get("axiomKind");
                        var axiom = qparams.get("axiom");

                        Supplier<OWLOntologyLoaderConfiguration> configSupplier = () -> activeOntology.getOWLOntologyManager().getOntologyLoaderConfiguration();

                        ManchesterOWLSyntaxParser parser = new ManchesterOWLSyntaxParserImpl(configSupplier, factory);

                        parser.setDefaultOntology(activeOntology);
                        OWLClassExpression expr = parser.parseClassExpression(axiom);
                        OWLClass owlClass = factory.getOWLClass(IRI.create(uri));


                        OWLClassAxiom ax = null;
                        switch (axiomKind) {
                            case "equivalentClass":
                                ax = factory.getOWLEquivalentClassesAxiom(owlClass, expr);
                                break;
                            case "subClass":
                                ax = factory.getOWLSubClassOfAxiom(owlClass, expr);
                                break;
                            case "disjointClass":
                                ax = factory.getOWLDisjointClassesAxiom(owlClass, expr);
                                break;
                            case "disjointUnionClass":
                                Set<OWLClassExpression> disjointSet = new HashSet<>();
                                disjointSet.add(expr);
                                ax = factory.getOWLDisjointUnionAxiom(owlClass, disjointSet);
                                break;
                        }

                        if (ax == null) {
                            sendResponse(exchange, "Unknown axiomType: " + axiomKind);
                            return;
                        }

                        OWLClassAxiom finalAx = ax;
                        SwingUtilities.invokeLater(() -> {
                            modelManager.applyChange(new RemoveAxiom(activeOntology, finalAx));
                            try {
                                sendResponse(exchange, "Success");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } catch (Exception ex) {
                        sendResponse(exchange, ex.toString());
                    }
                });

                server.createContext("/add-concept-axiom", exchange -> {
                    modelManager = getOWLModelManager();
                    OWLOntology activeOntology = modelManager.getActiveOntology();
                    OWLDataFactory factory = modelManager.getOWLDataFactory();

                    Map<String, String> qparams = parseQueryParams(exchange);
                    var uri = qparams.get("uri");
                    var axiomKind = qparams.get("axiomKind");
                    var classExpression = qparams.get("classExpression");

                    Supplier<OWLOntologyLoaderConfiguration> configSupplier = () -> activeOntology.getOWLOntologyManager().getOntologyLoaderConfiguration();

                    ManchesterOWLSyntaxParser parser = new ManchesterOWLSyntaxParserImpl(configSupplier, factory);

                    parser.setDefaultOntology(activeOntology);
                    OWLClassExpression expr = parser.parseClassExpression(classExpression);
                    OWLClass owlClass = factory.getOWLClass(IRI.create(uri));


                    OWLClassAxiom ax = null;
                    switch (axiomKind) {
                        case "equivalentClass":
                            ax = factory.getOWLEquivalentClassesAxiom(owlClass, expr);
                            break;
                        case "subClass":
                            ax = factory.getOWLSubClassOfAxiom(owlClass, expr);
                            break;
                        case "disjointClass":
                            ax = factory.getOWLDisjointClassesAxiom(owlClass, expr);
                            break;
                        case "disjointUnionClass":
                            Set<OWLClassExpression> disjointSet = new HashSet<>();
                            disjointSet.add(expr);
                            ax = factory.getOWLDisjointUnionAxiom(owlClass, disjointSet);
                            break;
                    }

                    if (ax == null) {
                        sendResponse(exchange, "Unknown axiomType: " + axiomKind);
                        return;
                    }

                    OWLClassAxiom finalAx = ax;
                    SwingUtilities.invokeLater(() -> {
                        modelManager.applyChange(new AddAxiom(activeOntology, finalAx));
                        try {
                            sendResponse(exchange, "Success");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                server.createContext("/list-concept-axioms", exchange -> {
                    OWLModelManager modelManager = getOWLModelManager();
                    OWLOntology activeOntology = modelManager.getActiveOntology();
                    OWLDataFactory factory = modelManager.getOWLDataFactory();

                    ShortFormProvider sfp = new SimpleShortFormProvider();
                    ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
                    renderer.setShortFormProvider(sfp);

                    Map<String, String> qparams = parseQueryParams(exchange);
                    OWLClass concept = factory.getOWLClass(IRI.create(qparams.get("uri")));

                    List<String> eqAxioms = new ArrayList<>();
                    for (var axiom : activeOntology.getEquivalentClassesAxioms(concept)) {
                        String rendered = renderer.render(axiom);
                        eqAxioms.add(rendered);
                    }

                    List<String> subAxioms = new ArrayList<>();
                    for (var axiom : activeOntology.getSubClassAxiomsForSubClass(concept)) {
                        String rendered = renderer.render(axiom);
                        subAxioms.add(rendered);
                    }

                    List<String> disjointAxioms = new ArrayList<>();
                    for (var axiom : activeOntology.getDisjointClassesAxioms(concept)) {
                        String rendered = renderer.render(axiom);
                        disjointAxioms.add(rendered);
                    }

                    List<String> disjointUnionAxioms = new ArrayList<>();
                    for (var axiom : activeOntology.getDisjointUnionAxioms(concept)) {
                        String rendered = renderer.render(axiom);
                        disjointUnionAxioms.add(rendered);
                    }

                    List<String> hasKeyAxioms = new ArrayList<>();
                    for (var axiom : activeOntology.getHasKeyAxioms(concept)) {
                        String rendered = renderer.render(axiom);
                        hasKeyAxioms.add(rendered);
                    }

                    sendResponse(exchange, new Gson().toJson(new ListConceptAxiomsResponse(eqAxioms, subAxioms, disjointAxioms, disjointUnionAxioms, hasKeyAxioms)));
                });

                server.createContext("/create-object-property", exchange -> {
                    var modelManager = getOWLModelManager();
                    var activeOntology = modelManager.getActiveOntology();
                    var factory = modelManager.getOWLDataFactory();

                    var qparams = parseQueryParams(exchange);
                    var uri = qparams.get("uri");

                    var propertyIRI = IRI.create(uri);

                    var hasParent = factory.getOWLObjectProperty(propertyIRI);
                    var declarationAxiom = factory.getOWLDeclarationAxiom(hasParent);

                    SwingUtilities.invokeLater(() -> {
                        modelManager.applyChange(new AddAxiom(activeOntology, declarationAxiom));
                        try {
                            sendResponse(exchange, "Success");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                server.createContext("/list-object-properties", exchange -> {
                    var modelManager = getOWLModelManager();
                    var activeOntology = modelManager.getActiveOntology();

                    Set<OWLObjectProperty> properties = activeOntology.getObjectPropertiesInSignature();

                    var response = properties.stream().map(x -> {
                        var functional = !activeOntology.getFunctionalObjectPropertyAxioms(x).isEmpty();
                        var inverseFunctional = !activeOntology.getInverseFunctionalObjectPropertyAxioms(x).isEmpty();
                        var transitive = !activeOntology.getTransitiveObjectPropertyAxioms(x).isEmpty();
                        var symmetric = !activeOntology.getSymmetricObjectPropertyAxioms(x).isEmpty();
                        var asymmetric = !activeOntology.getAsymmetricObjectPropertyAxioms(x).isEmpty();
                        var reflexive = !activeOntology.getReflexiveObjectPropertyAxioms(x).isEmpty();
                        var irreflexive = !activeOntology.getIrreflexiveObjectPropertyAxioms(x).isEmpty();

                        return new ObjectPropertyWithCharacteristics(x, functional, inverseFunctional, transitive, symmetric, asymmetric, reflexive, irreflexive);
                    }).toArray();

                    sendResponse(exchange, new Gson().toJson(response));
                });

                server.createContext("/rename-object-property", exchange -> {
                    var modelManager = getOWLModelManager();
                    var factory = modelManager.getOWLDataFactory();

                    var qparams = parseQueryParams(exchange);
                    var oldUri = IRI.create(qparams.get("oldUri"));
                    var newUri = IRI.create(qparams.get("newUri"));

                    var property = factory.getOWLObjectProperty(oldUri);
                    var renamer = new OWLEntityRenamer(modelManager.getOWLOntologyManager(), modelManager.getOntologies());
                    var changes = renamer.changeIRI(property, newUri);

                    SwingUtilities.invokeLater(() -> {
                        modelManager.applyChanges(changes);
                        try {
                            sendResponse(exchange, "Success");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                server.createContext("/delete-object-property", exchange -> {
                    var modelManager = getOWLModelManager();
                    var activeOntology = modelManager.getActiveOntology();
                    var factory = modelManager.getOWLDataFactory();

                    var qparams = parseQueryParams(exchange);
                    var uri = qparams.get("uri");

                    var propertyIRI = IRI.create(uri);

                    var hasParent = factory.getOWLObjectProperty(propertyIRI);
                    var declarationAxiom = factory.getOWLDeclarationAxiom(hasParent);

                    SwingUtilities.invokeLater(() -> {
                        modelManager.applyChange(new RemoveAxiom(activeOntology, declarationAxiom));
                        try {
                            sendResponse(exchange, "Success");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                server.createContext("/set-object-property-characteristics", exchange -> {
                    var modelManager = getOWLModelManager();
                    var activeOntology = modelManager.getActiveOntology();
                    var factory = modelManager.getOWLDataFactory();

                    var qparams = parseQueryParams(exchange);
                    var uri = IRI.create(qparams.get("uri"));
                    var prop = factory.getOWLObjectProperty(
                            uri);

                    var functional = qparams.get("functional");
                    var inverseFunctional = qparams.get("inverseFunctional");
                    var transitive = qparams.get("transitive");
                    var symmetric = qparams.get("symmetric");
                    var asymmetric = qparams.get("asymmetric");
                    var reflexive = qparams.get("reflexive");
                    var irreflexive = qparams.get("irreflexive");

                    var changes = new ArrayList<OWLOntologyChange>();

                    ProcessCharacteristicChange(changes, activeOntology, factory.getOWLFunctionalObjectPropertyAxiom(prop), functional);
                    ProcessCharacteristicChange(changes, activeOntology, factory.getOWLInverseFunctionalObjectPropertyAxiom(prop), inverseFunctional);
                    ProcessCharacteristicChange(changes, activeOntology, factory.getOWLTransitiveObjectPropertyAxiom(prop), transitive);
                    ProcessCharacteristicChange(changes, activeOntology, factory.getOWLSymmetricObjectPropertyAxiom(prop), symmetric);
                    ProcessCharacteristicChange(changes, activeOntology, factory.getOWLAsymmetricObjectPropertyAxiom(prop), asymmetric);
                    ProcessCharacteristicChange(changes, activeOntology, factory.getOWLReflexiveObjectPropertyAxiom(prop), reflexive);
                    ProcessCharacteristicChange(changes, activeOntology, factory.getOWLIrreflexiveObjectPropertyAxiom(prop), irreflexive);

                    SwingUtilities.invokeLater(() -> {
                        modelManager.applyChanges(changes);
                        try {
                            sendResponse(exchange, "Success");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                server.createContext("/list-object-property-axioms", exchange -> {
                    var modelManager = getOWLModelManager();
                    var ontology = modelManager.getActiveOntology();
                    var df = modelManager.getOWLDataFactory();

                    var qparams = parseQueryParams(exchange);
                    var uri = IRI.create(qparams.get("uri"));
                    var prop = df.getOWLObjectProperty(uri);

                    var axioms = ontology.getAxioms(prop);
                    var response = axioms.toString();
                    sendResponse(exchange, response);
                });

                server.createContext("/add-object-property-axiom", exchange -> {
                    var modelManager = getOWLModelManager();
                    var ontology = modelManager.getActiveOntology();
                    var df = modelManager.getOWLDataFactory();

                    var qparams = parseQueryParams(exchange);
                    var uri = IRI.create(qparams.get("uri"));
                    var axiomKind = qparams.get("axiomKind");
                    var classExpression = qparams.get("classExpression");

                    Supplier<OWLOntologyLoaderConfiguration> configSupplier = () -> ontology.getOWLOntologyManager().getOntologyLoaderConfiguration();

                    ManchesterOWLSyntaxParser parser = new ManchesterOWLSyntaxParserImpl(configSupplier, df);

                    parser.setDefaultOntology(ontology);
                    OWLClassExpression expr = parser.parseClassExpression(classExpression);
                    var prop = df.getOWLObjectProperty(uri);

                    OWLObjectPropertyAxiom ax = null;
                    switch (axiomKind) {
                        case "equivalentTo":
                            OWLObjectProperty eqProp = df.getOWLObjectProperty(IRI.create(classExpression));
                            ax = df.getOWLEquivalentObjectPropertiesAxiom(prop, eqProp);
                            break;
                        case "subPropertyOf":
                            OWLObjectProperty superProp = df.getOWLObjectProperty(IRI.create(classExpression));
                            ax = df.getOWLSubObjectPropertyOfAxiom(prop, superProp);
                            break;
                        case "inverseOf":
                            OWLObjectProperty inverseProp = df.getOWLObjectProperty(IRI.create(classExpression));
                            ax = df.getOWLInverseObjectPropertiesAxiom(prop, inverseProp);
                            break;
                        case "domains":
                            ax = df.getOWLObjectPropertyDomainAxiom(prop, expr);
                            break;
                        case "ranges":
                            ax = df.getOWLObjectPropertyRangeAxiom(prop, expr);
                            break;
                        case "disjointWith":
                            OWLObjectProperty disjointProp = df.getOWLObjectProperty(IRI.create(classExpression));
                            ax = df.getOWLDisjointObjectPropertiesAxiom(prop, disjointProp);
                            break;
                        case "superPropertyOf":
                            OWLObjectProperty subProp = df.getOWLObjectProperty(IRI.create(classExpression));
                            ax = df.getOWLSubObjectPropertyOfAxiom(subProp, prop);
                            break;
                    }

                    if (ax == null) {
                        sendResponse(exchange, "Unknown axiomType: " + axiomKind);
                        return;
                    }

                    var finalAx = ax;

                    SwingUtilities.invokeLater(() -> {
                        ontology.getOWLOntologyManager().applyChange(new AddAxiom(ontology, finalAx));
                        try {
                            sendResponse(exchange, "Success");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                server.createContext("/delete-object-property-axiom", exchange -> {
                    var modelManager = getOWLModelManager();
                    var ontology = modelManager.getActiveOntology();
                    var df = modelManager.getOWLDataFactory();

                    var qparams = parseQueryParams(exchange);
                    var uri = IRI.create(qparams.get("uri"));
                    var axiomKind = qparams.get("axiomKind");
                    var classExpression = qparams.get("classExpression");

                    Supplier<OWLOntologyLoaderConfiguration> configSupplier = () -> ontology.getOWLOntologyManager().getOntologyLoaderConfiguration();

                    ManchesterOWLSyntaxParser parser = new ManchesterOWLSyntaxParserImpl(configSupplier, df);

                    parser.setDefaultOntology(ontology);
                    OWLClassExpression expr = parser.parseClassExpression(classExpression);
                    var prop = df.getOWLObjectProperty(uri);

                    OWLObjectPropertyAxiom ax = null;
                    switch (axiomKind) {
                        case "equivalentTo":
                            OWLObjectProperty eqProp = df.getOWLObjectProperty(IRI.create(classExpression));
                            ax = df.getOWLEquivalentObjectPropertiesAxiom(prop, eqProp);
                            break;
                        case "subPropertyOf":
                            OWLObjectProperty superProp = df.getOWLObjectProperty(IRI.create(classExpression));
                            ax = df.getOWLSubObjectPropertyOfAxiom(prop, superProp);
                            break;
                        case "inverseOf":
                            OWLObjectProperty inverseProp = df.getOWLObjectProperty(IRI.create(classExpression));
                            ax = df.getOWLInverseObjectPropertiesAxiom(prop, inverseProp);
                            break;
                        case "domains":
                            ax = df.getOWLObjectPropertyDomainAxiom(prop, expr);
                            break;
                        case "ranges":
                            ax = df.getOWLObjectPropertyRangeAxiom(prop, expr);
                            break;
                        case "disjointWith":
                            OWLObjectProperty disjointProp = df.getOWLObjectProperty(IRI.create(classExpression));
                            ax = df.getOWLDisjointObjectPropertiesAxiom(prop, disjointProp);
                            break;
                        case "superPropertyOf":
                            OWLObjectProperty subProp = df.getOWLObjectProperty(IRI.create(classExpression));
                            ax = df.getOWLSubObjectPropertyOfAxiom(subProp, prop);
                            break;
                    }

                    if (ax == null) {
                        sendResponse(exchange, "Unknown axiomType: " + axiomKind);
                        return;
                    }

                    var finalAx = ax;

                    SwingUtilities.invokeLater(() -> {
                        ontology.getOWLOntologyManager().applyChange(new RemoveAxiom(ontology, finalAx));
                        try {
                            sendResponse(exchange, "Success");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                server.createContext("/create-individual", exchange -> {
                    var modelManager = getOWLModelManager();
                    var ontology = modelManager.getActiveOntology();
                    var df = modelManager.getOWLDataFactory();

                    var qparams = parseQueryParams(exchange);
                    var uri = IRI.create(qparams.get("uri"));
                    var individual = df.getOWLNamedIndividual(uri);

                    var individualDeclaration = df.getOWLDeclarationAxiom(individual);

                    SwingUtilities.invokeLater(() -> {
                        ontology.getOWLOntologyManager().applyChange(new AddAxiom(ontology, individualDeclaration));
                        try {
                            sendResponse(exchange, "Success");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                server.createContext("/delete-individual", exchange -> {
                    var modelManager = getOWLModelManager();
                    var ontology = modelManager.getActiveOntology();
                    var df = modelManager.getOWLDataFactory();

                    var qparams = parseQueryParams(exchange);
                    var uri = IRI.create(qparams.get("uri"));
                    var individual = df.getOWLNamedIndividual(uri);

                    var individualDeclaration = df.getOWLDeclarationAxiom(individual);

                    SwingUtilities.invokeLater(() -> {
                        ontology.getOWLOntologyManager().applyChange(new RemoveAxiom(ontology, individualDeclaration));
                        try {
                            sendResponse(exchange, "Success");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                server.createContext("/rename-individual", exchange -> {
                    var modelManager = getOWLModelManager();
                    var qparams = parseQueryParams(exchange);

                    var oldIRI = IRI.create(qparams.get("oldUri"));
                    var newIRI = IRI.create(qparams.get("newUri"));

                    var renamer = new OWLEntityRenamer(
                            modelManager.getOWLOntologyManager(),
                            modelManager.getOntologies()
                    );
                    var changes = renamer.changeIRI(oldIRI, newIRI);

                    SwingUtilities.invokeLater(() -> {
                        for (OWLOntologyChange change : changes) {
                            modelManager.applyChange(change);
                        }
                        try {
                            sendResponse(exchange, "Success");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                server.createContext("/list-individuals", exchange -> {
                    var modelManager = getOWLModelManager();
                    var ontology = modelManager.getActiveOntology();
                    var individuals = new ArrayList<IndividualDetails>();

                    for (OWLNamedIndividual ind : ontology.getIndividualsInSignature()) {
                        System.out.println("inc");
                        var types = new ArrayList<String>();
                        ontology.getClassAssertionAxioms(ind).forEach(ax -> types.add(ax.getClassExpression().toString()));

                        var sameIndividuals = new ArrayList<String>();
                        ontology.getSameIndividualAxioms(ind).forEach(ax ->
                                ax.getIndividuals().stream()
                                        .filter(i -> !i.equals(ind))
                                        .forEach(i -> sameIndividuals.add(i.toString())));

                        var differentIndividuals = new ArrayList<String>();
                        ontology.getDifferentIndividualAxioms(ind).forEach(ax ->
                                ax.getIndividuals().stream()
                                        .filter(i -> !i.equals(ind))
                                        .forEach(i -> differentIndividuals.add(i.toString())));

                        var objectPropertyAssertions = new ArrayList<String>();
                        ontology.getObjectPropertyAssertionAxioms(ind).forEach(ax -> {
                            OWLObjectPropertyExpression prop = ax.getProperty();
                            OWLIndividual object = ax.getObject();
                            objectPropertyAssertions.add("  - " + prop + " -> " + object);
                        });

                        var negativeObjectPropertyAssertions = new ArrayList<String>();
                        ontology.getNegativeObjectPropertyAssertionAxioms(ind).forEach(ax -> {
                            OWLObjectPropertyExpression prop = ax.getProperty();
                            OWLIndividual object = ax.getObject();
                            negativeObjectPropertyAssertions.add("  - " + prop + " -> " + object);
                        });

                        individuals.add(new IndividualDetails(ind.getIRI().toString(), types, sameIndividuals, differentIndividuals, objectPropertyAssertions, negativeObjectPropertyAssertions));
                    }
                    sendResponse(exchange, new Gson().toJson(individuals));
                });

                server.createContext("/assign-type", exchange -> {
                    var modelManager = getOWLModelManager();
                    var ontology = modelManager.getActiveOntology();
                    var dataFactory = modelManager.getOWLDataFactory();

                    var qparams = parseQueryParams(exchange);

                    var individualIRI = IRI.create(qparams.get("individualUri"));
                    var typeIRI = IRI.create(qparams.get("typeUri"));

                    var individual = dataFactory.getOWLNamedIndividual(individualIRI);
                    var cls = dataFactory.getOWLClass(typeIRI);

                    var axiom = dataFactory.getOWLClassAssertionAxiom(cls, individual);
                    SwingUtilities.invokeLater(() -> {
                        try {
                            modelManager.applyChange(new AddAxiom(ontology, axiom));
                            sendResponse(exchange, "Success");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                server.createContext("/remove-assign-type", exchange -> {
                    var modelManager = getOWLModelManager();
                    var ontology = modelManager.getActiveOntology();
                    var dataFactory = modelManager.getOWLDataFactory();

                    var qparams = parseQueryParams(exchange);

                    var individualIRI = IRI.create(qparams.get("individualUri"));
                    var typeIRI = IRI.create(qparams.get("typeUri"));

                    var individual = dataFactory.getOWLNamedIndividual(individualIRI);
                    var cls = dataFactory.getOWLClass(typeIRI);

                    var axiom = dataFactory.getOWLClassAssertionAxiom(cls, individual);
                    SwingUtilities.invokeLater(() -> {
                        try {
                            modelManager.applyChange(new RemoveAxiom(ontology, axiom));
                            sendResponse(exchange, "Success");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                server.createContext("/assign-same-individual", exchange -> {
                    var modelManager = getOWLModelManager();
                    var ontology = modelManager.getActiveOntology();
                    var dataFactory = modelManager.getOWLDataFactory();

                    var qparams = parseQueryParams(exchange);

                    var individualIRI = IRI.create(qparams.get("individualUri"));
                    var sameIndividualIRI = IRI.create(qparams.get("sameIndividualUri"));

                    var individual = dataFactory.getOWLNamedIndividual(individualIRI);
                    var same = dataFactory.getOWLNamedIndividual(sameIndividualIRI);

                    var sameAxiom = dataFactory.getOWLSameIndividualAxiom(individual, same);
                    SwingUtilities.invokeLater(() -> {
                        try {
                            modelManager.applyChange(new AddAxiom(ontology, sameAxiom));
                            sendResponse(exchange, "Success");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                server.createContext("/remove-assign-same-individual", exchange -> {
                    var modelManager = getOWLModelManager();
                    var ontology = modelManager.getActiveOntology();
                    var dataFactory = modelManager.getOWLDataFactory();

                    var qparams = parseQueryParams(exchange);

                    var individualIRI = IRI.create(qparams.get("individualUri"));
                    var sameIndividualIRI = IRI.create(qparams.get("sameIndividualUri"));

                    var individual = dataFactory.getOWLNamedIndividual(individualIRI);
                    var same = dataFactory.getOWLNamedIndividual(sameIndividualIRI);

                    var sameAxiom = dataFactory.getOWLSameIndividualAxiom(individual, same);
                    SwingUtilities.invokeLater(() -> {
                        try {
                            modelManager.applyChange(new RemoveAxiom(ontology, sameAxiom));
                            sendResponse(exchange, "Success");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                server.createContext("/assign-different-individual", exchange -> {
                    var modelManager = getOWLModelManager();
                    var ontology = modelManager.getActiveOntology();
                    var dataFactory = modelManager.getOWLDataFactory();

                    var qparams = parseQueryParams(exchange);

                    var individualIRI = IRI.create(qparams.get("individualUri"));
                    var diffrentIRI = IRI.create(qparams.get("differentIndividualUri"));

                    var individual = dataFactory.getOWLNamedIndividual(individualIRI);
                    var different = dataFactory.getOWLNamedIndividual(diffrentIRI);

                    var differentAxiom = dataFactory.getOWLDifferentIndividualsAxiom(individual, different);
                    SwingUtilities.invokeLater(() -> {
                        try {
                            modelManager.applyChange(new AddAxiom(ontology, differentAxiom));
                            sendResponse(exchange, "Success");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                server.createContext("/remove-assign-different-individual", exchange -> {
                    var modelManager = getOWLModelManager();
                    var ontology = modelManager.getActiveOntology();
                    var dataFactory = modelManager.getOWLDataFactory();

                    var qparams = parseQueryParams(exchange);

                    var individualIRI = IRI.create(qparams.get("individualUri"));
                    var diffrentIRI = IRI.create(qparams.get("differentIndividualUri"));

                    var individual = dataFactory.getOWLNamedIndividual(individualIRI);
                    var different = dataFactory.getOWLNamedIndividual(diffrentIRI);

                    var differentAxiom = dataFactory.getOWLDifferentIndividualsAxiom(individual, different);
                    SwingUtilities.invokeLater(() -> {
                        try {
                            modelManager.applyChange(new RemoveAxiom(ontology, differentAxiom));
                            sendResponse(exchange, "Success");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                server.createContext("/assign-object-property-assertion", exchange -> {
                    var modelManager = getOWLModelManager();
                    var ontology = modelManager.getActiveOntology();
                    var dataFactory = modelManager.getOWLDataFactory();

                    var qparams = parseQueryParams(exchange);

                    var individualIRI = IRI.create(qparams.get("individualUri"));
                    var objectPropertyIRI = IRI.create(qparams.get("objectPropertyUri"));
                    var secondIndividualIRI = IRI.create(qparams.get("secondIndividualUri"));

                    var individual = dataFactory.getOWLNamedIndividual(individualIRI);
                    var objectProperty = dataFactory.getOWLObjectProperty(objectPropertyIRI);
                    var second = dataFactory.getOWLNamedIndividual(secondIndividualIRI);

                    var axiom = dataFactory.getOWLObjectPropertyAssertionAxiom(objectProperty, individual, second);
                    SwingUtilities.invokeLater(() -> {
                        try {
                            modelManager.applyChange(new AddAxiom(ontology, axiom));
                            sendResponse(exchange, "Success");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                server.createContext("/remove-assign-object-property-assertion", exchange -> {
                    var modelManager = getOWLModelManager();
                    var ontology = modelManager.getActiveOntology();
                    var dataFactory = modelManager.getOWLDataFactory();

                    var qparams = parseQueryParams(exchange);

                    var individualIRI = IRI.create(qparams.get("individualUri"));
                    var objectPropertyIRI = IRI.create(qparams.get("objectPropertyUri"));
                    var secondIndividualIRI = IRI.create(qparams.get("secondIndividualUri"));

                    var individual = dataFactory.getOWLNamedIndividual(individualIRI);
                    var objectProperty = dataFactory.getOWLObjectProperty(objectPropertyIRI);
                    var second = dataFactory.getOWLNamedIndividual(secondIndividualIRI);

                    var axiom = dataFactory.getOWLObjectPropertyAssertionAxiom(objectProperty, individual, second);
                    SwingUtilities.invokeLater(() -> {
                        try {
                            modelManager.applyChange(new RemoveAxiom(ontology, axiom));
                            sendResponse(exchange, "Success");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                server.createContext("/assign-negative-object-property-assertion", exchange -> {
                    var modelManager = getOWLModelManager();
                    var ontology = modelManager.getActiveOntology();
                    var dataFactory = modelManager.getOWLDataFactory();

                    var qparams = parseQueryParams(exchange);

                    var individualIRI = IRI.create(qparams.get("individualUri"));
                    var objectPropertyIRI = IRI.create(qparams.get("objectPropertyUri"));
                    var secondIndividualIRI = IRI.create(qparams.get("secondIndividualUri"));

                    var individual = dataFactory.getOWLNamedIndividual(individualIRI);
                    var objectProperty = dataFactory.getOWLObjectProperty(objectPropertyIRI);
                    var second = dataFactory.getOWLNamedIndividual(secondIndividualIRI);

                    var axiom = dataFactory.getOWLNegativeObjectPropertyAssertionAxiom(objectProperty, individual, second);
                    SwingUtilities.invokeLater(() -> {
                        try {
                            modelManager.applyChange(new AddAxiom(ontology, axiom));
                            sendResponse(exchange, "Success");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                server.createContext("/remove-assign-negative-object-property-assertion", exchange -> {
                    var modelManager = getOWLModelManager();
                    var ontology = modelManager.getActiveOntology();
                    var dataFactory = modelManager.getOWLDataFactory();

                    var qparams = parseQueryParams(exchange);

                    var individualIRI = IRI.create(qparams.get("individualUri"));
                    var objectPropertyIRI = IRI.create(qparams.get("objectPropertyUri"));
                    var secondIndividualIRI = IRI.create(qparams.get("secondIndividualUri"));

                    var individual = dataFactory.getOWLNamedIndividual(individualIRI);
                    var objectProperty = dataFactory.getOWLObjectProperty(objectPropertyIRI);
                    var second = dataFactory.getOWLNamedIndividual(secondIndividualIRI);

                    var axiom = dataFactory.getOWLNegativeObjectPropertyAssertionAxiom(objectProperty, individual, second);
                    SwingUtilities.invokeLater(() -> {
                        try {
                            modelManager.applyChange(new RemoveAxiom(ontology, axiom));
                            sendResponse(exchange, "Success");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                server.setExecutor(null);
                server.start();
                System.out.println("✅ Server running at http://localhost:" + port);

                Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        serverThread.setDaemon(true);
        serverThread.start();

        mcpThread.setDaemon(true);
        mcpThread.start();
    }

    private ArrayList<OWLOntologyChange> ProcessCharacteristicChange(ArrayList<OWLOntologyChange> changes, OWLOntology activeOntology, OWLAxiom ax, String value)
    {
        var boolValue = value.toLowerCase();
        if (boolValue.equals("true"))
            changes.add(new AddAxiom(activeOntology, ax));
        else if(boolValue.equals("false"))
            changes.add(new RemoveAxiom(activeOntology, ax));

        return changes;
    }

    private void sendResponse(HttpExchange exchange, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }


    public void dispose() {
    }

    public void actionPerformed(ActionEvent event) {
    }

    private Map<String, String> parseQueryParams(HttpExchange exchange) {
        Map<String, String> result = new HashMap<>();
        String query = exchange.getRequestURI().getQuery();
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
                        System.out.println("Error decoding URL parameter" + e);
                    }
                }
            }
        }
        return result;
    }
}