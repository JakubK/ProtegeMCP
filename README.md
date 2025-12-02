# ProtegeMCP

Proof of Concept of [Protege](https://protege.stanford.edu/) Plugin which starts Model Context Protocol server for Protege, allowing you to work on your ontology and query it with your Claude Desktop or any MCP compatible LLM/Agent.


## Requirements

- .NET 10
- JavaSE-11+

## How it works

At its core its just Protege plugin, serving Protege features via simple REST API.
In final artifact plugin jar there is embedded MCP Server developed in dotnet which is consuming mentioned REST API.
I had few attempts to have just java MCP Server, but I'm not very proficient with Java's ecosystem and dependency management, hence the dotnet + java approach.

## Capabilities

All MCP Server capabilities are exposed as Tools.

### Concepts - 4 tools

- `list-concepts` - List all concepts present in current Ontology
- `create-concept` - Create new concept
- `rename-concept` - Rename already existing concept
- `delete-concept` - Deletes/Removes concept from current Ontology

### Concept Axioms - 3 tools

- `list-concept-axioms` - List all axioms organized by category for given Concept in current Ontology
- `add-concept-axiom` - Assign Axiom to concept (equivalentClass, subClass, disjointClass, disjointUnionClass)
- `remove-concept-axiom` - Removes assigned Axiom from concept (equivalentClass, subClass, disjointClass, disjointUnionClass)

### Object Properties - 5 tools

- `list-object-properties` - List all Object Properties present in current Ontology
- `create-object-property` - Create new Object Property
- `rename-object-property` - Rename already existing Object Property
- `delete-object-property` - Deletes/Removes Object Property from current Ontology
- `set-object-property-characteristics` - Set/Reset characteristics of given Object Property (functional, inverseFunctional, transitive, symmetric, asymmetric, reflexive, irreflexive)

### Object Property Axioms - 3 tools

- `list-object-property-axioms` - List all Object Property axioms for given Object Property organized by category (equivalentTo, subPropertyOf, inverseOf, domains, range, disjointWith, superPropertyOf) 
- `add-object-property-axiom` - Add Object Property axiom
- `delete-object-property-axiom` - Remove Object Property axiom

### Individuals - 4 tools

- `list-individuals` - List all Individuals present in current Ontology
- `create-individual` - Create new Individual
- `delete-individual` - Delete individual
- `rename-individual` - Rename individual


### Individual Properties - 10 tools

- `assign-type` - Assign type (concept) to given individual
- `remove-type` - Remove type assignment
- `assign-same-individual` - Denote that 2 individuals are actually the same
- `remove-same-individual` - Remove same individual assignment
- `assign-different-individual` - Denote that 2 individuals are separate ones
- `remove-different-individual` - Remove different individuals assignment
- `assign-object-property-assertion` - Add Object Property assertion to individual
- `remove-object-property-assertion` - Remove Object Property assertion
- `assign-negative-object-property-assertion` - Add Negative Object Property Assertion
- `remove-negative-object-property-assertion` - Remove Negative Object Property Assertion

### IO - 4 tools

- `get-current-ontology` - Get the information about current ontology. Useful when working with multiple ones to make sure correct one gets updated
- `new-ontology` - Create new window with new empty Ontology
- `open-ontology` - Opens Ontology from given file (supported by Protege). Switches current ontology to it.  
- `save-as-ontology` - Saves the ontology
