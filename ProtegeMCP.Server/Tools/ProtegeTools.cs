using System.ComponentModel;
using System.Net.Http.Json;
using System.Text.Json;
using ModelContextProtocol.Server;
using ProtegeMCP.Server.Requests;

namespace ProtegeMCP.Server.Tools;

[McpServerToolType]
public class ProtegeTools
{
    [McpServerTool(Name = "list-concepts")]
    [Description("List all Concepts present in current Ontology")]
    public static async Task<string> ListConceptsAsync(HttpClient client)
    {
        var response = await client.GetAsync("/concepts");
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "create-concept")]
    [Description(@"
        Allows to create new Concept in current Ontology. Returns status string which informs if operation was successful.
    ")]
    public static async Task<string> CreateConceptAsync(HttpClient client,
        [Description("URI of the concept to be created. Example value: http://www.example.org/animals#Mammal")] string conceptUri
    )
    {
        var response = await client.PostAsJsonAsync("/concepts", new CreateNewConceptRequest(conceptUri), JsonSerializerOptions.Web);
        return await response.Content.ReadAsStringAsync();
    }

    [McpServerTool(Name = "subclass-concept")]
    [Description(@"
        Changes parent of given concept in current Ontology.
        Both given concept URIs must already exist in the Ontology.
    ")]
    public static async Task<string> SubclassConceptAsync(HttpClient client,
        [Description("URI of the child concept. Example value: http://www.example.org/animals#Mammal")] string childUri,
        [Description("URI of the parent concept. When skipped, the owl:Thing will become a parent")] string? parentUri)
    {
        var response = await client.PostAsJsonAsync("/subclass", new SubclassConceptRequest(childUri, parentUri), JsonSerializerOptions.Web);
        return await response.Content.ReadAsStringAsync();
    }
}
