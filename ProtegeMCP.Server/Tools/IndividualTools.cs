using System.ComponentModel;
using Microsoft.AspNetCore.WebUtilities;
using ModelContextProtocol.Server;

namespace ProtegeMCP.Server.Tools;

[McpServerToolType, Description("Tools for CRUD operations on Individuals")]
public class IndividualTools
{
    [McpServerTool(Name = "list-individuals")]
    [Description("List all Individuals present in current Ontology")]
    public static async Task<string> ListIndividuals(HttpClient client)
    {
        var response = await client.GetAsync("/list-individuals");
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "create-individual")]
    [Description("Creates an Individual in current Ontology")]
    public static async Task<string> CreateIndividual(HttpClient client,
        [Description("uri: URI of the Individual to be created. Example value: http://www.example.org/animals#John")] string uri)
    {
        var query = new Dictionary<string, string?>
        {
            ["uri"] = uri
        };
        var url = QueryHelpers.AddQueryString("/create-individual", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "delete-individual")]
    [Description("Deletes an Individual from current Ontology")]
    public static async Task<string> DeleteIndividual(HttpClient client,
        [Description("uri: URI of the Individual to be deleted. Example value: http://www.example.org/animals#John")] string uri)
    {
        var query = new Dictionary<string, string?>
        {
            ["uri"] = uri,
        };
        var url = QueryHelpers.AddQueryString("/delete-individual", query);
        var response = await client.DeleteAsync(url);
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "rename-individual")]
    [Description(@"
        Allows to rename already existing Individual. Returns status string which informs if operation was successful.
        Example Payload:
        {
            'oldUri': 'http://www.example.org/animals#John',
            'newName': 'http://www.example.org/animals#JohnDoe'
        }
    ")]
    public static async Task<string> RenameIndividual(HttpClient client,
        [Description("oldUri: URI of the Individual to be renamed. Example value: http://www.example.org/animals#John")] string oldUri,
        [Description("newUri: New URI of Individual. Example value: http://www.example.org/animals#JohnDoe")] string newUri
    )
    {
        var query = new Dictionary<string, string?>
        {
            ["oldUri"] = oldUri,
            ["newUri"] = newUri
        };
        var url = QueryHelpers.AddQueryString("/rename-individual", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }
}