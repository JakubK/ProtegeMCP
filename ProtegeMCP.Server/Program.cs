using Microsoft.AspNetCore.Builder;
using Microsoft.Extensions.DependencyInjection;
using ProtegeMCP.Server.Tools;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddMcpServer()
    .WithTools<ConceptTools>()
    .WithTools<IOTools>()
    .WithTools<ConceptAxiomTools>()
    .WithTools<ObjectPropertiesTools>()
    .WithTools<ObjectPropertyAxiomTools>()
    .WithStdioServerTransport()
    .WithHttpTransport();

using var httpClient = new HttpClient();
httpClient.BaseAddress = new Uri("http://localhost:8080");
builder.Services.AddSingleton(httpClient);

var app = builder.Build();
app.MapMcp();
await app.RunAsync();