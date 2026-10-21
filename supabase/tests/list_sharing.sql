-- Disposable confirmed accounts. Roll back every fixture and membership.
begin;
insert into auth.users(id,email,email_confirmed_at) values
 ('f6930000-0000-4000-8000-000000000001','owner@sharing.invalid',now()),
 ('f6930000-0000-4000-8000-000000000002','admin@sharing.invalid',now()),
 ('f6930000-0000-4000-8000-000000000003','stranger@sharing.invalid',now());
set local role authenticated;
select set_config('request.jwt.claim.sub','f6930000-0000-4000-8000-000000000001',true);
select public.shopping_sync_apply('[
 {"operation_id":"f6930000-0000-4000-8000-000000000011","entity":"list","id":"f6930000-0000-4000-8000-000000000021","action":"insert","body":{"name":"Shared fixture","sort_order":0}},
 {"operation_id":"f6930000-0000-4000-8000-000000000012","entity":"list","id":"f6930000-0000-4000-8000-000000000022","action":"insert","body":{"name":"Private fixture","sort_order":1}},
 {"operation_id":"f6930000-0000-4000-8000-000000000013","entity":"catalog","id":"f6930000-0000-4000-8000-000000000023","action":"insert","body":{"title":"Private catalog","category":"Other","link_url":null}},
 {"operation_id":"f6930000-0000-4000-8000-000000000014","entity":"item","id":"f6930000-0000-4000-8000-000000000024","action":"insert","body":{"title":"Shared entry","category":"Other","qty":2,"checked":false,"active":true,"list_id":"f6930000-0000-4000-8000-000000000021","catalog_id":"f6930000-0000-4000-8000-000000000023"}}]');
