package com.protegeplugin;

import org.semanticweb.owlapi.model.OWLObjectProperty;

public class ObjectPropertyWithCharacteristics {
    public OWLObjectProperty objectProperty;
    public boolean functional;
    public boolean inverseFunctional;
    public boolean transitive;
    public boolean symmetric;
    public boolean asymmetric;
    public boolean reflexive;
    public boolean irreflexive;

    public ObjectPropertyWithCharacteristics(
        OWLObjectProperty objectProperty,
        boolean functional,
        boolean inverseFunctional,
        boolean transitive,
        boolean symmetric,
        boolean asymmetric,
        boolean reflexive,
        boolean irreflexive)
    {
        this.objectProperty = objectProperty;
        this.functional = functional;
        this.inverseFunctional = inverseFunctional;
        this.transitive = transitive;
        this.symmetric = symmetric;
        this.asymmetric = asymmetric;
        this.reflexive = reflexive;
        this.irreflexive = irreflexive;
    }
}
