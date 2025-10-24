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
}