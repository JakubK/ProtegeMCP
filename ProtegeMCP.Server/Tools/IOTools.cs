using System.ComponentModel;
using Microsoft.AspNetCore.WebUtilities;
using ModelContextProtocol.Server;

namespace ProtegeMCP.Server.Tools;

[McpServerToolType,  Description("Tools for IO operations on ontology saved on filesystem")]
public class IOTools
{
    [McpServerTool(Name = "get-current-ontology")]
    [Description("Returns information about what is the current Ontology")]
    public static async Task<string> GetCurrentOntology(HttpClient client)
    {
        var response = await client.GetAsync("/get-current-ontology");
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "new-ontology")]
    [Description("Creates new blank ontology and switches workspace to it")]
    public static async Task<string> NewOntology(HttpClient client)
    {
        var response = await client.PostAsync("/new", null);
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "open-ontology")]
    [Description(@"
        Opens ontology from given path and switches Protege workspace to it
        Example payload:
        {
            'path': '/home/user/ontology.owx'
        }
        "
    )]
    public static async Task<string> OpenOntology(HttpClient client, 
        [Description("path: Required path to file with ontology")] string path)
    {
        var query = new Dictionary<string, string?>
        {
            ["path"] = path
        };
        var url = QueryHelpers.AddQueryString("/open", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "save-as-ontology")]
    [Description(@"
        Saves current ontology to specified path or just saves if not specified
        Example payload:
        {
           'path': '/home/user/ontology.owx'
        }
    ")]
    public static async Task<string> SaveAsOntology(HttpClient client,
        [Description("path: Location on the file system specifying where to save ontology. If left empty, it will assume that the currently opened ontology is already associated with a file")] string? path)
    {
        var query = new Dictionary<string, string?>
        {
            ["path"] = path
        };
        var url = QueryHelpers.AddQueryString("/save", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }
}