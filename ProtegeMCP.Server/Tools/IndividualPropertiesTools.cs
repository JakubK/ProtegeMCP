using System.ComponentModel;
using Microsoft.AspNetCore.WebUtilities;
using ModelContextProtocol.Server;

namespace ProtegeMCP.Server.Tools;

[McpServerToolType, Description("Tools for CRUD operations on Properties of given Individual")]
public class IndividualPropertiesTools
{
    [McpServerTool(Name = "assign-type")]
    [Description("Assigns given Type to given Individual")]
    public static async Task<string> AssignType(HttpClient client,
        [Description("Uri of type")] string typeUri,
        [Description("Uri of individual")] string individualUri)
    {
        var query = new Dictionary<string, string?>
        {
            ["typeUri"] = typeUri,
            ["individualUri"] = individualUri,
        };
        var url = QueryHelpers.AddQueryString("/assign-type", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "remove-assign-type")]
    [Description("Removes Type assigned to given Individual")]
    public static async Task<string> RemoveTypeAssign(HttpClient client,
        [Description("Uri of type")] string typeUri,
        [Description("Uri of individual")] string individualUri)
    {
        var query = new Dictionary<string, string?>
        {
            ["typeUri"] = typeUri,
            ["individualUri"] = individualUri,
        };
        var url = QueryHelpers.AddQueryString("/remove-assign-type", query);
        var response = await client.DeleteAsync(url);
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "assign-same-individual")]
    [Description("Assigns SameIndividual assertion to given Individual")]
    public static async Task<string> AssignSameIndividual(HttpClient client,
        [Description("Uri of individual")] string individualUri,
        [Description("Uri of same individual")] string sameIndividualUri)
    {
        var query = new Dictionary<string, string?>
        {
            ["individualUri"] = individualUri,
            ["sameIndividualUri"] = sameIndividualUri,
        };
        var url = QueryHelpers.AddQueryString("/assign-same-individual", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "remove-assign-same-individual")]
    [Description("Removes Assignment of SameIndividual")]
    public static async Task<string> RemoveAssignSameIndividual(HttpClient client,
        [Description("Uri of individual")] string individualUri,
        [Description("Uri of same individual")] string sameIndividualUri)
    {
        var query = new Dictionary<string, string?>
        {
            ["individualUri"] = individualUri,
            ["sameIndividualUri"] = sameIndividualUri,
        };
        var url = QueryHelpers.AddQueryString("/remove-assign-same-individual", query);
        var response = await client.DeleteAsync(url);
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "assign-different-individual")]
    [Description("Assigns DifferentIndividual assertion to given Individual")]
    public static async Task<string> AssignDifferentIndividual(HttpClient client,
        [Description("Uri of individual")] string individualUri,
        [Description("Uri of different individual")] string differentIndividualUri)
    {
        var query = new Dictionary<string, string?>
        {
            ["individualUri"] = individualUri,
            ["differentIndividualUri"] = differentIndividualUri,
        };
        var url = QueryHelpers.AddQueryString("/assign-different-individual", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "remove-assign-different-individual")]
    [Description("Removes Assignment of DifferentIndividual")]
    public static async Task<string> RemoveAssignDifferentIndividual(HttpClient client,
        [Description("Uri of individual")] string individualUri,
        [Description("Uri of different individual")] string differentIndividual)
    {
        var query = new Dictionary<string, string?>
        {
            ["individualUri"] = individualUri,
            ["differentIndividual"] = differentIndividual,
        };
        var url = QueryHelpers.AddQueryString("/remove-assign-different-individual", query);
        var response = await client.DeleteAsync(url);
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "assign-object-property-assertion")]
    [Description("Assigns Object Property Assertion to given Individual")]
    public static async Task<string> AssignObjectPropertyAssertion(HttpClient client,
        [Description("Uri of individual")] string individualUri,
        [Description("Uri of ObjectProperty")] string objectPropertyUri,
        [Description("Uri of Second Individual")] string secondIndividualUri)
    {
        var query = new Dictionary<string, string?>
        {
            ["individualUri"] = individualUri,
            ["objectPropertyUri"] = objectPropertyUri,
            ["secondIndividualUri"] = secondIndividualUri
        };
        var url = QueryHelpers.AddQueryString("/assign-object-property-assertion", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "remove-assign-object-property-assertion")]
    [Description("Removes assignment of Object Property Assertion from given Individual")]
    public static async Task<string> RemoveAssignObjectPropertyAssertion(HttpClient client,
        [Description("Uri of individual")] string individualUri,
        [Description("Uri of ObjectProperty")] string objectPropertyUri,
        [Description("Uri of Second Individual")] string secondIndividualUri)
    {
        var query = new Dictionary<string, string?>
        {
            ["individualUri"] = individualUri,
            ["objectPropertyUri"] = objectPropertyUri,
            ["secondIndividualUri"] = secondIndividualUri
        };
        var url = QueryHelpers.AddQueryString("/remove-assign-object-property-assertion", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "assign-negative-object-property-assertion")]
    [Description("Assigns Negative Object Property Assertion to given Individual")]
    public static async Task<string> AssignNegativeObjectPropertyAssertion(HttpClient client,
        [Description("Uri of individual")] string individualUri,
        [Description("Uri of ObjectProperty")] string objectPropertyUri,
        [Description("Uri of Second Individual")] string secondIndividualUri)
    {
        var query = new Dictionary<string, string?>
        {
            ["individualUri"] = individualUri,
            ["objectPropertyUri"] = objectPropertyUri,
            ["secondIndividualUri"] = secondIndividualUri
        };
        var url = QueryHelpers.AddQueryString("/assign-negative-object-property-assertion", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }
    
    [McpServerTool(Name = "remove-assign-negative-object-property-assertion")]
    [Description("Removes assignment of Negative Object Property Assertion from given Individual")]
    public static async Task<string> RemoveAssignNegativeObjectPropertyAssertion(HttpClient client,
        [Description("Uri of individual")] string individualUri,
        [Description("Uri of ObjectProperty")] string objectPropertyUri,
        [Description("Uri of Second Individual")] string secondIndividualUri)
    {
        var query = new Dictionary<string, string?>
        {
            ["individualUri"] = individualUri,
            ["objectPropertyUri"] = objectPropertyUri,
            ["secondIndividualUri"] = secondIndividualUri
        };
        var url = QueryHelpers.AddQueryString("/remove-assign-negative-object-property-assertion", query);
        var response = await client.PostAsync(url, null);
        return await response.Content.ReadAsStringAsync();
    }
}