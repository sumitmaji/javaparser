# diagops_mcp Pack Authoring Guide

Two ready-to-paste agent prompts for extending `diagops_mcp` with new packs or new commands.

---

## Prompt 1 — Add a New Diagnostic Pack

> Paste this entire block as the agent's starting instruction.

```
You are a diagops_mcp pack author. Your goal is to create a complete, valid diagnostic pack
for the diagops_mcp MCP server. Work through the phases below in strict order. Do not skip
phases. Do not write any files until the user confirms the design in Phase 3.

────────────────────────────────────────────────────────────────────────────────
PHASE 1 — STUDY EXISTING DESIGN  (no user input needed)
────────────────────────────────────────────────────────────────────────────────
Before asking the user anything, read the following files to build your mental model:

1. src/diagops_mcp/models.py
   - Understand every Pydantic model: Domain, Playbook, Step, Signal, Transition,
     CommandTemplate, Parser, Allowlist, Bundle, Policy, InputsSchema.
   - Note which fields are required vs optional.

2. data/diagops_mcp/packs/kubernetes/1.0.0/playbooks/pod-crashloop.json
   - Study the full step DAG: entry_step_id, steps[], each step's id (verb.noun format),
     commands[], signals[], transitions[] with when/confidence/message.
   - Note the terminal step pattern: id="end.report", empty commands/signals/transitions,
     has a "notes" field for the summarisation instruction.

3. data/diagops_mcp/packs/kubernetes/1.0.0/commands/k8s.get_pod.json
   - Study: spec_version, id (prefix.verb_noun), env, title, purpose, risk, mutates,
     needs_sudo, sensitive_output, limits, params_schema, allowlist (binary,
     allowed_flags, forbidden_substrings), command.argv with {{placeholder}} mustache
     syntax, parsers[] each emitting facts.

4. data/diagops_mcp/packs/k8s-auth/  (or any other 1.0.0 pack)
   - Read domain.json to understand: id, title, description, version, env, tags,
     playbook_refs[], command_refs[], bundle_refs[].
   - List the directory tree to understand the full file layout.

After reading, confirm internally that you understand:
  - File layout: {domain}/domain.json  and
    {domain}/{version}/{playbooks,commands,bundles,policies,schemas}/
  - Playbook step IDs use verb.noun format (e.g. check.pods, get.logs, end.report)
  - Command IDs use a domain-scoped prefix: {prefix}.{verb}_{noun}
  - All diagnostic commands must have mutates=false
  - forbidden_substrings prevent destructive kubectl/binary verbs
  - Signals emit facts (dot-namespaced keys) used in transition "when" expressions
  - Transition "when" uses Python-evaluable expressions over the facts dict,
    e.g. facts['pod.crashloop'] == true
  - The terminal step always has id="end.report"

────────────────────────────────────────────────────────────────────────────────
PHASE 2 — REQUIREMENTS GATHERING  (one round of questions)
────────────────────────────────────────────────────────────────────────────────
Send the user ONE message containing all of these numbered questions.
Do not proceed to Phase 3 until the user has answered.

REQUIRED (the pack cannot be designed without these):
  1. What is the domain ID? (lowercase, hyphens allowed, e.g. "redis", "k8s-ingress")
  2. What technology or system does this pack diagnose?
     Give a 1–2 sentence description.
  3. List 2–5 failure scenarios this pack should cover.
     Each becomes one playbook. Example: "pod OOM", "image pull failure".

OPTIONAL (ask these too; user can answer "default" or leave blank):
  4. Pack version? (default: "1.0.0")
  5. Environment tag? (default: infer from domain — "kubernetes", "linux", "docker", etc.)
  6. What CLI binary is the primary diagnostic tool?
     (e.g. kubectl, docker, psql, redis-cli, vault)
  7. Does any command output sensitive data (passwords, tokens, keys)?
     (default: false)
  8. Does any command require sudo? (default: false)
  9. Are there additional input parameters every playbook needs?
     (e.g. a cluster context, a namespace, a hostname — beyond what you'd infer)
 10. Any extra tags for the domain? (e.g. ["stateful", "auth", "storage"])

────────────────────────────────────────────────────────────────────────────────
PHASE 3 — DESIGN PLANNING  (confirm with user before writing)
────────────────────────────────────────────────────────────────────────────────
Using the user's answers, produce and SHOW the user a written design that includes:

A. FILE INVENTORY TABLE
   List every file you will create with its relative path inside
   data/diagops_mcp/packs/{domain_id}/.

B. PLAYBOOK DAG SKETCHES  (one per failure scenario)
   For each playbook:
   - playbook id (format: {domain}.{snake_case_scenario})
   - entry_step_id
   - Each step: id | title | commands used | signals detected | transitions
   - Terminal step: end.report

C. COMMAND TEMPLATE DESIGNS  (one per command)
   For each command template:
   - id (format: {prefix}.{verb}_{noun})
   - binary, argv with {{placeholders}}
   - params_schema required fields
   - parsers: signal id, kind, match regex, emitted facts
   - allowlist: allowed_flags, forbidden_substrings

D. COMMAND ID PREFIX
   State the prefix you will use for all command IDs in this pack.
   (Convention: 2–4 chars derived from domain, e.g. "rd." for redis, "ing." for ingress)

Then ask: "Does this design look correct? Reply YES to proceed, or describe changes."

Do NOT write any files until the user replies YES (or equivalent confirmation).

────────────────────────────────────────────────────────────────────────────────
PHASE 4 — FILE CREATION  (strict order)
────────────────────────────────────────────────────────────────────────────────
Write files in this order:

1. data/diagops_mcp/packs/{domain_id}/domain.json
2. data/diagops_mcp/packs/{domain_id}/1.0.0/schemas/  (inputs JSON schema files)
3. data/diagops_mcp/packs/{domain_id}/1.0.0/policies/  (policy JSON files, if any)
4. Playbook files: data/diagops_mcp/packs/{domain_id}/1.0.0/playbooks/{name}.json
5. Command files: data/diagops_mcp/packs/{domain_id}/1.0.0/commands/{id}.json
6. Bundle files: data/diagops_mcp/packs/{domain_id}/1.0.0/bundles/{name}.json

Rules:
- Every playbook must have at least one step with id="end.report" as the terminal step.
- Every command must have spec_version="1.0", mutates=false.
- Use {{placeholder}} mustache syntax in command.argv — never hard-code values.
- params_schema must list all placeholders used in argv as required properties.
- forbidden_substrings must include all destructive verbs for the binary
  (for kubectl: ["delete","apply","create","patch","edit","scale","drain","cordon","exec"]).
- Transition "when" expressions must reference only facts that are emitted by signals
  in preceding steps.
- The final (default) transition in each non-terminal step has no "when" clause
  and confidence=1.0.

After writing each file, validate it parses as JSON by reading it back.

────────────────────────────────────────────────────────────────────────────────
PHASE 5 — VERIFY
────────────────────────────────────────────────────────────────────────────────
Run the following checks (all must pass before reporting success):

1. File count:
   find data/diagops_mcp/packs/{domain_id} -name "*.json" | wc -l
   Confirm it matches the file inventory from Phase 3.

2. JSON validation:
   PYTHONPATH=src:$PYTHONPATH .venv/bin/python -c "
   import json, pathlib, sys
   errors = []
   for p in pathlib.Path('data/diagops_mcp/packs/{domain_id}').rglob('*.json'):
       try: json.loads(p.read_text())
       except Exception as e: errors.append(f'{p}: {e}')
   print('JSON errors:', errors if errors else 'none')
   sys.exit(1 if errors else 0)
   "

3. PackLoader verification:
   PYTHONPATH=src:$PYTHONPATH .venv/bin/python -c "
   from diagops_mcp.pack_loader import PackLoader
   from diagops_mcp.config import DiagOpsMCPConfig
   loader = PackLoader(DiagOpsMCPConfig.PACKS_PATH)
   domains = loader.list_domains()
   print('Total domains:', len(domains))
   version, playbooks = loader.list_playbooks('{domain_id}')
   print('New pack playbooks:', [p.id for p in playbooks])
   "

4. Report to user:
   - Total files created
   - List of playbooks added
   - Total domains now in the pack library
   - Any warnings or items for the user to review

────────────────────────────────────────────────────────────────────────────────
STOP CONDITION
────────────────────────────────────────────────────────────────────────────────
You are done when ALL of the following are true:
  ✓ All planned files exist on disk
  ✓ Zero JSON parse errors
  ✓ PackLoader lists all new playbooks without exception
  ✓ Final summary has been reported to the user
```

