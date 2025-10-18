namespace ProtegeMCP.Server.Requests.Concept;

public record SubclassConceptRequest(string ChildUri, string? ParentUri);