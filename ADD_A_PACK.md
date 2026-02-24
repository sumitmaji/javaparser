# How to Add a New Pack to DiagOps MCP

This guide provides step-by-step instructions on how to create and add a new diagnostic pack to the `diagops_mcp` server.

## Overview

A "pack" (or domain) is a versioned set of diagnostic playbooks, command templates, parsers, and guard policies focused on a specific problem area (e.g., `network`, `kubernetes`, `database`).

Packs are stored in the filesystem and dynamically discovered and loaded by `PackLoader`.

---

## Step-by-Step Instructions

### Step 1: Create the Pack Directory Structure

All packs live in the `data/diagops_mcp/packs/` directory.

1. Create a new directory for your pack's domain (e.g., `storage`):
   ```bash
   mkdir -p data/diagops_mcp/packs/storage
   ```
2. Create a version directory inside using semantic versioning (e.g., `1.0.0`):
   ```bash
   mkdir -p data/diagops_mcp/packs/storage/1.0.0/{playbooks,commands,bundles,policies,schemas}
   ```

### Step 2: Define the `domain.json`

At the root of your pack directory, create a `domain.json` file. This describes the pack to the MCP client.

**File:** `data/diagops_mcp/packs/storage/domain.json`
```json
{
  "id": "storage",
  "title": "Storage & Volumes",
  "description": "Diagnostic playbooks for disk full, volume mount issues, and I/O errors."
}
```

### Step 3: Add Command Templates

Command templates define the actual CLI commands to execute, along with their expected inputs, limits, and parsers. These live in the `commands/` directory.

**File:** `data/diagops_mcp/packs/storage/1.0.0/commands/linux.df.json`
```json
{
  "spec_version": "1.0",
  "id": "linux.df",
  "env": "linux",
  "title": "Check Disk Space",
  "purpose": "Identify full or nearly full filesystems",
  "risk": "low",
  "mutates": false,
  "needs_sudo": false,
  "allowlist": {
    "binary": "df",
    "allowed_flags": ["-h", "-T"]
  },
  "command": {
    "args": ["df", "-hT"]
  },
  "parsers": [],
  "what_to_look_for": ["Use percentages near 100%"]
}
```

### Step 4: Add Playbooks

Playbooks orchestrate command templates into a sequence of steps, defining how to transition based on command output signals. These live in the `playbooks/` directory.

**File:** `data/diagops_mcp/packs/storage/1.0.0/playbooks/disk-full.json`
```json
{
  "spec_version": "1.0",
  "id": "disk-full",
  "domain": "storage",
  "title": "Disk Full Triage",
  "description": "Diagnose filesystems running out of space.",
  "supported_envs": ["linux"],
  "entry_step_id": "check_space",
  "steps": [
    {
      "id": "check_space",
      "title": "Check available disk space",
      "goal": "Identify if any partition is 100% full",
      "risk": "low",
      "commands": [
        {
          "template_id": "linux.df"
        }
      ]
    }
  ]
}
```

### Step 5: Add Guard Policies 

Guard policies determine execution safety and approval requirements. These live in the `policies/` directory.

**File:** `data/diagops_mcp/packs/storage/1.0.0/policies/default.json`
```json
{
  "spec_version": "1.0",
  "rules": [
    {
      "id": "require-ticket-for-mutations",
      "when": "step.mutates == true",
      "require": ["ticket_id"]
    }
  ]
}
```

### Step 6: (Optional) Define Schemas & Bundles
- **Schemas (`schemas/`):** If your playbook requires specific inputs (e.g., namespace, pod name), define a JSON schema and reference it in the playbook's `inputs_schema_ref`.
- **Bundles (`bundles/`):** If you want to define a specific set of commands to run together as a "snapshot" without full playbook transition logic, define them in the `bundles/` folder.

---

## Testing Your Pack

Once the files are saved, the `PackLoader` in `diagops_mcp` will automatically discover the highest semantic version of your domain. Restart your MCP server to ensure everything parses correctly. You can list the available domains using the appropriate MCP command to verify your new pack is visible.
