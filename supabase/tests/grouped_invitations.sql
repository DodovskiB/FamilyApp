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
 perform public.shopping_sharing_invite_many(array[a,b,c],'stranger@batch.invalid');
 snapshot:=public.shopping_sharing_snapshot();
 g:=(snapshot->'invitations'->0->>'group_id')::uuid;
 perform public.shopping_sharing_remove(a,null,(select (x->>'id')::uuid from jsonb_array_elements(snapshot->'invitations') x where x->>'list_id'=a::text));
 perform set_config('request.jwt.claim.sub',stranger::text,true);
 snapshot:=public.shopping_sharing_snapshot();
 if jsonb_array_length(snapshot->'invitations')<>0 then raise exception 'Group not fully cancelled';end if;
 begin perform public.shopping_sharing_respond_many(g,true);raise exception 'Cancelled group accepted';exception when insufficient_privilege then null;end;
 if jsonb_array_length(public.shopping_sharing_snapshot()->'access')<>0 then raise exception 'Cancelled group grants access';end if;
 -- A legacy recipient may already have accepted just one row. Keep that grant.
 perform set_config('request.jwt.claim.sub',owner::text,true);
 perform public.shopping_sharing_invite_many(array[a,b,c],'stranger@batch.invalid');
 snapshot:=public.shopping_sharing_snapshot();
 perform set_config('request.jwt.claim.sub',stranger::text,true);
 perform public.shopping_sharing_respond((select (x->>'id')::uuid from jsonb_array_elements(snapshot->'invitations') x where x->>'list_id'=a::text),true);
 begin perform public.shopping_sharing_remove(b,null,(select (x->>'id')::uuid from jsonb_array_elements(snapshot->'invitations') x where x->>'list_id'=b::text));raise exception 'Non-owner cancelled';exception when insufficient_privilege then null;end;
 perform set_config('request.jwt.claim.sub',owner::text,true);
 perform public.shopping_sharing_remove(b,null,(select (x->>'id')::uuid from jsonb_array_elements(snapshot->'invitations') x where x->>'list_id'=b::text));
 perform public.shopping_sharing_remove(b,null,(select (x->>'id')::uuid from jsonb_array_elements(snapshot->'invitations') x where x->>'list_id'=b::text));
 perform set_config('request.jwt.claim.sub',stranger::text,true);
 snapshot:=public.shopping_sharing_snapshot();
 if jsonb_array_length(snapshot->'invitations')<>0 or jsonb_array_length(snapshot->'access')<>1
    or snapshot->'access'->0->>'id'<>a::text then raise exception 'Cancellation removed accepted grant or left pending rows';end if;
end $$;
do $$ declare owner uuid:='f6950000-0000-4000-8000-000000000001'; admin uuid:='f6950000-0000-4000-8000-000000000002';
 b uuid:='f6950000-0000-4000-8000-000000000022'; before_revision bigint; after_revision bigint;
begin
 perform set_config('request.jwt.claim.sub',owner::text,true);
 if exists(select 1 from jsonb_array_elements(public.shopping_activity_snapshot()) x where (x->>'revision')::bigint<>0)
 then raise exception 'Own list creation flagged as somebody else';end if;
 perform public.shopping_sync_apply('[{"operation_id":"f6950000-0000-4000-8000-000000000061","entity":"list","id":"f6950000-0000-4000-8000-000000000022","action":"update","body":{"name":"Owner change"}}]');
 perform set_config('request.jwt.claim.sub',admin::text,true);
 select (x->>'revision')::bigint into before_revision from jsonb_array_elements(public.shopping_activity_snapshot()) x where x->>'id'=b::text;
 if before_revision is null or before_revision<=0 then raise exception 'Other actor change missing';end if;
 if jsonb_array_length(public.shopping_activity_snapshot())<>1 then raise exception 'Private list activity exposed';end if;
 perform public.shopping_sharing_apply(owner,b,'[{"operation_id":"f6950000-0000-4000-8000-000000000062","entity":"list","id":"f6950000-0000-4000-8000-000000000022","action":"update","body":{"name":"Admin change"}}]');
 select (x->>'revision')::bigint into after_revision from jsonb_array_elements(public.shopping_activity_snapshot()) x where x->>'id'=b::text;
 if after_revision<>before_revision then raise exception 'Own change generated unread activity';end if;
 perform public.shopping_sharing_apply(owner,b,'[{"operation_id":"f6950000-0000-4000-8000-000000000063","entity":"item","id":"f6950000-0000-4000-8000-000000000064","action":"insert","body":{"list_id":"f6950000-0000-4000-8000-000000000022","catalog_id":null,"title":"Activity fixture","category":"Other","qty":1,"checked":false,"active":true}}]');
 perform set_config('request.jwt.claim.sub',owner::text,true);
 select (x->>'revision')::bigint into before_revision from jsonb_array_elements(public.shopping_activity_snapshot()) x where x->>'id'=b::text;
 if before_revision<=after_revision then raise exception 'Admin change not visible to owner';end if;
 perform set_config('request.jwt.claim.sub',admin::text,true);
 perform public.shopping_media_apply(owner,b,'f6950000-0000-4000-8000-000000000064','f6950000-0000-4000-8000-000000000065',2,'https://example.com/activity');
 perform set_config('request.jwt.claim.sub',owner::text,true);
 select (x->>'revision')::bigint into after_revision from jsonb_array_elements(public.shopping_activity_snapshot()) x where x->>'id'=b::text;
 if after_revision<=before_revision then raise exception 'Media change not tracked';end if;
 perform public.shopping_sharing_remove(b,admin);
 perform set_config('request.jwt.claim.sub',admin::text,true);
 if public.shopping_activity_snapshot()<>'[]' then raise exception 'Revoked activity exposed';end if;
end $$;
reset role;
rollback;
select 'PASS: grouped cancellation, accepted-grant preservation, addressed acceptance, replay, owner-only access, per-list revoke, actor-aware activity and media changes; fixtures rolled back' as result;
