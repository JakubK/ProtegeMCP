// See https://aka.ms/new-console-template for more information

using Microsoft.AspNetCore.Builder;
using Microsoft.Extensions.DependencyInjection;
using ProtegeMCP.Server;
using ProtegeMCP.Server.Tools;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddMcpServer()
    .WithTools<ProtegeTools>()
    .WithStdioServerTransport()
    .WithHttpTransport();

builder.Services.AddHttpClient<ProtegePluginClient>(client =>
{
    client.BaseAddress = new Uri("http://localhost:8080");
});

var app = builder.Build();
app.MapMcp();
await app.RunAsync();