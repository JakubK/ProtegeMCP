package com.protegeplugin;

import java.util.List;

public class IndividualDetails {
    public String uri;
    public List<String> types;
    public List<String> sameIndividuals;
    public List<String> differentIndividuals;
    public List<String> objectPropertyAssertions;
    public List<String> negativeObjectPropertyAssertions;

    public IndividualDetails(String uri, List<String> types, List<String> sameIndividuals, List<String> differentIndividuals, List<String> objectPropertyAssertions, List<String> negativeObjectPropertyAssertions)
    {
        this.uri = uri;
        this.types = types;
        this.sameIndividuals = sameIndividuals;
        this.differentIndividuals = differentIndividuals;
        this.objectPropertyAssertions = objectPropertyAssertions;
        this.negativeObjectPropertyAssertions = negativeObjectPropertyAssertions;
    }
}
