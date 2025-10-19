using System.ComponentModel;
using ModelContextProtocol.Server;

namespace ProtegeMCP.Server.Tools;

[McpServerToolType, Description("Tools for CRUD operations on Axioms of Concepts")]
public class AxiomTools
{
    [McpServerTool(Name = "list-axioms")]
    [Description("List all axioms organized by category for given Concept in current Ontology")]
    public static async Task<string> ListConceptsAsync(HttpClient client)
    {
        var response = await client.GetAsync("/list-concept-axioms");
        return await response.Content.ReadAsStringAsync();
    }
}