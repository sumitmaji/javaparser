# Technical Specification: DiagOps MCP Pack Architecture

This document defines the technical structure, syntax, and dependencies for all components within a DiagOps MCP diagnostic pack.

## Directory Structure

A diagnostic pack is scoped to a specific problem area (a "Domain") and is explicitly versioned (sematic versioning).
```
data/diagops_mcp/packs/<domain_id>/
├── domain.json
└── <version>/
    ├── bundles/
    ├── commands/
    ├── playbooks/
    ├── policies/
    └── schemas/
```

---

## Component Dependency Graph

The components within a pack reference each other to orchestrate a diagnosis.
* **Playbooks** (`playbooks/`) depend on **Command Templates** (`commands/`) and **Schemas** (`schemas/`).
* **Bundles** (`bundles/`) depend on **Command Templates** (`commands/`).
* **Policies** (`policies/`) implicitly apply to **Command Templates** and **Playbooks** based on their rules (`mutates`, `risk`, etc.).

---

## 1. Domain Configuration (`domain.json`)

**Path:** `packs/<domain_id>/domain.json`
Defines the metadata for the diagnostic domain.

### Syntax
```json
{
  "id": "string (unique identifier, e.g., 'network')", 
  "title": "string (human readable title)",
  "description": "string (detailed description of what this pack diagnoses)"
}
```

---

## 2. Input Schemas (`schemas/*.json`)

**Path:** `packs/<domain_id>/<version>/schemas/<schema_name>.json`
Defines the JSON Schema for the inputs required by a playbook. This ensures the user provides correct parameters (like `host`, `port`, `namespace`) before execution.

### Syntax
Standard JSON Schema (Draft-07).
```json
{
  "type": "object",
  "properties": {
    "target": {
      "type": "object",
      "properties": {
        "host": {"type": "string"},
        "port": {"type": "integer"}
      },
      "required": ["host"]
    }
  }
}
```

### Reference Point
Referenced by `Playbook.inputs_schema_ref`.

---

## 3. Command Templates (`commands/*.json`)

**Path:** `packs/<domain_id>/<version>/commands/<command_id>.json`
Defines the atomic execution unit (a CLI command), safety boundaries (allowlist), and parser rules to extract facts from output.

### Syntax
```json
{
  "spec_version": "1.0",
  "id": "string (unique across domain, e.g., 'linux.curl')",
  "env": "string ('linux', 'docker', 'kubernetes', 'vm')",
  "title": "string",
  "purpose": "string",
  "risk": "string ('low', 'medium', 'high')",
  "mutates": "boolean (does this command alter state?)",
  "needs_sudo": "boolean",
  "sensitive_output": "boolean",
  "limits": {
    "timeout_seconds": "integer (default: 20)",
    "max_output_kb": "integer (default: 64)"
  },
  "allowlist": {
    "binary": "string (the executable, e.g., 'curl')",
    "allowed_flags": ["list of strings (e.g., '-I', '-v')"],
    "forbidden_substrings": ["list of strings (e.g., 'eval', '|', '&')"]
  },
  "command": {
    "args": ["list of strings (e.g., 'curl', '-v', '{{target.host}}')"]
  },
  "parsers": [
    {
      "id": "string",
      "kind": "string ('regex', 'jsonpath', 'exit_code', 'contains')",
      "source": "string ('stdout' or 'stderr')",
      "match": "string (the matching pattern)",
      "emit": {
        "fact.name": "string (value to emit, e.g., '$1' for regex capture groups)"
      }
    }
  ],
  "what_to_look_for": ["list of strings"],
  "expected_signals": ["list of strings"]
}
```

### Variable Injection Syntax
Within the `"command" : { "args": [] }` list, you can use `{{variable.name}}` syntax. These map directly to the keys defined in your Input Schema.
Example: `{{target.host}}`.

### Reference Point
Referenced by `Playbook.steps[].commands[].template_id` and `Bundle.commands[]`.

---

## 4. Playbooks (`playbooks/*.json`)

**Path:** `packs/<domain_id>/<version>/playbooks/<playbook_id>.json`
Defines the State Machine for a diagnostic sequence.

### Syntax
```json
{
  "spec_version": "1.0",
  "id": "string (e.g., 'troubleshoot-dns')",
  "domain": "string (matches domain.json id)",
  "title": "string",
  "description": "string",
  "supported_envs": ["list of strings ('linux', 'kubernetes', etc.)"],
  "entry_step_id": "string (the id of the starting step)",
  "inputs_schema_ref": "string (filename in schemas/ folder, e.g., 'dns-inputs.json')",
  "steps": [
    {
      "id": "string (step identifier)",
      "title": "string",
      "goal": "string",
      "risk": "string ('low', 'medium', 'high')",
      "commands": [
        {
          "template_id": "string (references a command template id)",
          "when": "string (Optional python-like expression, e.g., 'inputs.target.host != None')",
          "overrides": {
            "key.name": "value to override input schema defaults"
          }
        }
      ],
      "signals": [
        {
          "id": "string",
          "kind": "string ('regex', 'jsonpath', 'exit_code', 'contains')",
          "source": "string ('stdout', 'stderr')",
          "match": "string",
          "severity": "string ('info', 'warning', 'critical')",
          "facts": {
            "fact.name": "value"
          }
        }
      ],
      "transitions": [
        {
          "to_step_id": "string (next step.id to transition to)",
          "when": "string (Python-like expression using facts, e.g., 'facts[\"fact.name\"] == \"value\"')",
          "confidence": "float (0.0 to 1.0)",
          "message": "string (Reason for transition)"
        }
      ]
    }
  ]
}
```

### Reference Point
Uses `inputs_schema_ref` to dynamically request variables from the user.
Uses `template_id` to refer to specific command templates for execution.

---

## 5. Bundles (`bundles/*.json`)

**Path:** `packs/<domain_id>/<version>/bundles/<bundle_id>.json`
Defines strictly linear snapshot groups of command templates. No signal parsing or transitioning occurs across bundles.

### Syntax
```json
{
  "spec_version": "1.0",
  "id": "string (e.g., 'network-baseline')",
  "title": "string",
  "description": "string",
  "commands": [
    "string (List of Command Template IDs, e.g., 'linux.ping', 'linux.ip_route')"
  ]
}
```

---

## 6. Guard Policies (`policies/default.json`)

**Path:** `packs/<domain_id>/<version>/policies/default.json`
Defines safety and approval rules applied by the DiagOps execution engine before rendering or executing commands.

### Syntax
```json
{
  "spec_version": "1.0",
  "rules": [
    {
      "id": "string (e.g., 'require-ticket-for-mutations')",
      "when": "string (Python-like expression matching Command Template properties, e.g., 'step.mutates == true' or 'step.risk == \"high\"')",
      "require": ["list of strings (e.g., 'ticket_id', 'risk_ack', 'justification')"]
    }
  ]
}
```

### Reference Point
Policies implicitly rely on the `risk`, `mutates`, and `needs_sudo` Boolean flags defined in `commands/*.json`. When a playbook attempts to execute a command matching a policy `"when"` condition, the executor will mandate the client provides the fields defined in `"require"`.
