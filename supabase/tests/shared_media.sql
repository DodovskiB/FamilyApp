-- Run after the media migration. All synthetic accounts and records roll back.
begin;
insert into auth.users(id,email,email_confirmed_at) values
 ('f6940000-0000-4000-8000-000000000001','owner@media.invalid',now()),
 ('f6940000-0000-4000-8000-000000000002','admin@media.invalid',now()),
 ('f6940000-0000-4000-8000-000000000003','stranger@media.invalid',now());
set local role authenticated;
select set_config('request.jwt.claim.sub','f6940000-0000-4000-8000-000000000001',true);
select public.shopping_sync_apply('[
 {"operation_id":"f6940000-0000-4000-8000-000000000011","entity":"list","id":"f6940000-0000-4000-8000-000000000021","action":"insert","body":{"name":"Media fixture","sort_order":0}},
 {"operation_id":"f6940000-0000-4000-8000-000000000012","entity":"list","id":"f6940000-0000-4000-8000-000000000022","action":"insert","body":{"name":"Media fixture","sort_order":1}},
 {"operation_id":"f6940000-0000-4000-8000-000000000013","entity":"item","id":"f6940000-0000-4000-8000-000000000024","action":"insert","body":{"title":"Media entry","category":"Other","qty":2,"checked":false,"active":true,"list_id":"f6940000-0000-4000-8000-000000000021","catalog_id":null}}]');
select public.shopping_sharing_invite('f6940000-0000-4000-8000-000000000021','admin@media.invalid');
do $$ declare
 owner uuid:='f6940000-0000-4000-8000-000000000001';
 admin uuid:='f6940000-0000-4000-8000-000000000002';
 stranger uuid:='f6940000-0000-4000-8000-000000000003';
 list_id uuid:='f6940000-0000-4000-8000-000000000021';
 item uuid:='f6940000-0000-4000-8000-000000000024';
 op uuid:='f6940000-0000-4000-8000-000000000031';
 path text; invitation uuid;
begin
 path:=owner::text||'/'||list_id::text||'/'||repeat('a',64)||'.jpg';
 if not public.shopping_media_access(path,false) then raise exception 'Owner image scope denied'; end if;
 perform public.shopping_media_apply(owner,list_id,item,op,2,'https://example.com/product',null);
 perform public.shopping_media_apply(owner,list_id,item,op,2,'https://example.com/product',null);
 begin perform public.shopping_media_apply(owner,list_id,item,op,2,'https://example.com/changed',null);
   raise exception 'Changed replay allowed'; exception when raise_exception then if SQLERRM<>'Operation ID reused' then raise;end if;end;
 begin perform public.shopping_media_apply(owner,list_id,item,gen_random_uuid(),null,null,null);
   raise exception 'Null mask allowed'; exception when raise_exception then if SQLERRM<>'Invalid media operation' then raise;end if;end;
 begin perform public.shopping_media_apply(owner,list_id,item,gen_random_uuid(),2,'javascript:alert(1)',null);
   raise exception 'Invalid link allowed'; exception when raise_exception then if SQLERRM<>'Invalid link' then raise;end if;end;
 begin perform public.shopping_media_apply(owner,list_id,item,gen_random_uuid(),1,null,path);
   raise exception 'Missing image allowed'; exception when insufficient_privilege then null;end;
 perform set_config('request.jwt.claim.sub',stranger::text,true);
 if public.shopping_media_snapshot()<>'[]' or public.shopping_media_access(path,false) then raise exception 'Stranger sees media';end if;
 begin perform public.shopping_media_apply(owner,list_id,item,gen_random_uuid(),2,'https://example.com/unauthorized',null);
   raise exception 'Stranger wrote media';exception when insufficient_privilege then null;end;
 begin perform 1 from public.shopping_item_media;raise exception 'Direct media read allowed';exception when insufficient_privilege then null;end;
 perform set_config('request.jwt.claim.sub',admin::text,true);
 if public.shopping_media_snapshot()<>'[]' or public.shopping_media_access(path,false) then raise exception 'Pending invite exposed media';end if;
 invitation:=(public.shopping_sharing_snapshot()->'invitations'->0->>'id')::uuid;
 perform public.shopping_sharing_respond(invitation,true);
 if jsonb_array_length(public.shopping_media_snapshot())<>1 or not public.shopping_media_access(path,false) then raise exception 'Admin image scope denied';end if;
 if public.shopping_media_access(owner::text||'/f6940000-0000-4000-8000-000000000022/'||repeat('a',64)||'.jpg',false) then raise exception 'Private list exposed';end if;
 if public.shopping_media_snapshot()->0->>'link_url'<>'https://example.com/product' then raise exception 'Shared link missing';end if;
 perform public.shopping_media_apply(owner,list_id,item,gen_random_uuid(),1,null,null);
 if public.shopping_media_snapshot()->0->>'link_url'<>'https://example.com/product' then raise exception 'Photo edit erased link';end if;
 perform public.shopping_media_apply(owner,list_id,item,gen_random_uuid(),2,null,null);
 if public.shopping_media_snapshot()->0->>'link_url' is not null then raise exception 'Link removal failed';end if;
 perform set_config('request.jwt.claim.sub',owner::text,true);
 perform public.shopping_sharing_remove(list_id,admin);
 perform set_config('request.jwt.claim.sub',admin::text,true);
 if public.shopping_media_snapshot()<>'[]' or public.shopping_media_access(path,false) then raise exception 'Revoked member sees media';end if;
 begin perform public.shopping_media_apply(owner,list_id,item,op,2,'https://example.com/product',null);
   raise exception 'Revoked replay allowed';exception when insufficient_privilege then null;end;
 perform set_config('request.jwt.claim.sub',owner::text,true);
  perform public.shopping_sync_merge_lists(list_id,'f6940000-0000-4000-8000-000000000022');
  list_id:='f6940000-0000-4000-8000-000000000022';
  perform public.shopping_media_apply(owner,list_id,item,op,2,'https://example.com/product',null);
  if public.shopping_media_snapshot()->0->>'link_url' is not null then raise exception 'Merge replay changed cleared link';end if;
 perform public.shopping_sync_apply(jsonb_build_array(jsonb_build_object('operation_id',gen_random_uuid(),'entity','item','id',item,'action','delete','body','{}'::jsonb)));
 perform public.shopping_media_apply(owner,list_id,item,gen_random_uuid(),2,'https://example.com/late',null);
 if public.shopping_media_snapshot()<>'[]' then raise exception 'Deleted item resurrected';end if;
end $$;
reset role;
rollback;
select 'PASS: media Owner/Admin scope, private list isolation, pending/revoked denial, replay, validation, independent fields and deletion; fixtures rolled back' as result;
