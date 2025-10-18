using System.ComponentModel;
using ModelContextProtocol.Server;

namespace ProtegeMCP.Server.Tools;

[McpServerToolType]
public class IOTools
{
    [McpServerTool(Name = "new-ontology")]
    [Description("Creates new blank ontology and switches workspace to it")]
    public static async Task<string> NewOntologyAsync(HttpClient client)
    {
        return "";
    }
    
    [McpServerTool(Name = "open-ontology")]
    [Description("Opens ontology from given path and switches Protege workspace to it")]
    public static async Task<string> OpenOntologyAsync(HttpClient client)
    {
        return "";
    }
    
    [McpServerTool(Name = "save-ontology")]
    [Description("Saves current ontology and fails if it has no file associated")]
    public static async Task<string> SaveOntologyAsync(HttpClient client)
    {
        return "";
    }
    
    [McpServerTool(Name = "save-as-ontology")]
    [Description("Saves current ontology to given file path")]
    public static async Task<string> SaveAsOntologyAsync(HttpClient client)
    {
        return "";
    }
}