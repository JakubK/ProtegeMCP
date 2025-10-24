package com.protegeplugin;

import java.util.List;

public class ListConceptAxiomsResponse {
    public List<String> equivalentClasses;
    public List<String> subClasses;
    public List<String> disjointClasses;
    public List<String> disjointUnionClasses;
    public List<String> hasKeyClasses;


    public ListConceptAxiomsResponse(List<String> equivalentClasses, List<String> subClasses, List<String> disjointClasses, List<String> disjointUnionClasses, List<String> hasKeyClasses) {
        this.equivalentClasses = equivalentClasses;
        this.subClasses = subClasses;
        this.disjointClasses = disjointClasses;
        this.disjointUnionClasses = disjointUnionClasses;
        this.hasKeyClasses = hasKeyClasses;
    }
}