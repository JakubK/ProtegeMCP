using System.ComponentModel;
using Microsoft.AspNetCore.WebUtilities;
using ModelContextProtocol.Server;

namespace ProtegeMCP.Server.Tools;

[McpServerToolType, Description("Tools for CRUD operations on Axioms of Concepts")]
public class ConceptAxiomTools
{
    [McpServerTool(Name = "list-concept-axioms")]
    [Description("List all axioms organized by category for given Concept in current Ontology")]
    public static async Task<string> ListConceptAxioms(HttpClient client,
        [Description("uri: URI of the concept to list its axioms. Example value: http://www.example.org/animals#Mammal")] string uri)
    {
        var query = new Dictionary<string, string?>
        {
            ["uri"] = uri
        };
        var url = QueryHelpers.AddQueryString("/list-concept-axioms", query);
        var response = await client.GetAsync(url);
        return await response.Content.ReadAsStringAsync();
    }

    [McpServerTool(Name = "add-concept-axiom")]
    [Description("""
         Assign Axiom to concept
         Concepts or Object Properties in expression should be mentioned via IRI's wrapped in <>
    """)]
    public static async Task<string> AddConceptAxiom(HttpClient client, 
        [Description("uri: URI of the concept to add axiom to. Example value: http://www.example.org/animals#Mammal")] string uri,
        [Description("axiomKind: Kind of Axiom to be added. Allowed values are: ['equivalentClass', 'subClass', 'disjointClass', 'disjointUnionClass']")] string axiomKind,
        [Description("classExpression: Class Expression of axiom to be added")] string classExpression)
    {
        var query = new Dictionary<string, string?>
        {
            ["uri"] = uri,
            ["axiomKind"] = axiomKind,
            ["classExpression"] = classExpression
        };
        var url = QueryHelpers.AddQueryString("/add-concept-axiom", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "remove-concept-axiom")]
    [Description("""
         Remove axiom from concept
         Concepts in expression should be mentioned via IRI's wrapped in <>
    """)]
    public static async Task<string> RemoveConceptAxiom(HttpClient client,
        [Description("uri: URI of the concept to remove axiom from. Example value: http://www.example.org/animals#Mammal")] string uri,
        [Description("axiomKind: Kind of Axiom to be removed. Allowed values are: ['equivalentClass', 'subClass', 'disjointClass', 'disjointUnionClass']")] string axiomKind,
        [Description("axiom: Manchester OWL Syntax axiom to be removed")] string axiom)
    {
        var query = new Dictionary<string, string?>
        {
            ["uri"] = uri,
            ["axiomKind"] = axiomKind,
            ["axiom"] = axiom
        };
        var url = QueryHelpers.AddQueryString("/remove-concept-axiom", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }
}