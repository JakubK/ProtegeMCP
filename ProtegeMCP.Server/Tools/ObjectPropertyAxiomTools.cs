using System.ComponentModel;
using Microsoft.AspNetCore.WebUtilities;
using ModelContextProtocol.Server;

namespace ProtegeMCP.Server.Tools;

[McpServerToolType, Description("Tools for CRUD operations on Axioms of Object Properties")]
public class ObjectPropertyAxiomTools
{
    [McpServerTool(Name = "list-object-property-axioms")]
    [Description("List all object-property axioms organized by category for given Object Property in current Ontology")]
    public static async Task<string> ListObjectPropertyAxioms(HttpClient client,
        [Description("uri: URI of the Object Property to list its axioms. Example value: http://www.example.org/animals#Mammal")] string uri)
    {
        var query = new Dictionary<string, string?>
        {
            ["uri"] = uri
        };
        var url = QueryHelpers.AddQueryString("/list-object-property-axioms", query);
        var response = await client.GetAsync(url);
        return await response.Content.ReadAsStringAsync();
    }

    [McpServerTool(Name = "add-object-property-axiom")]
    [Description("""
         Assign Axiom to object-property
         Concepts or Object Propertioes in expression should be mentioned via IRI's wrapped in <>
    """)]
    public static async Task<string> AddObjectPropertyAxiom(HttpClient client, 
        [Description("uri: URI of the Object Property to add axiom to. Example value: http://www.example.org/animals#Mammal")] string uri,
        [Description("axiomKind: Kind of Axiom to be added. Allowed values are: ['equivalentTo', 'subPropertyOf', 'inverseOf', 'domains', 'ranges', 'disjointWith', 'superPropertyOf']")] string axiomKind,
        [Description("classExpression: Class Expression of axiom to be added")] string classExpression)
    {
        var query = new Dictionary<string, string?>
        {
            ["uri"] = uri,
            ["axiomKind"] = axiomKind,
            ["classExpression"] = classExpression
        };
        var url = QueryHelpers.AddQueryString("/add-object-property-axiom", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }   
    
    [McpServerTool(Name = "delete-object-property-axiom")]
    [Description("""
         Remove axiom from concept
         Concepts in expression should be mentioned via IRI's wrapped in <>
    """)]
    public static async Task<string> DeleteConceptAxiom(HttpClient client,
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
        var url = QueryHelpers.AddQueryString("/delete-object-property-axiom", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }
}