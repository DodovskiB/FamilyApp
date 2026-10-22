begin;
insert into auth.users(id,email,email_confirmed_at) values
 ('f6950000-0000-4000-8000-000000000001','owner@batch.invalid',now()),
 ('f6950000-0000-4000-8000-000000000002','admin@batch.invalid',now()),
 ('f6950000-0000-4000-8000-000000000003','stranger@batch.invalid',now());
set local role authenticated;
select set_config('request.jwt.claim.sub','f6950000-0000-4000-8000-000000000001',true);
select public.shopping_sync_apply('[
 {"operation_id":"f6950000-0000-4000-8000-000000000011","entity":"list","id":"f6950000-0000-4000-8000-000000000021","action":"insert","body":{"name":"First","sort_order":0}},
 {"operation_id":"f6950000-0000-4000-8000-000000000012","entity":"list","id":"f6950000-0000-4000-8000-000000000022","action":"insert","body":{"name":"Second","sort_order":1}},
 {"operation_id":"f6950000-0000-4000-8000-000000000013","entity":"list","id":"f6950000-0000-4000-8000-000000000023","action":"insert","body":{"name":"Private","sort_order":2}}]');
do $$ declare owner uuid:='f6950000-0000-4000-8000-000000000001'; admin uuid:='f6950000-0000-4000-8000-000000000002'; stranger uuid:='f6950000-0000-4000-8000-000000000003';
 a uuid:='f6950000-0000-4000-8000-000000000021'; b uuid:='f6950000-0000-4000-8000-000000000022'; c uuid:='f6950000-0000-4000-8000-000000000023'; g uuid; snapshot jsonb;
begin
 begin perform public.shopping_sharing_invite_many(array[a,'f6950000-0000-4000-8000-000000000099'::uuid],'admin@batch.invalid');raise exception 'Invalid batch allowed';exception when insufficient_privilege then null;end;
 if public.shopping_sharing_snapshot()->'invitations'<>'[]' then raise exception 'Partial invite committed';end if;
 perform public.shopping_sharing_invite_many(array[a,b,a],'ADMIN@batch.invalid');
 perform public.shopping_sharing_invite_many(array[a,b],'admin@batch.invalid');
 snapshot:=public.shopping_sharing_snapshot();
 if jsonb_array_length(snapshot->'invitations')<>2 then raise exception 'Duplicate invitations';end if;
 if (select count(distinct x->>'group_id') from jsonb_array_elements(snapshot->'invitations') x)<>1 then raise exception 'Wrong group';end if;
 g:=(snapshot->'invitations'->0->>'group_id')::uuid;
 perform set_config('request.jwt.claim.sub',stranger::text,true);
 begin perform public.shopping_sharing_respond_many(g,true);raise exception 'Stranger accepted';exception when insufficient_privilege then null;end;
 perform set_config('request.jwt.claim.sub',admin::text,true);
 if public.shopping_sharing_snapshot()->'records'<>'[]' then raise exception 'Pending invite grants access';end if;
 perform public.shopping_sharing_respond_many(g,true);perform public.shopping_sharing_respond_many(g,true);
 snapshot:=public.shopping_sharing_snapshot();
 if jsonb_array_length(snapshot->'access')<>2 then raise exception 'Selected lists not granted';end if;
 if exists(select 1 from jsonb_array_elements(snapshot->'access') x where x->>'id'=c::text) then raise exception 'Unselected list exposed';end if;
 begin perform public.shopping_sharing_invite_many(array[a,b],'stranger@batch.invalid');raise exception 'Admin invited';exception when insufficient_privilege then null;end;
 perform set_config('request.jwt.claim.sub',owner::text,true);
 perform public.shopping_sharing_remove(a,admin);
 perform set_config('request.jwt.claim.sub',admin::text,true);
 snapshot:=public.shopping_sharing_snapshot();
 if jsonb_array_length(snapshot->'access')<>1 or snapshot->'access'->0->>'id'<>b::text then raise exception 'Independent revocation failed';end if;
 begin perform public.shopping_sharing_respond_many(g,true);raise exception 'Revoked replay granted access';exception when insufficient_privilege then null;end;
 perform set_config('request.jwt.claim.sub',owner::text,true);
 perform public.shopping_sharing_invite_many(array[a,b],'admin@batch.invalid');
 snapshot:=public.shopping_sharing_snapshot();
 if jsonb_array_length(snapshot->'invitations')<>1 then raise exception 'Existing member reinvited';end if;
 g:=(snapshot->'invitations'->0->>'group_id')::uuid;
 perform set_config('request.jwt.claim.sub',admin::text,true);
 perform public.shopping_sharing_respond_many(g,false);perform public.shopping_sharing_respond_many(g,false);
 if jsonb_array_length(public.shopping_sharing_snapshot()->'access')<>1 then raise exception 'Decline granted access';end if;
 perform set_config('request.jwt.claim.sub',owner::text,true);
 perform public.shopping_sharing_invite_many(array[a,c],'stranger@batch.invalid');
 snapshot:=public.shopping_sharing_snapshot();
 perform public.shopping_sharing_remove(a,null,(select (x->>'id')::uuid from jsonb_array_elements(snapshot->'invitations') x where x->>'list_id'=a::text));
 perform set_config('request.jwt.claim.sub',stranger::text,true);
 snapshot:=public.shopping_sharing_snapshot();
 g:=(snapshot->'invitations'->0->>'group_id')::uuid;
 perform public.shopping_sharing_respond_many(g,true);
 if jsonb_array_length(public.shopping_sharing_snapshot()->'access')<>1 then raise exception 'Cancelled row accepted';end if;
end $$;
reset role;
rollback;
select 'PASS: atomic grouped invite, dedup, addressed acceptance, replay, unselected isolation, owner-only sharing, per-list revoke, decline and cancellation; fixtures rolled back' as result;
