using System.Net.Http.Json;
using System.Text.Json;
using ProtegeMCP.Server.Requests;

namespace ProtegeMCP.Server;

public class ProtegePluginClient(HttpClient httpClient)
{
    public async Task<IEnumerable<string>> ListConceptsAsync()
    {
        return await httpClient.GetFromJsonAsync<IEnumerable<string>>("/concepts", JsonSerializerOptions.Web) ?? [];
    }

    public async Task<string> CreateConceptAsync(CreateNewConceptRequest request)
    {
        var response = await httpClient.PostAsJsonAsync("/concepts", request, JsonSerializerOptions.Web);
        return await response.Content.ReadAsStringAsync();
    }
}