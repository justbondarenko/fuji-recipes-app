---
name: sdd-spec-authoring
description: Turn a product requirements document (PRD), feature brief, client request, or meeting notes into a complete spec-driven development feature folder — 01-functional.md, 02-schema.json, 03-behavior.feature, and 04-tasks.md. Use this whenever the user has a PRD, Notion page, requirements doc, feature description, or rough feature idea and needs it turned into implementable specs, or mentions SDD, spec-driven development, writing specs, feature specs, acceptance criteria, Gherkin scenarios, or a FEAT-XXX folder. Also use when the user asks to review or improve existing spec files. Trigger this even if the user just describes a feature and asks "write the specs for this" without naming the methodology.
---

# SDD spec authoring

Converts human-facing requirements into machine-executable specs.

This skill covers the **authoring** side of spec-driven development: reading a
PRD and producing the four spec files a developer or coding agent will build
from. It does not cover implementation — once the folder is written and complete,
the repo's own `AGENTS.md` takes over.

## What this produces

```
specs/features/FEAT-XXX-slug/
├── 01-functional.md    # what to build, scope, out-of-scope
├── 02-schema.json      # data contract, JSON Schema — ONLY if the feature
│                       #   introduces a shape crossing a boundary (see Step 4)
├── 03-behavior.feature # acceptance criteria, Gherkin
└── 04-tasks.md         # ordered implementation checklist
```

Blank templates for the first three are in `templates/` alongside this file.
Read them before writing — they carry structural detail not repeated here.
`04-tasks.md` has no template; it's a plain checklist.

---

## Process

### Step 0 — Load project context (do this first, always)

**Do not write any spec file before completing this step.** A spec written
without project knowledge invents field names that clash with existing ones,
proposes approaches the architecture rules out, and splits scope along seams
that don't exist in the codebase. The output looks plausible and is wrong in
ways that only surface during implementation.

Read, in the target repo:

1. `specs/steering/architecture.md` — system shape, boundaries, directory
   conventions, domain glossary, shared contract conventions, known constraints
2. `specs/steering/tech-stack.md` — what's approved; constrains what the spec
   can propose
3. `specs/steering/coding-standards.md` — architectural rules the spec must not
   require breaking
4. `specs/features/` — list existing feature folders. Skim any that touch the
   same area.

Then survey the codebase directly for what the steering files don't capture:

- **Existing contracts** — the OpenAPI file, migrations, or shared types named
  in `architecture.md`. If the feature touches a shape that already exists,
  the spec extends it rather than defining a parallel one.
- **Naming in the area you're specifying** — grep for the domain nouns from the
  PRD. If the codebase calls it a `Dataset`, the spec says `Dataset`, even if
  the PRD says "file".
- **Established patterns for similar features** — if three existing endpoints
  return a `{ error, message }` envelope, the new one does too.

If `architecture.md` is missing or mostly empty, say so before proceeding. You
can still write a spec, but flag explicitly which parts are guesses — naming,
file paths, and contract conventions are the ones most likely to be wrong.

**What this step changes about the output:**

| Without context | With context |
|---|---|
| Invents field names | Reuses the domain glossary's terms |
| Generic error shape | The project's actual error envelope |
| Tasks naming invented paths | Tasks naming real directories |
| Duplicates an existing contract | Extends or points at it |
| Proposes a background job | Knows there are no workers, scopes around it |

---

### Step 1 — Read the source material and identify what's missing

The input is usually a PRD, but may be meeting notes, a client email, a Slack
thread, or a verbal description. Whatever it is, extract:

- Who the user is
- What they can't do today
- What they should be able to do after this ships
- Anything explicitly ruled out
- Any constraints (performance, compatibility, existing systems)

**Then list what's absent.** PRDs are written for humans and routinely omit
things specs need. Common gaps:

- Empty states — what shows when there are zero results?
- Error handling — what happens on invalid input, network failure, permission denied?
- Boundaries — maximum sizes, counts, lengths
- Concurrency — what if two things happen at once?
- Whether the change is persistent or session-only
- What happens on undo/reversal

