using System.ComponentModel;
using ModelContextProtocol.Server;

namespace ProtegeMCP.Server.Tools;

[McpServerToolType]
public class ProtegeTools
{
    [McpServerTool(Name = "list-concepts")]
    [Description("List all Concepts present in current Ontology")]
    public static async Task<string> ListConceptsAsync()
    {
        return "";
    }
}