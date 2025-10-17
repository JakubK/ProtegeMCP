using System.ComponentModel;
using ModelContextProtocol.Server;

namespace ProtegeMCP.Server.Tools;

[McpServerToolType]
public class ProtegeTools()
{
    [McpServerTool(Name = "list-concepts")]
    [Description("List all Concepts present in current Ontology")]
    public static async Task<IEnumerable<string>> ListConceptsAsync(ProtegePluginClient client)
    {
        return await client.ListConceptsAsync();
    }
    
    [McpServerTool(Name = "create-concept")]
    [Description("Allows to create new Concept in current Ontology. Returns status string which informs if operation was successful")]
    public static async Task<string> CreateConceptAsync(ProtegePluginClient client,
        [Description("URI of the concept to be created. Example value: http://www.example.org/animals#Mammal")] string conceptUri)
    {
        return await client.CreateConceptAsync(new (conceptUri));
    }
}
