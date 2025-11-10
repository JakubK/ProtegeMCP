using System.ComponentModel;
using Microsoft.AspNetCore.WebUtilities;
using ModelContextProtocol.Server;

namespace ProtegeMCP.Server.Tools;

[McpServerToolType, Description("Tools for CRUD operations on Axioms of Concepts")]
public class AxiomTools
{
    [McpServerTool(Name = "list-concept-axioms")]
    [Description("List all axioms organized by category for given Concept in current Ontology")]
    public static async Task<string> ListConceptsAsync(HttpClient client,
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
    [Description("Assign Axiom for concept using Descriptive Logic expression")]
    public static async Task<string> AddConceptAxiom(HttpClient client, 
        [Description("uri: URI of the concept to add axiom to. Example value: http://www.example.org/animals#Mammal")] string uri,
        [Description("axiomKind: Kind of Axiom to be added. Allowed values are: ['equivalentClass', 'subClass', 'disjointClass', 'disjointUnionClass']")] string axiomKind,
        [Description("dlQuery: Expression in Descriptive Logic for axiom to be made. Valid dlQuery can be just a className or query in descriptive logic without specifying again the information present in axiomKind")] string dlQuery)
    {
        var query = new Dictionary<string, string?>
        {
            ["uri"] = uri,
            ["axiomKind"] = axiomKind,
            ["dlQuery"] = dlQuery
        };
        var url = QueryHelpers.AddQueryString("/add-concept-axiom", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }
}