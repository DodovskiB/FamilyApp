# Owner/Admin sharing — private pilot

Implemented on 2026-10-21, on top of the existing owner sync. Public app branding and
Android/Room identity are unchanged. No new SDK, paid service or email sender.

## Access and invitations

- One Owner (the canonical record owner); accepted members are Admins, with no Viewer role.
- Both edit the shared list name and its entries, quantities and checked states.
- Only Owner can delete the list, invite people, cancel invitations and remove members.
- Long press a list -> Share / Manage members. Long press an Admin/pending invitation
  in that screen to remove/cancel, with confirmation. Invitations are in the hamburger menu.
- An invitation addresses a confirmed Google account email, normalized on the server.
  Only that authenticated account can Accept/Decline. JWT email overrides are ignored.
- Pending invitations expire after 7 days; cancelled/declined/expired invitations can
  be renewed. Repeating an unchanged pending invitation or acceptance/decline is idempotent.
- No arbitrary member count restriction. Free provider quotas still apply. Invitations
  are in-app only; adding one does not send an email. The Google app is still Testing;
  testers need Google OAuth test-user access during this pilot.

## Server

Run `supabase/migrations/20261021235000_list_sharing.sql` **after** both earlier owner
sync migrations. It was deployed to the existing project. The migration preserves
records, revisions, operation IDs, owner identity and old APK compatibility.

RLS denies direct reads/writes of membership/invitation tables. Authenticated RPCs
derive the actor from auth.uid(). The original apply body is moved to an internal,
non-client-executable helper; owner apply supplies auth.uid(). Shared apply verifies
current membership, live parent list, allowed fields/actions and item parent, before
calling the helper. Owner sync, shared writes, invite responses and revocation serialize
on the same owner transaction lock. Client-supplied owner IDs never grant permission.
Global (entity,id) uniqueness prevents shared and private rows aliasing in local UUID maps.

An Admin cannot use the shared RPC to delete the list, access a catalog, edit another
list's item, change its parent, or replay changes after revocation. Canonical entry
mutations still use field patches, quantity deltas, deletion tombstones and operation IDs.
Existing owner pull/merge behavior is retained.

Sharing snapshot returns all currently authorized shared lists/entries and roles,
pending invitations for the recipient/Owner, and member emails only for that Owner.
It does not expose private catalogs or unrelated lists. A full snapshot is deliberate:
acceptance can grant access to records older than an incremental cursor. It is a
foreground private-pilot implementation; pagination/traffic optimization is future work.

## Android persistence and revocation

Shared records reuse their canonical UUIDs and the existing Room entities/DAO/repository.
Auxiliary sharing access/queue tables do not change Room schema version 5.
Shared snapshot revisions never advance the personal incremental cursor.
Queued shared edits route to the canonical Owner/list; personal catalog edits still
route only to their own account. A local association to the Admin's own catalog stays
local and survives a sanitized shared snapshot; it is not uploaded as the Owner's catalog.

After a successful complete access snapshot, revoked/deleted shared lists and entries
are removed from normal local views before uploads. Their pending edits are moved to
private `shopping_revoked_changes`, not re-imported as personally owned data and not
retried indefinitely. There is no recovery/export UI for that private quarantine yet.
Network failures retain cached lists and their durable queues. Revocation while a
phone is offline cannot erase already cached data until it reconnects. The server
rejects any subsequent write without current permission, including races/replays.
Local repository and UI also prevent Admin list deletion.

## Verified / remaining

- App APK, test APK and unit-test Gradle tasks passed; 3 unit tests passed.
- 19 isolated phone tests passed on Honor: 2 sharing UI, 2 sharing storage, 7 sync,
  3 account isolation, 2 catalog, 3 list storage/migrations. Sharing checks routing,
  cached roles, private cursor,
  offline retention, revoked queue quarantine and local delete denial.
- Existing 2 opt-in live sync tests passed against the real service with isolated DBs.
- Original owner/merge SQL regressions and `supabase/tests/list_sharing.sql` passed
  with rolled-back fixture users/data. New SQL checks scoped read/write, invite
  lifecycle, idempotency, private catalogs, internal helper ACL, owner-only actions
  and revocation. No real user email or UUID is included in the fixture tests.
- Note 20 was privately backed up and update-installed without uninstall. Actual UI
  creation of a disposable Sharing test list/product and pending invitation succeeded.
- Physical two-account acceptance passed: recipient saw one addressed pending invitation,
  accepted it, and viewed the shared entry. Admin changed 1/unchecked to 2/checked;
  Owner confirmed and changed it to 3/unchecked; Honor directly displayed 3/unchecked.
  Admin added a personal catalog product, re-added it as qty 2 without a duplicate,
  and removed the entry. Admin list actions showed Rename and Cancel only.
- With Honor offline, Admin incremented the test entry to 4. A private backup verified
  exactly one pending operation. Owner revoked access via the real management UI.
  After reconnect the list disappeared. Backup validation found no shared access,
  no pending operations, no shared entries and exactly one quarantined +1. Three
  personal lists remained; the old account's list/item/catalog tables were unchanged.
  Honor's disposable personal product was removed; Account showed Up to date.
- Final Note 20 update-install completed. Actual Owner UI still showed 3/unchecked
  after the revoked offline increment, confirming that it was not applied remotely.
  Only the disposable test list and its catalog product were removed through the UI.
  Account showed Up to date. Private SQLite backup comparisons verified exactly
  3 original lists, 6 original entries and 5 original catalog products, all original
  columns unchanged, and 0 pending operations. Both phones now have the same APK.
  No real list was automatically shared; the test membership is revoked.

Cloud photos and product-link attachment sharing are not included in this first
sharing pass: shared entries expose title/category/qty/check/active snapshots;
personal catalogs and local photos/links stay private. Purchased section / Clear
purchased, languages, design and distribution remain later tasks. Do not publish
this build as a completed public release.
