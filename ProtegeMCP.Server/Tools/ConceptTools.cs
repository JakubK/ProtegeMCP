using System.ComponentModel;
using Microsoft.AspNetCore.WebUtilities;
using ModelContextProtocol.Server;

namespace ProtegeMCP.Server.Tools;

[McpServerToolType, Description("Tools for CRUD operations on Concepts")]
public class ConceptTools
{
    [McpServerTool(Name = "list-concepts")]
    [Description("List all Concepts present in current Ontology")]
    public static async Task<string> ListConcepts(HttpClient client)
    {
        var response = await client.GetAsync("/concepts");
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "create-concept")]
    [Description(@"
        Allows to create new Concept in current Ontology. Returns status string which informs if operation was successful.
        Example Payload:
        {
            'uri': 'http://www.example.org/animals#Mammal'
        }
    ")]
    public static async Task<string> CreateConcept(HttpClient client,
        [Description("uri: URI of the concept to be created. Example value: http://www.example.org/animals#Mammal")] string uri
    )
    {
        var query = new Dictionary<string, string?>
        {
            ["uri"] = uri
        };
        var url = QueryHelpers.AddQueryString("/concepts", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "rename-concept")]
    [Description(@"
        Allows to rename already existing Concept to new one. Returns status string which informs if operation was successful.
        Example Payload:
        {
            'oldUri': 'http://www.example.org/animals#Mammal',
            'newName': 'http://www.example.org/animals#SomethingElse'
        }
    ")]
    public static async Task<string> RenameConcept(HttpClient client,
        [Description("oldUri: URI of the Concept to be renamed. Example value: http://www.example.org/animals#Mammal")] string oldUri,
        [Description("newUri: New URI of Concept. Example value: http://www.example.org/animals#Mammal")] string newUri
    )
    {
        var query = new Dictionary<string, string?>
        {
            ["oldUri"] = oldUri,
            ["newUri"] = newUri
        };
        var url = QueryHelpers.AddQueryString("/rename-concept", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "delete-concept")]
    [Description(@"
        Allows to delete Concept from current Ontology. Returns status string which informs if operation was successful.
        Example Payload:
        {
            'conceptUri': 'http://www.example.org/animals#Mammal'
        }
    ")]
    public static async Task<string> DeleteConcept(HttpClient client,
        [Description("uri: URI of the concept to be deleted. Example value: http://www.example.org/animals#Mammal")] string uri
    )
    {
        var query = new Dictionary<string, string?>
        {
            ["uri"] = uri,
        };
        var url = QueryHelpers.AddQueryString("/delete-concept", query);
        var response = await client.DeleteAsync(url);
        return await response.Content.ReadAsStringAsync();
    }
}
