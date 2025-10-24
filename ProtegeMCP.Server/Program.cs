// See https://aka.ms/new-console-template for more information

using Microsoft.AspNetCore.Builder;
using Microsoft.Extensions.DependencyInjection;
using ProtegeMCP.Server.Tools;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddMcpServer()
    .WithTools<ConceptTools>()
    .WithTools<IOTools>()
    .WithTools<AxiomTools>()
    .WithStdioServerTransport()
    .WithHttpTransport();

using var httpClient = new HttpClient();
httpClient.BaseAddress = new Uri("http://localhost:8080");
builder.Services.AddSingleton(httpClient);

var app = builder.Build();
app.MapMcp();
await app.RunAsync();