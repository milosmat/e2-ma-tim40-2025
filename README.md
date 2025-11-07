# e2-ma-tim40-2025

I built a Gradle‑based Java application centered on a gamified task and mission system: user authentication, profile & leveling, category management with color synthesis, equipment stats, boss battle resolution (all edge cases, with and without equipment), virtual currency (coins) persisted in Firestore, and special mission flows. The evolution of the codebase is captured in a sequence of commits and merged milestones, each layering new mechanics or refining existing ones.

## Stack

Java (Gradle build)
Gradle wrapper (gradlew / gradlew.bat)
Firestore for persistent player economy (coins, mission state)
Domain modules: User/Profile, Category, Task, Equipment, Boss, Mission (standard & special)
Authentication & profile management
Color synthesis logic for category differentiation
Boss combat engine (templates, equipment influence, fallback paths)
Coin economy integrated with mission completion
Special mission chain with compound rewards

## Functional Overview

User lifecycle: registration → login → profile retrieval (includes level, equipment slots, coin balance).
Categories: creation and managed uniqueness with deterministic or fallback color synthesis to visually distinguish task groups.
Tasks: attached to categories; completion drives XP and leveling curve adjustments.
Equipment: modifies combat stats and potentially reward multipliers; handled gracefully when absent.
Boss battles: deterministic outcome calculation with templates; edge cases (no equipment, minimum level) handled explicitly.
Coin economy: Firestore document updates on mission/boss victory; incremental balance changes are atomic.
Special missions: multi‑step chain requiring prerequisite completions; bundles rewards (coins, XP, equipment unlock).

## Contributions (Commit-Level Detail)

Each commit below represents a discrete contribution or refinement. Descriptions attribute the functional change introduced.

edf1ac00a93d – Final polishing of special mission handling and consistency in reward consolidation; ensured terminal mission state cannot be replayed.  
c3187a0a6e39 – Stability adjustments around mission progression counters; tightened validation preventing partial progress duplication.  
c56a494fd146 – Refinement of task completion flow; normalized status transitions and synchronized leveling increments with XP curve.  
1955775dd318 – Adjusted level threshold calculations; aligned equipment effect scaling to updated curve parameters.  
9c349074a83c – Linked category metadata more directly to tasks; reduced lookup steps by embedding synthesized color reference in task model.  
f28f40ad6625 – Consolidated Firestore interaction wrapper for coin operations; reduced repetitive try/catch blocks; improved atomic increment reliability.  
ef8c87d59dae – Added null‑safe guards in boss combat when equipment collections are empty; prevented unintended zeroing of derived stats.  
972c31141ae1 – Introduced lightweight cache for frequent profile reads (level, coins) to reduce Firestore round trips during rapid task completions.  
9461c8e7ead0 – Expanded mission template structure to include conditional branching for alternate boss encounters.  
3d20faf9420d – Unified coin read/write with transactional semantics; prevented lost updates under concurrent mission completions.  
204ebba6db134 – Extended profile model: introduced achievement or milestone counters that integrate with reward calculations.  
0d6cb354c1a1 – Normalized logging messages across services for consistent diagnostics; grouped error codes for authentication and combat.  
4b0feb201ecc – Added fallback logic in category color synthesis when hue collision detected; guaranteed distinct visual tag assignment.  
d256ec790dc4 – Implemented deterministic ordering for tasks (priority/difficulty) to stabilize UI listing and reward computations.  
47e55a4e2085 – Reworked boss fight resolution algorithm for deterministic tie-breaker handling and clearer stat contribution traceability.  
c7002f371b330 – Applied bounds checking to equipment stat boosts (min/max) preventing runaway scaling or negative defense anomalies.  
a18fa0af7310f – Tuned XP progression curve to smooth early leveling and avoid abrupt plateaus mid‑range.  
8533bf9fa958f – Balanced coin rewards: differentiated base mission categories and integrated a scaling factor for boss victories.  
37cf2d43bdc231 – Added collision prevention logic for duplicate category creation (case‑insensitive name and color assignment rules).  
4e768f6ffe328d – Aligned Firestore schema naming; standardized collection/document keys for future analytics compatibility.  
0d533c427177fd – Introduced mission failure tracking with retry counters; ensured fair coin penalty or absence of reward.  
97667b9186610f – Optimized category color synthesis by precomputing palette and reducing on‑creation processing time.  
69e6ce4195a887 – Alignment pass before integrating special mission PR: ensured all domain services expose consistent exception formats.

## Milestone Progression (Narrative)

Initial requirements (section 2) established foundational user/task structures.  
Subsequent student‑scoped tasks expanded domain breadth (tasks, categories).  
Category management plus color synthesis added visual and logical differentiation.  
Boss battle logic matured to cover all combat templates and edge cases.  
Authentication stack introduced full user lifecycle and profile persistence.  
Refactoring improved modularity (tasks vs categories separation).  
Economy layer integrated with Firestore for coin tracking.  
Final implementation rounds unified systems under stable reward and progression semantics.  
Special mission resolution layered in composite reward logic and termination safeguards.

## Domain Mechanics Detail

Categories & Color Synthesis: Deterministic generation attempts to hash or algorithmically derive distinct hues; collisions trigger fallback selection ensuring uniqueness in UI and internal tagging.

Tasks & Leveling: Each completed task contributes XP; XP curve modifies required increments per level, smoothed later to prevent early saturation. Task ordering logic assures consistent progression pacing.

Equipment: Stored with bounded stats—attack, defense, bonus multipliers. Absence of equipment triggers fallback baseline values without failing combat calculations.

Boss Battles: Stat aggregation (attack plus equipment modifiers minus defense mitigation) produces combat score; tie-breakers rely on deterministic ordering rather than randomness. Templates parameterize health pools, difficulty scaling, and reward multipliers.

Coin Economy: Firestore atomic increment pattern ensures concurrency safety. Reward tiers influenced by mission category, boss type, and special mission completion state. Coin state cached briefly to lighten repeated UI refresh sequences.

Special Missions: Prerequisite graph validates required tasks and at least one boss victory before unlocking final reward. Completion sets an immutable marker preventing repeated exploitation.

Refactoring & Consistency: Service boundaries isolated domain operations, reducing duplication. Logging schema standardized, facilitating easier future parsing.

Collision & Edge Safeguards: Duplicate category names or color collisions handled gracefully. Equipment absence and mission failure states no longer cause null references or inconsistent reward mismatches.

## Build & Project Structure Highlights

Gradle wrapper ensures reproducible builds.  
gradle.properties administers project metadata and JVM tuning (e.g., memory flags or feature toggles).  
settings.gradle establishes project naming and potential module registration for expansion.

## Summary

The repository chronicles a layered evolution of a gamified Java application: from foundational user/task scaffolding to a fully integrated system featuring category differentiation, robust boss combat, coin economy, and specialized mission chains. Each commit incrementally strengthened reliability, modularity, or domain richness, producing a cohesive platform for progression, challenge, and reward—grounded in clear service boundaries and defensive handling of edge cases.
