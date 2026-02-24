# How to Add a Command and Playbook to an Existing Pack

This guide covers how to extend an existing diagnostic pack (e.g., `network`) by adding a new command template, defining an input schema for it, parsing its output to extract facts, and writing a playbook to automate its execution.

## Step 1: Define the Input Schema

If your playbook and commands require specific inputs from the user (e.g., a "hostname" or "port"), you should enforce this format using a JSON Schema.

1. Create a schema file inside the pack's `schemas/` directory.

**File:** `data/diagops_mcp/packs/network/1.0.0/schemas/ping-inputs.json`
```json
{
  "type": "object",
  "properties": {
    "target": {
      "type": "object",
      "properties": {
        "host": {
          "type": "string",
          "description": "The destination IP or hostname"
        },
        "count": {
          "type": "integer",
          "default": 3,
          "description": "Number of pings to send"
        }
      },
      "required": ["host"]
    }
  }
}
```

## Step 2: Add the Command Template

A Command Template (saved in the `commands/` directory) defines the actual CLI command to run, what arguments it takes, and how to extract ("parse") the output.

### Using Inputs in Commands
You can inject inputs from the playbook using the `{{key}}` syntax in the `"args"` array. Given the schema above, we can reference `{{target.host}}` and `{{target.count}}`.

### Parsing Outputs
You can use `"parsers"` to extract facts from `stdout` or `stderr`. Supported parsing kinds are `regex`, `jsonpath`, `exit_code`, and `contains`. The `emit` block specifies what fact variable to store the extracted data in.

**File:** `data/diagops_mcp/packs/network/1.0.0/commands/linux.ping_custom.json`
```json
{
  "spec_version": "1.0",
  "id": "linux.ping_custom",
  "env": "linux",
  "title": "Ping Host",
  "purpose": "Verify simple ICMP reachability to a custom host.",
  "mutates": false,
  "needs_sudo": false,
  "allowlist": {
    "binary": "ping",
    "allowed_flags": ["-c", "-W"]
  },
  "command": {
    "args": ["ping", "-c", "{{target.count}}", "{{target.host}}"]
  },
  "parsers": [
    {
      "id": "parse_packet_loss",
      "kind": "regex",
      "source": "stdout",
      "match": "(\\d+)% packet loss",
      "emit": {
        "network.packet_loss": "$1"
      }
    }
  ],
  "expected_signals": ["0% packet loss"]
}
```

## Step 3: Write the Playbook

Playbooks live in the `playbooks/` folder. They orchestrate step-by-step execution.
Each playbook defines:
1. `inputs_schema_ref`: A reference to the input schema we created.
2. `entry_step_id`: The first step to run.
3. `steps`: An array of steps containing `commands` to execute, `signals` to match, and `transitions` to decide what to do next based on the signals. 

**File:** `data/diagops_mcp/packs/network/1.0.0/playbooks/verify-reachability.json`
```json
{
  "spec_version": "1.0",
  "id": "verify-reachability",
  "domain": "network",
  "title": "Reachability Troubleshooting",
  "description": "Verify network path and latency using custom ICMP tools.",
  "supported_envs": ["linux", "vm"],
  "entry_step_id": "test_ping",
  "inputs_schema_ref": "ping-inputs.json",
  "steps": [
    {
      "id": "test_ping",
      "title": "Execute ICMP Ping",
      "goal": "Determine if ICMP packets reach the target",
      "risk": "low",
      "commands": [
        {
          "template_id": "linux.ping_custom",
          "overrides": {
            "target.count": 4
          }
        }
      ],
      "signals": [
        {
          "id": "100_percent_loss",
          "kind": "regex",
          "source": "stdout",
          "match": "100% packet loss",
          "severity": "critical",
          "facts": {
            "ping.status": "dead"
          }
        },
        {
          "id": "0_percent_loss",
          "kind": "regex",
          "source": "stdout",
          "match": "0% packet loss",
          "severity": "info",
          "facts": {
            "ping.status": "alive"
          }
        }
      ],
      "transitions": [
        {
          "to_step_id": "diagnose_routing",
          "when": "facts['ping.status'] == 'dead'",
          "confidence": 0.9,
          "message": "Target is unreachable via ICMP; checking local routes."
        }
      ]
    },
    {
      "id": "diagnose_routing",
      "title": "Check routing table",
      "goal": "Verify there is a valid route to the destination host",
      "commands": [
        {
          "template_id": "linux.ip_route"
        }
      ]
    }
  ]
}
```

## Summary of How the Flow Works:

1. **User requests diagnosis:** When a diag.plan is invoked with `playbook_id: verify-reachability`, `diagops` checks `ping-inputs.json`. If `target.host` is missing, it automatically asks the user for it.
2. **Command Rendering:** The variables (`{{target.host}}`, etc.) injected from the schema are safely rendered into the command arguments (`ping -c 4 <host>`).
3. **Execution & Parsing (Output formatting):** The output `stdout` is evaluated against the Command Template parser (`parse_packet_loss`) and stores the percentage as a fact `network.packet_loss`.
4. **Step Signals & Transitions:** The playbook's signal rules verify if there's a 100% or 0% loss, then set an internal fact (`ping.status: dead`), and uses this fact in a transition expression (`when: "facts['ping.status'] == 'dead'"`) to evaluate the next recommended step (`diagnose_routing`). 
