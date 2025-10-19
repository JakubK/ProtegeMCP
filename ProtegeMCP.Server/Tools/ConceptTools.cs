using System.ComponentModel;
using Microsoft.AspNetCore.WebUtilities;
using ModelContextProtocol.Server;

namespace ProtegeMCP.Server.Tools;

[McpServerToolType, Description("Tools for CRUD operations on Concepts")]
public class ConceptTools
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
        Example Payload:
        {
            'conceptUri': 'http://www.example.org/animals#Mammal'
        }
    ")]
    public static async Task<string> CreateConceptAsync(HttpClient client,
        [Description("URI of the concept to be created. Example value: http://www.example.org/animals#Mammal")] string conceptUri
    )
    {
        var query = new Dictionary<string, string?>
        {
            ["uri"] = conceptUri
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
    public static async Task<string> RenameConceptAsync(HttpClient client,
        [Description("URI of the Concept to be renamed. Example value: http://www.example.org/animals#Mammal")] string oldUri,
        [Description("New URI of Concept. Example value: http://www.example.org/animals#Mammal")] string newUri
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
    public static async Task<string> DeleteConceptAsync(HttpClient client,
        [Description("URI of the concept to be deleted. Example value: http://www.example.org/animals#Mammal")] string conceptUri
    )
    {
        var query = new Dictionary<string, string?>
        {
            ["conceptUri"] = conceptUri,
        };
        var url = QueryHelpers.AddQueryString("/delete-concept", query);
        var response = await client.DeleteAsync(url);
        return await response.Content.ReadAsStringAsync();
    }

    [McpServerTool(Name = "subclass-concept")]
    [Description(@"
        Changes parent of given concept in current Ontology.
        Both given concept URIs must already exist in the Ontology.
        Example Payload:
        {
            'childUrl': 'http://www.example.org/animals#Mammal',
            'parentUri': 'http://www.example.org/animals#Parent'
        }
    ")]
    public static async Task<string> SubclassConceptAsync(HttpClient client,
        [Description("URI of the child concept. Example value: http://www.example.org/animals#Mammal")] string childUri,
        [Description("URI of the parent concept. When skipped, the owl:Thing will become a parent")] string? parentUri)
    {
        var query = new Dictionary<string, string?>
        {
            ["childUri"] = childUri,
            ["parentUri"] = parentUri,
        };
        var url = QueryHelpers.AddQueryString("/subclass-concept", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }
}