select public.shopping_sharing_invite('f6930000-0000-4000-8000-000000000021','ADMIN@sharing.invalid');
select public.shopping_sharing_invite('f6930000-0000-4000-8000-000000000021','admin@sharing.invalid');
do $$ declare snapshot jsonb; invitation uuid; ops jsonb; owner uuid:='f6930000-0000-4000-8000-000000000001'; list_id uuid:='f6930000-0000-4000-8000-000000000021';
begin
  if jsonb_array_length(public.shopping_sharing_snapshot()->'invitations')<>1 then raise exception 'Repeated invite duplicated'; end if;
  perform set_config('request.jwt.claim.sub','f6930000-0000-4000-8000-000000000003',true);
  if public.shopping_sharing_snapshot()->'records'<>'[]' or public.shopping_sharing_snapshot()->'invitations'<>'[]' then raise exception 'Stranger sees data'; end if;
  -- Spoofing JWT email must not make an invitation visible.
  perform set_config('request.jwt.claim.email','admin@sharing.invalid',true);
  if public.shopping_sharing_snapshot()->'invitations'<>'[]' then raise exception 'JWT email spoof'; end if;
  perform set_config('request.jwt.claim.sub','f6930000-0000-4000-8000-000000000002',true);
  snapshot:=public.shopping_sharing_snapshot();
  if snapshot->'records'<>'[]' then raise exception 'Pending invite grants data'; end if;
  invitation:=(snapshot->'invitations'->0->>'id')::uuid;
  perform set_config('request.jwt.claim.sub','f6930000-0000-4000-8000-000000000003',true);
  begin perform public.shopping_sharing_respond(invitation,true); raise exception 'Stranger accepted'; exception when insufficient_privilege then null; end;
  perform set_config('request.jwt.claim.sub','f6930000-0000-4000-8000-000000000002',true);
  perform public.shopping_sharing_respond(invitation,true);
  perform public.shopping_sharing_respond(invitation,true);
  snapshot:=public.shopping_sharing_snapshot();
  if jsonb_array_length(snapshot->'records')<>2 or snapshot->'access'->0->>'role'<>'admin' then raise exception 'Wrong scope/role'; end if;
  if exists(select 1 from jsonb_array_elements(snapshot->'records') r where r->>'entity'='catalog' or r->'body'->>'catalog_id' is not null) then raise exception 'Catalog exposed'; end if;
  if public.shopping_sync_pull(0)<>'[]' then raise exception 'Private owner feed exposed'; end if;
  begin perform public.shopping_sync_apply_internal(owner,'[]'); raise exception 'Internal helper callable'; exception when insufficient_privilege then null; end;
  begin perform public.shopping_sharing_invite(list_id,'stranger@sharing.invalid'); raise exception 'Admin invited'; exception when insufficient_privilege then null; end;
  begin perform public.shopping_sharing_remove(list_id,'f6930000-0000-4000-8000-000000000002'); raise exception 'Admin revoked'; exception when insufficient_privilege then null; end;
  begin perform public.shopping_sharing_apply(owner,list_id,'[{"entity":"list","id":"f6930000-0000-4000-8000-000000000021","action":"delete","body":{}}]'); raise exception 'Admin deleted list'; exception when insufficient_privilege then null; end;
  begin perform public.shopping_sharing_apply(owner,list_id,'[{"entity":"item","id":"f6930000-0000-4000-8000-000000000025","action":"insert","body":{"list_id":"f6930000-0000-4000-8000-000000000022"}}]'); raise exception 'Admin wrote private list'; exception when insufficient_privilege then null; end;
  ops:='[
   {"operation_id":"f6930000-0000-4000-8000-000000000031","entity":"list","id":"f6930000-0000-4000-8000-000000000021","action":"update","body":{"name":"Renamed by Admin"}},
   {"operation_id":"f6930000-0000-4000-8000-000000000032","entity":"item","id":"f6930000-0000-4000-8000-000000000024","action":"update","body":{"qty_delta":1,"checked":true}},
   {"operation_id":"f6930000-0000-4000-8000-000000000033","entity":"item","id":"f6930000-0000-4000-8000-000000000025","action":"insert","body":{"title":"Admin addition","category":"Other","qty":1,"checked":false,"active":true,"list_id":"f6930000-0000-4000-8000-000000000021","catalog_id":null}}]';
  perform public.shopping_sharing_apply(owner,list_id,ops);
  perform public.shopping_sharing_apply(owner,list_id,ops);
  snapshot:=public.shopping_sharing_snapshot();
  if not exists(select 1 from jsonb_array_elements(snapshot->'records') r where r->>'entity'='item' and r->'body'->>'qty'='3' and r->'body'->>'checked'='true') then raise exception 'Admin edit/replay failed'; end if;
  perform public.shopping_sharing_apply(owner,list_id,'[{"operation_id":"f6930000-0000-4000-8000-000000000034","entity":"item","id":"f6930000-0000-4000-8000-000000000025","action":"delete","body":{}}]');
  perform set_config('request.jwt.claim.sub',owner::text,true);
  perform public.shopping_sharing_remove(list_id,'f6930000-0000-4000-8000-000000000002');
  perform set_config('request.jwt.claim.sub','f6930000-0000-4000-8000-000000000002',true);
  if public.shopping_sharing_snapshot()->'records'<>'[]' then raise exception 'Revoked member sees data'; end if;
  begin perform public.shopping_sharing_apply(owner,list_id,ops); raise exception 'Revoked replay allowed'; exception when insufficient_privilege then null; end;
  begin perform 1 from public.shopping_list_members; raise exception 'Direct member read allowed'; exception when insufficient_privilege then null; end;
  perform set_config('request.jwt.claim.sub',owner::text,true);
  perform public.shopping_sharing_invite(list_id,'admin@sharing.invalid');
  perform set_config('request.jwt.claim.sub','f6930000-0000-4000-8000-000000000002',true);
  perform public.shopping_sharing_respond(invitation,false);
  perform public.shopping_sharing_respond(invitation,false);
  if public.shopping_sharing_snapshot()->'records'<>'[]' then raise exception 'Decline granted access'; end if;
  perform set_config('request.jwt.claim.sub',owner::text,true);
  perform public.shopping_sharing_invite(list_id,'admin@sharing.invalid');
  perform public.shopping_sharing_remove(list_id,null,invitation);
  perform set_config('request.jwt.claim.sub','f6930000-0000-4000-8000-000000000002',true);
  begin perform public.shopping_sharing_respond(invitation,true); raise exception 'Cancelled invitation accepted'; exception when insufficient_privilege then null; end;
  perform set_config('request.jwt.claim.sub',owner::text,true);
  perform public.shopping_sharing_invite(list_id,'admin@sharing.invalid');
end $$;
reset role;
update public.shopping_list_invitations set expires_at=now()-interval '1 second'
  where owner_id='f6930000-0000-4000-8000-000000000001';
select set_config('sharing_test.invitation_id',(select id::text from public.shopping_list_invitations
  where owner_id='f6930000-0000-4000-8000-000000000001'),true);
set local role authenticated;
select set_config('request.jwt.claim.sub','f6930000-0000-4000-8000-000000000002',true);
do $$ declare invitation uuid;
begin
  if public.shopping_sharing_snapshot()->'invitations'<>'[]' then raise exception 'Expired invitation visible'; end if;
  begin perform public.shopping_sharing_respond(current_setting('sharing_test.invitation_id')::uuid,true);
    raise exception 'Expired invitation accepted'; exception when insufficient_privilege then null; end;
  -- Use the known owner view to renew; the recipient must not gain access by expiry.
  perform set_config('request.jwt.claim.sub','f6930000-0000-4000-8000-000000000001',true);
  perform public.shopping_sharing_invite('f6930000-0000-4000-8000-000000000021','admin@sharing.invalid');
  if jsonb_array_length(public.shopping_sharing_snapshot()->'invitations')<>1 then raise exception 'Expired invitation not renewed'; end if;
end $$;
reset role;
rollback;
select 'PASS: scoped Owner/Admin access, private catalog, addressed acceptance, replay, member edits, owner-only actions and revocation; fixtures rolled back' as result;