**Also check the PRD against project context from Step 0.** PRD authors don't
know the codebase, so watch for:

- Requirements that conflict with a known constraint (e.g. asking for something
  long-running when functions time out at 10s)
- Terms that don't match the domain glossary — resolve to the codebase's term
- Assumed features that don't exist yet, making this feature depend on unbuilt work
- Requirements needing a library not in `tech-stack.md`

These are more valuable to surface than PRD-internal gaps, because the PRD
author can't self-detect them.

**Ask about these gaps before writing.** Don't fill them by inference — a
plausible guess baked into a spec is worse than an open question, because it
looks decided. If the user isn't available to answer, write the question into
the Open questions section of `01-functional.md` and flag that the spec isn't
ready until those close.

### Step 2 — Establish the feature ID and slug

The ID comes from wherever the team tracks work; don't invent one. The slug is
derived from the feature title, lowercased and hyphenated, and is fixed from
this point — the branch name and folder name both depend on it staying stable.

`FEAT-123` + "CSV export" → `specs/features/FEAT-123-csv-export/`

### Step 3 — Write `01-functional.md`

This is a **compression** of the PRD, not a reformatting of it.

Cut: narrative, background, rationale, screenshots, meeting context, competitive
analysis, anything that explains *why the decision was made* rather than *what
was decided*.

Keep and sharpen:

- **User stories** — "As a X, I can Y, so that Z." If a requirement won't fit
  this shape, it's probably not yet clear enough to build.
- **Scope** — explicit and enumerated. Not listed means not included.
- **Out of scope** — equally explicit, and label where deferred items go:
  `(v2)`, `(separate feature)`, `(won't do)`. This section does real work: it
  stops scope creep, and it stops coding agents from helpfully building extras.
- **Constraints** — only ones that bind the implementation.

**Use the project's vocabulary, not the PRD's.** If the PRD says "spreadsheet"
and the domain glossary says `Dataset`, the spec says `Dataset`. Note the
mapping once if it aids readability, then use the project term throughout.

Test: could someone build this feature correctly having read only this file
plus the steering files? If they'd have to guess at anything, it isn't finished.

### Step 4 — Write `02-schema.json` (only if the feature needs one)

**Decide first whether this file should exist at all.** Write it only when the
feature introduces or changes a data shape that **crosses a boundary** —
network, disk, database, or process. Concretely:

| Include a schema | Skip it |
|---|---|
| API request/response bodies | Component or view state |
| Database entities and migrations | Local variables, derived values |
| Event, queue, or webhook payloads | Props passed between components |
| Import/export file formats | Anything a type declaration already covers |
| Config read from file or environment | Pure UI behavior with no persisted shape |

The test: **would two independently-written pieces of code have to agree on
this shape?** If the answer is no, the schema is ceremony — a type declaration
restated in a more verbose format — and the file should be omitted.

