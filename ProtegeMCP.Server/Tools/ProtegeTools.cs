using System.ComponentModel;
using ModelContextProtocol.Server;

namespace ProtegeMCP.Server.Tools;

[McpServerToolType]
public class ProtegeTools()
{
    [McpServerTool(Name = "list-concepts")]
    [Description("List all Concepts present in current Ontology")]
    public static async Task<string> ListConceptsAsync(ProtegePluginClient client)
    {
        return await client.ListConceptsAsync();
    }
    
    [McpServerTool(Name = "create-concept")]
    [Description(@"
        Allows to create new Concept in current Ontology. Returns status string which informs if operation was successful.
    ")]
    public static async Task<string> CreateConceptAsync(ProtegePluginClient client,
        [Description("URI of the concept to be created. Example value: http://www.example.org/animals#Mammal")] string conceptUri
    )
    {
        return await client.CreateConceptAsync(new (conceptUri));
    }

    [McpServerTool(Name = "subclass-concept")]
    [Description(@"
        Changes parent of given concept in current Ontology.
        Both given concept URIs must already exist in the Ontology.
    ")]
    public static async Task<string> SubclassConceptAsync(ProtegePluginClient client,
        [Description("URI of the child concept. Example value: http://www.example.org/animals#Mammal")] string childUri,
        [Description("URI of the parent concept. When skipped, the owl:Thing will become a parent")] string? parentUri)
    {
        return await client.SubclassConceptAsync(new (childUri, parentUri));
    }
}