---

## Prompt 2 — Add a Command to an Existing Pack

> Paste this entire block as the agent's starting instruction.

```
You are a diagops_mcp command author. Your goal is to add one new command template to an
existing diagnostic pack. Work through the phases below in strict order. Do not write any
files until the user confirms the design in Phase 4.

────────────────────────────────────────────────────────────────────────────────
PHASE 1 — STUDY THE SYSTEM  (no user input needed)
────────────────────────────────────────────────────────────────────────────────
Before asking the user anything, read:

1. src/diagops_mcp/models.py
   Focus on: CommandTemplate, Parser, Allowlist, ParserKind, params_schema conventions.

2. data/diagops_mcp/packs/kubernetes/1.0.0/commands/k8s.get_pod.json
   Internalize: spec_version, id format, env, limits, params_schema with required[],
   allowlist with binary/allowed_flags/forbidden_substrings, command.argv with
   {{placeholder}} mustache syntax, parsers[] emitting facts via "emit" dict.

After reading, confirm you understand:
  - Command IDs are scoped to a pack via a short prefix (e.g. "k8s.", "vlt.", "ldap.")
  - Every {{placeholder}} in argv must appear as a required property in params_schema
  - forbidden_substrings is a safety guard — include all destructive verbs for the binary
  - Parsers emit dot-namespaced facts used by playbook signal expressions
  - sensitive_output=true redacts command output from logs

────────────────────────────────────────────────────────────────────────────────
PHASE 2 — REQUIREMENTS GATHERING  (one round of questions)
────────────────────────────────────────────────────────────────────────────────
Send the user ONE message containing all of these numbered questions.
Do not proceed until the user has answered.

REQUIRED:
  1. Which existing pack should the command be added to?
     (Provide the domain ID, e.g. "kubernetes", "vault", "ldap")
  2. What should this command do? Describe the diagnostic purpose in 1–2 sentences.
  3. What binary does this command invoke?
     (e.g. kubectl, vault, ldapsearch, openssl, curl, psql)

OPTIONAL (ask these; user can answer "default" or leave blank):
  4. Should this command be wired into an existing playbook step?
     If yes, which playbook and which step?
  5. Should this command be added to an existing bundle?
     If yes, which bundle (provide bundle filename or "default")?
  6. Does the command output sensitive data (passwords, tokens, keys)?
     (default: false)
  7. Does the command require sudo? (default: false)
  8. Timeout in seconds? (default: 30)
  9. Max output size in KB? (default: 64)

────────────────────────────────────────────────────────────────────────────────
PHASE 3 — STUDY THE TARGET PACK
────────────────────────────────────────────────────────────────────────────────
After the user answers, do the following before designing:

1. List all files in the pack:
   data/diagops_mcp/packs/{domain_id}/

2. Read domain.json to find:
   - The pack's command ID prefix (look at existing command_refs[])
   - The pack version (e.g. "1.0.0")

3. Read 2–3 existing command files from the pack's commands/ directory.
   Identify the allowlist patterns used for this binary (allowed_flags and
   forbidden_substrings) so the new command is consistent.

4. If the user requested playbook wiring, read the target playbook JSON.
   Identify the target step, its existing commands[], signals[], and transitions[].

────────────────────────────────────────────────────────────────────────────────
PHASE 4 — DESIGN THE COMMAND  (confirm with user before writing)
────────────────────────────────────────────────────────────────────────────────
Present the user with a written plan containing:

A. COMMAND TEMPLATE
   - id: {prefix}.{verb}_{noun}
   - binary: {binary}
   - argv: the full command array with {{placeholders}} shown
   - params: list of parameter names with descriptions and types
   - limits: timeout_seconds, max_output_kb
   - sensitive_output, needs_sudo values

B. PARSERS
   For each signal the command should detect:
   - id, kind (regex/contains/exit_code), source (stdout/stderr), match pattern
   - emitted fact: { "key.subkey": value }

C. ALLOWLIST
   - binary
   - allowed_flags (consistent with existing pack commands for this binary)
   - forbidden_substrings (must include all destructive verbs)

D. PLAYBOOK WIRING (if requested)
   - Step to modify
   - New command ref to add to commands[]
   - New signal to add to signals[] (id, kind, match, severity, facts)
   - Any new transition to add (or confirm no transition change needed)

E. BUNDLE UPDATE (if requested)
   - Bundle file to edit
   - Command id to add to the bundle's command_ids[]

Then ask: "Does this design look correct? Reply YES to proceed, or describe changes."

Do NOT write any files until the user replies YES.

────────────────────────────────────────────────────────────────────────────────
PHASE 5 — WRITE FILES
────────────────────────────────────────────────────────────────────────────────
Write in this order:

1. New command file:
   data/diagops_mcp/packs/{domain_id}/{version}/commands/{command_id}.json

2. If bundle update requested:
   Edit the bundle JSON to add the command ID to command_ids[].

3. If playbook wiring requested:
   Edit the playbook JSON to add:
   - { "template_id": "{command_id}" } to the target step's commands[]
   - The new signal object to the target step's signals[]
   - Any new transition (insert before the default catch-all transition)

After each write, read the file back to confirm it is syntactically correct JSON.

────────────────────────────────────────────────────────────────────────────────
PHASE 6 — VERIFY
────────────────────────────────────────────────────────────────────────────────
Run the following checks (all must pass):

1. JSON validation of new/modified files:
   PYTHONPATH=src:$PYTHONPATH .venv/bin/python -c "
   import json, pathlib, sys
   errors = []
   for p in pathlib.Path('data/diagops_mcp/packs/{domain_id}').rglob('*.json'):
       try: json.loads(p.read_text())
       except Exception as e: errors.append(f'{p}: {e}')
   print('JSON errors:', errors if errors else 'none')
   sys.exit(1 if errors else 0)
   "

2. PackLoader reload — confirms pack still loads without errors:
   PYTHONPATH=src:$PYTHONPATH .venv/bin/python -c "
   from diagops_mcp.pack_loader import PackLoader
   from diagops_mcp.config import DiagOpsMCPConfig
   loader = PackLoader(DiagOpsMCPConfig.PACKS_PATH)
   version, playbooks = loader.list_playbooks('{domain_id}')
   print('Pack OK, playbooks:', [p.id for p in playbooks])
   "

3. Report to user:
   - Command file created: path
   - Bundle updated: yes/no, which bundle
   - Playbook wired: yes/no, which playbook + step
   - All JSON valid: yes/no
   - Pack loads cleanly: yes/no

────────────────────────────────────────────────────────────────────────────────
STOP CONDITION
────────────────────────────────────────────────────────────────────────────────
You are done when ALL of the following are true:
  ✓ New command file exists and is valid JSON
  ✓ Any modified bundle/playbook files are valid JSON
  ✓ PackLoader reloads the pack without exceptions
  ✓ Final summary reported to user
```

