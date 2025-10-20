# ProtegeMCP

[Protege](https://protege.stanford.edu/) Plugin which starts Model Context Protocol server for Protege, allowing you to work on your ontology and query it with your Claude Desktop or any MCP compatible LLM/Agent.

## How it works

At its core its just Protege plugin, serving Protege features via simple REST API.
In final artifact plugin jar there is embedded MCP Server developed in dotnet which is consuming mentioned REST API.
I had few attempts to have just java MCP Server, but I'm not very proficient with Java's ecosystem and dependency management, hence the dotnet + java approach.
