using System.ComponentModel;
using Microsoft.AspNetCore.WebUtilities;
using ModelContextProtocol.Server;

namespace ProtegeMCP.Server.Tools;

[McpServerToolType, Description("Tools for CRUD operations on Object Properties")]
public class ObjectPropertiesTools
{
    [McpServerTool(Name = "create-object-property")]
    [Description(@"
        Allows to create new Object Property in current Ontology. Returns status string which informs if operation was successful.
        Example Payload:
        {
            'uri': 'http://www.example.org/animals#property'
        }
    ")]
    public static async Task<string> CreateObjectProperty(HttpClient client,
        [Description("uri: URI of the object property to be created. Example value: http://www.example.org/animals#Mammal")] string uri
    )
    {
        var query = new Dictionary<string, string?>
        {
            ["uri"] = uri
        };
        var url = QueryHelpers.AddQueryString("/create-object-property", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "list-object-properties")]
    [Description("List all Object Properties present in current Ontology")]
    public static async Task<string> ListObjectProperties(HttpClient client)
    {
        var response = await client.GetAsync("/list-object-properties");
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "rename-object-property")]
    [Description(@"
        Allows to rename already existing Object Property to new one. Returns status string which informs if operation was successful.
        Example Payload:
        {
            'oldUri': 'http://www.example.org/animals#Mammal',
            'newName': 'http://www.example.org/animals#SomethingElse'
        }
    ")]
    public static async Task<string> RenameObjectProperty(HttpClient client,
        [Description("oldUri: URI of the Object Property to be renamed. Example value: http://www.example.org/animals#Mammal")] string oldUri,
        [Description("newUri: New URI of Object Property. Example value: http://www.example.org/animals#SomethingElse")] string newUri
    )
    {
        var query = new Dictionary<string, string?>
        {
            ["oldUri"] = oldUri,
            ["newUri"] = newUri
        };
        var url = QueryHelpers.AddQueryString("/rename-object-property", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "delete-object-property")]
    [Description(@"
        Allows to delete object property
        Example Payload:
        {
            'uri': 'http://www.example.org/animals#Mammal',
        }
    ")]
    public static async Task<string> DeleteObjectProperty(HttpClient client,
        [Description("uri: Uri of object property to be deleted. Example value: http://www.example.org/animals#Mammal")] string uri
    )
    {
        var query = new Dictionary<string, string?>
        {
            ["uri"] = uri
        };
        var url = QueryHelpers.AddQueryString("/delete-object-property", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }

    [McpServerTool(Name = "set-object-property-characteristics")]
    [Description("Allows to modify characteristics of given Object Property.")]
    public static async Task<string> SetObjectPropertyCharacterictics(HttpClient client,
        [Description("Uri of Object Property being modified. Example value: http://www.example.org/animals#Mammal")] string uri, 
        [Description("Whether the Object Property should be Functional or not. Defaults to null which means the setting wont be touched by this tool")] bool? functional,
        [Description("Whether the Object Property should be InverseFunctional or not. Defaults to null which means the setting wont be touched by this tool")] bool? inverseFunctional,
        [Description("Whether the Object Property should be Transitive or not. Defaults to null which means the setting wont be touched by this tool")] bool? transitive,
        [Description("Whether the Object Property should be Symmetric or not. Defaults to null which means the setting wont be touched by this tool")] bool? symmetric,
        [Description("Whether the Object Property should be Asymmetric or not. Defaults to null which means the setting wont be touched by this tool")] bool? asymmetric,
        [Description("Whether the Object Property should be Reflexive or not. Defaults to null which means the setting wont be touched by this tool")] bool? reflexive,
        [Description("Whether the Object Property should be Irreflexive or not. Defaults to null which means the setting wont be touched by this tool")] bool? irreflexive)
    {
        var query = new Dictionary<string, string?>
        {
            ["uri"] = uri,
            ["functional"] = functional is null ? "" : functional.ToString(),
            ["inverseFunctional"] = inverseFunctional is null ? "" : inverseFunctional.ToString(),
            ["transitive"] = transitive is null ? "" : transitive.ToString(),
            ["symmetric"] = symmetric is null ? "" : symmetric.ToString(),
            ["asymmetric"] = asymmetric is null ? "" : asymmetric.ToString(),
            ["reflexive"] = reflexive is null ? "" : reflexive.ToString(),
            ["irreflexive"] = irreflexive is null ? "" : irreflexive.ToString(),
            
        };
        var url = QueryHelpers.AddQueryString("/set-object-property-characteristics", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }
}