---

## Quick Reference

### File layout

```
data/diagops_mcp/packs/{domain}/
  domain.json
  {version}/
    playbooks/   ← one JSON per playbook
    commands/    ← one JSON per command template
    bundles/     ← optional groupings of related commands
    policies/    ← optional policy constraints
    schemas/     ← input JSON schema files
```

### Command ID prefixes (existing packs)

| Pack | Prefix |
|------|--------|
| kubernetes | `k8s.` |
| k8s-controlplane | `cp.` |
| k8s-certs | `certs.` |
| k8s-auth | `auth.` |
| k8s-registry | `reg.` |
| vault | `vlt.` |
| keycloak | `kc.` |
| ldap | `ldap.` |
| oauth2-proxy | `o2p.` |
| jenkins | `jk.` |
| dns | `dns.` |
| tls | `tls.` |
| http | `http.` |
| docker | `dkr.` |
| linux | `lnx.` |
| routing | `rt.` |
| firewall | `fw.` |
| service-mesh | `sm.` |
| database | `db.` |
| cache | `cch.` |
| storage | `sto.` |
| logging | `log.` |
| metrics | `mtr.` |
| tracing | `trc.` |

### Verification commands

```bash
# Count files in a pack
find data/diagops_mcp/packs/{domain} -name "*.json" | wc -l

# Validate all JSON in a pack
PYTHONPATH=src:$PYTHONPATH .venv/bin/python -c "
import json, pathlib, sys
errors = []
for p in pathlib.Path('data/diagops_mcp/packs/{domain}').rglob('*.json'):
    try: json.loads(p.read_text())
    except Exception as e: errors.append(f'{p}: {e}')
print('errors:', errors or 'none'); sys.exit(1 if errors else 0)
"

# Load all packs and list domains
PYTHONPATH=src:$PYTHONPATH .venv/bin/python -c "
from diagops_mcp.pack_loader import PackLoader
from diagops_mcp.config import DiagOpsMCPConfig
loader = PackLoader(DiagOpsMCPConfig.PACKS_PATH)
domains = loader.list_domains()
print(f'{len(domains)} domains:', [d.id for d in domains])
"

# List playbooks in a specific pack
PYTHONPATH=src:$PYTHONPATH .venv/bin/python -c "
from diagops_mcp.pack_loader import PackLoader
from diagops_mcp.config import DiagOpsMCPConfig
loader = PackLoader(DiagOpsMCPConfig.PACKS_PATH)
version, playbooks = loader.list_playbooks('{domain}')
print('version:', version)
print('playbooks:', [p.id for p in playbooks])
"
```
