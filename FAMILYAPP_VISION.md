# FamilyApp — Product Vision and Functional Roadmap

> Current name (owner decision, 2026-10-21): **Shopping List App**. This document
> retains the original FamilyApp wording as historical product context.

**Project:** FamilyApp (originally FamilyLists)  
**Platform:** Android  
**Current project package:** `com.example.familyapp`  
**Document status:** Historical product vision compiled from earlier planning discussions, October 2026  
**Important:** This is a product specification, **not** a verified inventory of the current source code. The owner reports that the app now runs successfully. Inspect the actual repository before deciding what is implemented.

## 1. Product vision

FamilyApp is a family organization app whose first and primary use case is shared grocery and shopping lists. Family members should be able to prepare lists together, reuse a common product catalog, adjust quantities, and mark items purchased, with changes eventually synchronized across their devices.

The long-term intention is broader family organization through practical extensions, while preserving a fast, uncomplicated everyday shopping experience. The app is initially for personal/family use and internal testing, with possible later release on Google Play.

### Core product principles

- A reusable catalog of shopping products, separate from individual shopping lists.
- Multiple independent lists (e.g., home, weekend house, travel).
- Simple item entry, quantity adjustment, and purchased/unpurchased status.
- Shared use by family members, with list-specific visibility and administrative permissions.
- Reliable data persistence and a simple interface.
- Add advanced features incrementally, without destabilizing working core functionality.

## 2. Core user experiences

### 2.1 Main menu

The original main menu contains entry points for **Shopping List** and **Shopping Items**. A basic main menu and navigation were implemented earlier. The final visual layout has not been specified.

### 2.2 Shopping Items — reusable catalog

This is a persistent catalog of products the household commonly buys. A product is created once and can be selected again for different lists.

**Agreed capabilities:**

- View catalog items.
- Add new products.
- Edit/rename existing products.
- Delete products.
- Organize products into categories.
- Select catalog products when composing shopping lists.
- Use a long press to access product actions such as rename, delete, and eventually image handling.
- Support an optional image per product in a later iteration.

Examples: Milk, Bread, Eggs, Coffee, Detergent. These are examples, not a prescribed initial catalog.

**Not yet decided:** Category taxonomy, whether categories are user-created or fixed, detailed deletion semantics, duplicate-name behavior, and final catalog UI design.

### 2.3 Shopping Lists — daily shopping

A shopping list contains products selected from the catalog, with list-specific quantities and purchase status.

**Agreed capabilities:**

- View products on a list.
- Add products, including selecting from the reusable catalog.
- Adjust item quantity using `+` and `−` controls.
- Check and uncheck purchased items.
- Remove an item from a list.
- Persist list state.
- Support multiple lists, with their own products, quantities, and checked states.

The user should be able to add a new product when needed rather than being limited to products already in the catalog. The exact UX for creating a catalog item while adding it to a list remains to be designed.

**Important distinction:** Removing an item from a shopping list must not be assumed to mean deleting the product from the reusable catalog. The exact deletion policy should be confirmed before implementation.

**Not yet decided:** Quantity units (pieces/kg/liters), minimum quantity and step size, sort order, handling of purchased items, and exact list-item edit controls.

### 2.4 Multiple lists

Examples discussed: **Home**, **Weekend House**, **Travel**.

**Agreed capabilities:** Create, view, rename, and delete lists. Each list maintains its own contents. Products can be reused across lists.

**Not yet decided:** Default list creation, ordering of lists, archive behavior, and whether lists may be duplicated as templates.

### 2.5 Family sharing and permissions

**Long-term essential capability:** Multiple family members use shared lists on their own devices, and changes appear for other authorized users with real-time or near-real-time synchronization.

**Agreed requirements:**

- Share lists among family members.
- Synchronize product additions, quantity changes, and checked states.
- Control who can see a particular list (e.g., a weekend-house list visible only to selected people).
- Provide administrator permissions to manage access.

**Not yet decided:** Sign-in provider, identity model, invitations, role definitions, backend/database provider, conflict resolution, offline sync behavior, and security rules. No particular cloud service has been selected.

## 3. Future extensions (planned ideas, not MVP commitments)

### 3.1 Product photos

Attach an image to a catalog product and manage it from product actions. Source (camera/gallery), image storage, and image lifecycle are undecided.

### 3.2 Price and discount information

Investigate retrieving product prices and promotions from retailer websites or other sources, with possible price comparison and discount notifications. This was an exploratory idea, **not** a decided integration. Retailers, geographic coverage, scraping permissions, data accuracy, and update frequency remain unresolved.

### 3.3 Reminders and location-based reminders

Ordinary reminders and geofenced reminders associated with shopping needs or locations. Trigger conditions, permissions, and reminder UX remain undecided.

### 3.4 Photo recognition

Explore recognizing products from photos to help add items to the catalog or lists. No model, vendor, or recognition workflow has been chosen.

### 3.5 Travel list templates

Provide reusable/default packing or travel lists that users can adapt to their own needs. Exact templates and creation workflow are undecided.

### 3.6 Shopping history and statistics

Explore historical activity and shopping-related statistics. Specific reports, measures, and retention policies have not been agreed.

### 3.7 Links on items

Allow attaching relevant product or website links. The precise data model and display behavior are undecided.

### Explicit current non-goal