If the feature needs no schema, omit the file and state that explicitly in
`01-functional.md` ("No data contract — this feature introduces no persisted or
transmitted shapes") so nobody assumes it was forgotten.

**If the shape already exists elsewhere** — an OpenAPI file, an existing
migration, a shared types package — don't duplicate it. Write a one-line
pointer to the canonical definition instead. Two copies of a contract is worse
than one, because they drift.

When you do write it, **conform to the project's existing conventions** from
`architecture.md` — the error envelope, field naming case, timestamp format,
ID type, and pagination shape are all project decisions, not per-feature ones.
A schema that invents its own error shape is a bug in the spec.

Check `architecture.md`'s "Existing contracts" section before defining anything.
If the shape exists, extend or reference it rather than writing a parallel copy.

- Valid JSON Schema, not prose describing types — the point is that it can be
  validated programmatically and used to generate types, validators, and fixtures
- `title` and `description` on the schema and on every field
- `"additionalProperties": false` unless an open shape is deliberate
- At least one entry in `examples` — makes intent concrete and gives tests a fixture
- Constrain properly: `enum` for fixed sets, `minimum`/`maxLength` where real
  limits exist, explicit nullability with a note on when null occurs

The underrated reason this matters: without a pinned schema, one agent session
names a field `filterText` and the next names it `searchQuery`. The schema
removes that entire class of drift.

### Step 5 — Write `03-behavior.feature`

Valid Gherkin, meant to run through a Cucumber-style runner.

- One `Feature:` block with the As-a / I-want / So-that preamble
- One `Scenario:` per distinct behavior, in Given/When/Then
- Keep steps **declarative** — "the user filters by name", not "the user clicks
  the element with id #filter-1". Mechanical steps break on every UI change.
- Cover the happy path, the variations, the reversal, and the edge cases

Every user story in `01-functional.md` should map to at least one scenario. Every
gap identified in Step 1 should map to a scenario too — that's what those
questions were for. A spec with only happy paths produces an implementation with
only happy paths.

### Step 6 — Write `04-tasks.md`

Plain ordered checklist. Each item should be:

- Small enough to be one logical commit
- Concrete enough that "is it done?" has an obvious answer
- Ordered so dependencies come first
- **Naming real paths** from `architecture.md`'s "Where things live" section,
  not invented ones. `src/lib/parseCsv.ts`, not `the CSV parsing module`.

End with a task for tests covering the `03-behavior.feature` scenarios. If
something genuinely can't be tested automatically, say so in the task and name
the manual check to run instead.

### Step 7 — Check before handing off

- [ ] Every user story has at least one Gherkin scenario
- [ ] Every Gherkin scenario is reachable from something in scope
- [ ] Out-of-scope section is populated, not empty
- [ ] Schema exists only if a shape crosses a boundary — and if it exists, it
      matches the fields the functional spec implies
- [ ] If no schema, `01-functional.md` says so explicitly
- [ ] Edge cases from Step 1 appear as scenarios, not just as prose
- [ ] No open questions remain, or the spec is explicitly marked not-ready
- [ ] Task list covers everything the scenarios require
- [ ] Terminology matches the domain glossary in `architecture.md`
- [ ] Schema follows the project's shared contract conventions
- [ ] Tasks name real directories from the project's layout
- [ ] Nothing in the spec conflicts with a known constraint
- [ ] Nothing requires a library absent from `tech-stack.md`

---

## Reviewing existing specs

When asked to review rather than author, check the Step 7 list, then look for
the failure modes that show up most:

- **Vague acceptance criteria** — "works correctly", "handles errors
  gracefully". Not testable, so not acceptance criteria. Rewrite concretely.
- **Empty or missing out-of-scope** — nearly always means scope hasn't actually
  been bounded, just not written down.
- **Schema as prose** — a markdown table describing fields instead of JSON Schema.
- **Schema that shouldn't exist** — a contract file describing purely internal
  state that never crosses a boundary. Delete it; it's a type declaration in a
  more verbose format.
- **Duplicated contract** — a schema restating a shape already defined in an
  OpenAPI file, migration, or shared types package. Replace with a pointer.
- **Mechanical Gherkin** — steps referencing DOM elements, CSS selectors, or
  button positions.
- **Scope creep between files** — behavior scenarios testing things the
  functional spec never listed.
- **Happy path only** — no empty state, no error case, no boundary.
- **Context-blind spec** — invented field names, generic error shapes, tasks
  naming directories that don't exist, or a contract duplicating one already in
  the codebase. Usually means Step 0 was skipped; re-read the steering files
  and the existing contracts, then correct the spec against them.

---

## Notes

- Trivial changes — copy tweaks, styling, one-line fixes — don't need a feature
  folder at all. Say so rather than generating ceremony.
- Don't edit files in `templates/` for a specific feature. Copy them out first.
- `01-functional.md` is effectively frozen once implementation starts. Changes
  after that point ship alongside the code that required them.