A separate event-reminder system for school events, parent meetings, and similar appointments is **not** part of the current development scope.

## 4. Existing technical direction

The previously chosen stack is:

- **Kotlin** for Android.
- **Jetpack Compose** for UI.
- **Room** for local persistence.
- **ViewModel** for screen state and interaction logic.
- **Repository + DAO + Entity** layers for data access.
- **Kotlin Flow** for observing changing data.
- **Git/GitHub** for version control.
- **Android Studio** on Windows 11, with emulator testing and eventual physical-device testing.

The historical data flow is: `Compose UI → ViewModel → Repository → Room DAO → Room Database`.

Previously discussed files included `MainActivity.kt`, `MainMenuScreen.kt`, `ShoppingItemsScreen.kt`, `ShoppingListScreen.kt`, `MainMenuViewModel.kt`, `ChecklistViewModel.kt`, `FamilyRepository.kt`, `RoomFamilyRepository.kt`, `AppDatabase.kt`, `ItemDao.kt`, `ListDao.kt`, `ItemEntity.kt`, and `ListEntity.kt`. **This is not a verified current file list.**

The app previously had repository/import/interface mismatches. The owner has since reported that the app is working; **do not assume those old errors still exist**. Check the current code and build state first.

## 5. Development priorities

### Stage 0 — Protect and inspect the working app

1. Inspect current repository, branch, and Git status.
2. Preserve a safe checkpoint before modifications.
3. Run/verify the current build and inspect implemented screens.
4. Compare existing behavior against this vision.
5. Record what is already complete and what is missing; avoid unnecessary rewrites.

**Completion criterion:** A verified baseline and an accurate implementation inventory.

### Stage 1 — Complete the local Shopping Items catalog

Ensure catalog viewing, creation, renaming, deletion, categories, and local persistence are fully working. Add tests or manual verification for persistence after restarting the app. Product images may be deferred.

### Stage 2 — Complete local Shopping Lists

Ensure multiple lists, list CRUD, selection of catalog products, item quantities, purchased/unpurchased toggles, removal from a list, and reliable Room persistence all work together.

**Completion criterion:** A usable offline/local shopping-list app without requiring cloud accounts.

### Stage 3 — Family sharing

Choose authentication and synchronization technology after reviewing requirements and the existing architecture. Implement family membership, per-list permissions, administrator capabilities, cross-device synchronization, and testing on multiple phones.

### Stage 4 — Optional enhancements

Evaluate and prioritize photos, reminders, location-based reminders, travel templates, links, history/statistics, photo recognition, and retailer price/discount functionality. No final ordering has been agreed for these features.

### Stage 5 — Distribution

Internal testing first; potentially prepare a Google Play release later. No public launch date or commercialization plan has been decided.

## 6. Decision log: confirmed vs. open

### Confirmed

- Product name: FamilyApp; originally FamilyLists.
- Android/Kotlin/Jetpack Compose/Room project.
- Catalog and shopping lists are separate concepts.
- Multiple lists and reusable catalog products.
- Quantity +/- and checked/unchecked behavior.
- Family sharing and list visibility controls are important goals.
- Preserve the existing working project and proceed incrementally.
- Internal testing before any potential Google Play launch.

### Still open

- Detailed final UI/UX and visual design.
- Category schema and quantity units.
- Catalog/list item deletion rules and lifecycle.
- Authentication, sync backend, permissions model, and offline conflict behavior.
- Image capture/storage approach.
- Retailer price data sources and feasibility.
- Reminder scheduling/geofencing design.
- Analytics and template details.
- Timeline, public release scope, and monetization (if any).

## 7. Guidance for ChatGPT Work / Codex

1. **Read this document first, then inspect the actual repository.** Do not treat this document as proof that a feature exists in code.
2. The owner reports the app now works. Preserve that baseline. Do not reintroduce old fixes without evidence of current errors.
3. Do not start a new app or change architecture without a clear reason and owner approval.
4. Before implementing anything, provide a feature-by-feature gap analysis against the current code and a proposed next task.
5. Make focused, reviewable changes; run the relevant build/tests; describe modified files and outcomes.
6. Keep product documentation in the repository and update it as requirements are clarified.
7. Do not push changes to GitHub without the owner's approval.
8. When the original historical ChatGPT conversation export becomes available, review it for additional decisions and update this vision. **Do not silently turn speculative historical ideas into confirmed requirements.**

## 8. Immediate next action

**Do not begin feature implementation yet.** In ChatGPT Work, inspect the now-working FamilyApp project and compare it against this vision. Produce:

- A table of implemented, partially implemented, and missing features.
- Any unresolved decisions that block the next feature.
- A small, concrete implementation plan for the highest-priority missing capability.

Then ask the owner to approve the next development task.

---

*Source note: Compiled from the FamilyApp planning context available in this conversation.
The owner supplied the original brainstorming export, reviewed on 2026-10-21. It confirms
Android, reusable products, multiple lists, live shared updates, free initial use and
incremental extensions as the core goals. App Inventor/Firebase and early time estimates
were prototype suggestions, not requirements to replace the working Kotlin app.
The owner subsequently approved Google sign-in with Supabase Free and Owner/Admin
list membership; current decisions and implementation status are maintained in PLAN.md
and docs/ACCOUNTS_AND_SHARING.md. Historical undecided statements above describe the
original vision, not the current implementation status.*
